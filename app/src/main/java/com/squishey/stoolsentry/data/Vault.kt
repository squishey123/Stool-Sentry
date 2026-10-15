package com.squishey.stoolsentry.data

import android.content.Context
import com.squishey.stoolsentry.domain.ExportCrypto
import com.squishey.stoolsentry.ui.lock.AppLock
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase as SqlCipherDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

sealed interface VaultPhase {
    data object Loading : VaultPhase
    data object Offer : VaultPhase
    data object Locked : VaultPhase
    data object Ready : VaultPhase
    data class Working(val kind: Kind, val request: VaultRequest) : VaultPhase

    enum class Kind { Enable, Disable, Change }
}

sealed interface VaultRequest {
    data class Enable(val passphrase: String) : VaultRequest
    data class Disable(val passphrase: String) : VaultRequest
    data class Change(val passphrase: String) : VaultRequest
}

enum class VaultError { Failed }

/**
 * Opens the log database, optionally under SQLCipher, and keeps the passphrase
 * available for backup files. The passphrase is the secret. [KeyWrap] only
 * saves typing it on this phone.
 */
class Vault(
    context: Context,
    private val settings: SettingsStore,
    private val clock: com.squishey.stoolsentry.domain.AppClock,
    private val appLock: AppLock,
    private val autoExporter: () -> AutoExporter,
) {
    private val context = context.applicationContext
    private val keyWrap = KeyWrap(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val started = AtomicBoolean(false)
    private val performing = AtomicBoolean(false)
    private val unlocking = AtomicBoolean(false)

    private val phaseState = MutableStateFlow<VaultPhase>(VaultPhase.Loading)
    private val encryptedState = MutableStateFlow(false)
    private val busyState = MutableStateFlow(false)
    private val unlockFailedState = MutableStateFlow(false)
    private val errorEvents = MutableSharedFlow<VaultError>(extraBufferCapacity = 4)

    val phase: StateFlow<VaultPhase> = phaseState
    val encrypted: StateFlow<Boolean> = encryptedState
    val busy: StateFlow<Boolean> = busyState
    val unlockFailed: StateFlow<Boolean> = unlockFailedState
    val errors: SharedFlow<VaultError> = errorEvents.asSharedFlow()

    var repository: LogRepository? = null
        private set

    private var database: AppDatabase? = null
    private var session: ExportSession? = null

    suspend fun start() {
        if (!started.compareAndSet(false, true)) return
        try {
            phaseState.value = VaultPhase.Loading
            withContext(Dispatchers.IO) { DatabaseFiles.recover(databasesDir()) }
            when (DatabaseFiles.kind(mainFile())) {
                DatabaseFiles.Kind.Encrypted -> {
                    val saved = keyWrap.read()
                    if (saved != null && openEncrypted(saved.passphrase, saved.exportKey)) {
                        phaseState.value = VaultPhase.Ready
                    } else {
                        phaseState.value = VaultPhase.Locked
                    }
                }
                DatabaseFiles.Kind.Plaintext, DatabaseFiles.Kind.Absent -> {
                    if (settings.settings.first().encryptionOfferSeen) {
                        openPlain()
                        phaseState.value = VaultPhase.Ready
                    } else {
                        phaseState.value = VaultPhase.Offer
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            started.set(false)
            throw cancelled
        } catch (_: Exception) {
            phaseState.value = VaultPhase.Locked
        }
    }

    fun skipOffer() {
        scope.launch {
            val opened = runCatching { openPlain() }.isSuccess
            if (!opened) {
                errorEvents.tryEmit(VaultError.Failed)
                return@launch
            }
            settings.setEncryptionOfferSeen(true)
            phaseState.value = VaultPhase.Ready
        }
    }

    fun beginEnable(passphrase: String) = begin(VaultPhase.Kind.Enable, VaultRequest.Enable(passphrase))

    fun beginDisable(passphrase: String) = begin(VaultPhase.Kind.Disable, VaultRequest.Disable(passphrase))

    fun beginChange(passphrase: String) = begin(VaultPhase.Kind.Change, VaultRequest.Change(passphrase))

    fun submitUnlock(passphrase: String) {
        if (!unlocking.compareAndSet(false, true)) return
        scope.launch {
            busyState.value = true
            unlockFailedState.value = false
            try {
                val opened = unlock(passphrase)
                if (!opened) {
                    unlockFailedState.value = true
                    return@launch
                }
                session?.let { keyWrap.store(it) }
                appLock.unlock()
                if (!autoExporter().exportNow()) autoExporter().notifyFailure()
                phaseState.value = VaultPhase.Ready
            } finally {
                busyState.value = false
                unlocking.set(false)
            }
        }
    }

    fun matches(passphrase: String): Boolean {
        val current = session?.passphrase ?: return false
        val typed = ExportCrypto.normalize(passphrase).toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(current, typed)
    }

    fun seal(csv: String): ByteArray? {
        val key = session?.exportKey ?: return null
        return ExportCrypto.encrypt(csv.toByteArray(Charsets.UTF_8), key)
    }

    suspend fun readBackup(bytes: ByteArray, passphrase: String? = null): String? {
        return withContext(Dispatchers.IO) {
            if (!ExportCrypto.looksEncrypted(bytes)) {
                if (passphrase != null) return@withContext null
                return@withContext bytes.toString(Charsets.UTF_8)
            }
            val phrase = when {
                passphrase != null -> ExportCrypto.normalize(passphrase)
                else -> session?.passphrase?.toString(Charsets.UTF_8)
            }
            if (phrase.isNullOrEmpty()) return@withContext null
            runCatching {
                ExportCrypto.decrypt(bytes, phrase).toString(Charsets.UTF_8)
            }.getOrNull()
        }
    }

    suspend fun perform(request: VaultRequest) {
        if (!performing.compareAndSet(false, true)) return
        try {
            when (request) {
                is VaultRequest.Enable -> enable(request.passphrase)
                is VaultRequest.Disable -> disable(request.passphrase)
                is VaultRequest.Change -> change(request.passphrase)
            }
            if (phaseState.value is VaultPhase.Working) phaseState.value = VaultPhase.Ready
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            recoverPhase()
            errorEvents.tryEmit(VaultError.Failed)
        } finally {
            performing.set(false)
        }
    }

    private fun begin(kind: VaultPhase.Kind, request: VaultRequest) {
        if (phaseState.value is VaultPhase.Working) return
        val passphrase = when (request) {
            is VaultRequest.Enable -> request.passphrase
            is VaultRequest.Disable -> request.passphrase
            is VaultRequest.Change -> request.passphrase
        }
        if (ExportCrypto.normalize(passphrase).isEmpty()) return
        phaseState.value = VaultPhase.Working(kind, request)
    }

    private suspend fun enable(passphrase: String) {
        val normalized = ExportCrypto.normalize(passphrase)
        check(normalized.isNotEmpty())
        appLock.unlock()
        val secret = normalized.toByteArray(Charsets.UTF_8)
        val exporter = autoExporter()
        exporter.pause()
        try {
            exporter.drain()
            val rows = readRowsForEnable()
            withContext(Dispatchers.IO) {
                DatabaseFiles.deleteAll(migratingFile())
                SqlCipher.load()
                val migrating = AppDatabase.open(context, DatabaseFiles.MIGRATING, secret)
                try {
                    if (rows.isNotEmpty()) migrating.bowelLogDao().insertAll(rows)
                    check(migrating.bowelLogDao().count() == rows.size)
                } catch (error: Exception) {
                    checkpointClose(migrating)
                    DatabaseFiles.deleteAll(migratingFile())
                    throw error
                }
                checkpointClose(migrating)
                DatabaseFiles.deleteAll(mainFile())
                DatabaseFiles.rename(migratingFile(), mainFile())
            }
            val key = withContext(Dispatchers.IO) { ExportCrypto.derive(normalized) }
            if (!openEncrypted(secret, key)) error("Could not open the encrypted database")
            session?.let { keyWrap.store(it) }
            settings.setEncryptionOfferSeen(true)
            if (!exporter.exportNow()) exporter.notifyFailure()
        } finally {
            exporter.resume()
        }
    }

    private suspend fun readRowsForEnable(): List<BowelLogEntity> {
        val kind = DatabaseFiles.kind(mainFile())
        if (kind == DatabaseFiles.Kind.Encrypted) error("Logs are already encrypted")
        if (kind == DatabaseFiles.Kind.Absent) {
            closeLive()
            return emptyList()
        }
        val db = database ?: AppDatabase.open(context, DatabaseFiles.MAIN, null)
        return try {
            withContext(Dispatchers.IO) { db.bowelLogDao().getAll() }
        } finally {
            checkpointClose(db)
            if (database === db) {
                database = null
                repository = null
            }
        }
    }

    private suspend fun disable(passphrase: String) {
        check(matches(passphrase))
        val exporter = autoExporter()
        exporter.pause()
        try {
            exporter.drain()
            val rows = readOpenRows()
            withContext(Dispatchers.IO) {
                DatabaseFiles.deleteAll(migratingFile())
                val migrating = AppDatabase.open(context, DatabaseFiles.MIGRATING, null)
                try {
                    if (rows.isNotEmpty()) migrating.bowelLogDao().insertAll(rows)
                    check(migrating.bowelLogDao().count() == rows.size)
                } catch (error: Exception) {
                    checkpointClose(migrating)
                    DatabaseFiles.deleteAll(migratingFile())
                    throw error
                }
                checkpointClose(migrating)
                DatabaseFiles.deleteAll(mainFile())
                DatabaseFiles.rename(migratingFile(), mainFile())
            }
            session = null
            openPlain()
            if (!exporter.exportNow()) exporter.notifyFailure()
        } finally {
            exporter.resume()
        }
    }

    private suspend fun change(passphrase: String) {
        val normalized = ExportCrypto.normalize(passphrase)
        check(normalized.isNotEmpty())
        val newSecret = normalized.toByteArray(Charsets.UTF_8)
        val old = session ?: error("Logs are not encrypted")
        val oldSecret = old.passphrase.copyOf()
        val oldKey = old.exportKey
        val exporter = autoExporter()
        exporter.pause()
        try {
            exporter.drain()
            val db = database ?: error("Database is not open")
            withContext(Dispatchers.IO) {
                SqlCipher.load()
                val support = db.openHelper.writableDatabase
                runCatching {
                    support.query("PRAGMA wal_checkpoint(FULL)").close()
                }
                val sql = support as? SqlCipherDatabase ?: error("Encrypted database is not open")
                sql.changePassword(newSecret)
            }
            checkpointClose(db)
            database = null
            repository = null
            if (!openEncrypted(newSecret, null)) {
                if (!openEncrypted(oldSecret, oldKey)) {
                    session = null
                    encryptedState.value = false
                }
                error("Could not open the database with the new passphrase")
            }
            session?.let { keyWrap.store(it) }
            if (!exporter.exportNow()) exporter.notifyFailure()
        } finally {
            exporter.resume()
        }
    }

    private suspend fun readOpenRows(): List<BowelLogEntity> {
        val db = database ?: error("Database is not open")
        return try {
            withContext(Dispatchers.IO) { db.bowelLogDao().getAll() }
        } finally {
            checkpointClose(db)
            database = null
            repository = null
        }
    }

    private suspend fun unlock(passphrase: String): Boolean {
        val normalized = ExportCrypto.normalize(passphrase)
        if (normalized.isEmpty()) return false
        return openEncrypted(normalized.toByteArray(Charsets.UTF_8), exportKey = null)
    }

    private suspend fun openEncrypted(passphrase: ByteArray, exportKey: ExportCrypto.ExportKey?): Boolean {
        closeLive()
        SqlCipher.load()
        val db = AppDatabase.open(context, DatabaseFiles.MAIN, passphrase)
        return try {
            val key = withContext(Dispatchers.IO) {
                db.bowelLogDao().count()
                exportKey ?: ExportCrypto.derive(passphrase.toString(Charsets.UTF_8))
            }
            publish(db, ExportSession(passphrase.copyOf(), key))
            true
        } catch (cancelled: CancellationException) {
            checkpointClose(db)
            throw cancelled
        } catch (_: Exception) {
            checkpointClose(db)
            false
        }
    }

    private suspend fun openPlain() {
        closeLive()
        keyWrap.clear()
        session = null
        val db = AppDatabase.open(context, DatabaseFiles.MAIN, null)
        withContext(Dispatchers.IO) { db.bowelLogDao().count() }
        publish(db, null)
    }

    private suspend fun recoverPhase() {
        val remembered = session
        closeLive()
        withContext(Dispatchers.IO) { DatabaseFiles.recover(databasesDir()) }
        when (DatabaseFiles.kind(mainFile())) {
            DatabaseFiles.Kind.Encrypted -> {
                val saved = keyWrap.read()
                val opened = when {
                    saved != null && openEncrypted(saved.passphrase, saved.exportKey) -> true
                    remembered != null && openEncrypted(remembered.passphrase, remembered.exportKey) -> true
                    else -> false
                }
                phaseState.value = if (opened) VaultPhase.Ready else VaultPhase.Locked
            }
            DatabaseFiles.Kind.Plaintext, DatabaseFiles.Kind.Absent -> {
                if (settings.settings.first().encryptionOfferSeen) {
                    openPlain()
                    phaseState.value = VaultPhase.Ready
                } else {
                    phaseState.value = VaultPhase.Offer
                }
            }
        }
    }

    private fun publish(db: AppDatabase, next: ExportSession?) {
        database = db
        session = next
        repository = LogRepository(db.bowelLogDao(), clock)
        encryptedState.value = next != null
    }

    private fun closeLive() {
        val db = database
        database = null
        repository = null
        if (db != null) checkpointClose(db)
    }

    private fun checkpointClose(db: AppDatabase) {
        runCatching {
            if (db.isOpen) db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        }
        runCatching { db.close() }
    }

    private fun mainFile(): File = context.getDatabasePath(DatabaseFiles.MAIN)

    private fun migratingFile(): File = context.getDatabasePath(DatabaseFiles.MIGRATING)

    private fun databasesDir(): File = mainFile().parentFile ?: context.filesDir
}
