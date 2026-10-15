package com.squishey.stoolsentry.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.squishey.stoolsentry.domain.UserSettings
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes every log to one file in a folder the user picked.
 * Each run replaces that same file. Encryption uses stool-sentry.ssenc;
 * otherwise the file is stool-sentry.csv.
 */
class AutoExporter(
    context: Context,
    private val settingsStore: SettingsStore,
    private val logs: () -> LogRepository,
    private val seal: (String) -> ByteArray?,
) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val paused = AtomicBoolean(false)
    private val failureEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

    val failures: SharedFlow<Unit> = failureEvents.asSharedFlow()

    fun pause() {
        paused.set(true)
    }

    fun resume() {
        paused.set(false)
    }

    /** Waits until an in-flight write has finished. */
    suspend fun drain() {
        withContext(Dispatchers.IO) { mutex.withLock { } }
    }

    fun notifyFailure() {
        failureEvents.tryEmit(Unit)
    }

    /** Starts a write if auto-export is on. A failure is reported on [failures]. */
    fun enqueue() {
        if (paused.get()) return
        scope.launch {
            if (!exportIfEnabled()) failureEvents.tryEmit(Unit)
        }
    }

    suspend fun exportIfEnabled(): Boolean = export(force = false)

    /** Writes even while [pause] is set, so a migration can refresh the backup before resuming. */
    suspend fun exportNow(): Boolean = export(force = true)

    private suspend fun export(force: Boolean): Boolean = try {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!force && paused.get()) return@withLock true
                writeIfEnabled()
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    fun persistTree(treeUri: Uri): Boolean = runCatching {
        context.contentResolver.takePersistableUriPermission(treeUri, PERSIST_FLAGS)
    }.isSuccess

    fun releaseTree(treeUri: Uri) {
        runCatching { context.contentResolver.releasePersistableUriPermission(treeUri, PERSIST_FLAGS) }
    }

    fun folderName(treeUri: Uri): String {
        val fallback = runCatching {
            val id = DocumentsContract.getTreeDocumentId(treeUri)
            id.substringAfterLast(':').substringAfterLast('/').ifBlank { id }
        }.getOrDefault("Folder")
        val queried = runCatching {
            val treeId = DocumentsContract.getTreeDocumentId(treeUri)
            val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
            context.contentResolver.query(
                document,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(0)?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
        return queried ?: fallback
    }

    private suspend fun writeIfEnabled(): Boolean {
        val settings = settingsStore.settings.first()
        if (!settings.autoExportEnabled) return true
        return writeCsv(settings)
    }

    private suspend fun writeCsv(settings: UserSettings): Boolean {
        val raw = settings.autoExportTreeUri ?: return false
        val treeUri = runCatching { Uri.parse(raw) }.getOrNull() ?: return false
        val csv = logs().exportCsv()
        val sealed = seal(csv)
        return runCatching {
            if (sealed == null) {
                writeToTree(treeUri, csv)
                deleteNamed(treeUri, setOf(ENC_NAME, ENC_TEMP))
            } else {
                writeEncrypted(treeUri, sealed)
            }
        }.isSuccess
    }

    private fun writeEncrypted(treeUri: Uri, bytes: ByteArray) {
        writeDocument(treeUri, ENC_TEMP, "application/octet-stream", bytes)
        deleteNamed(treeUri, setOf(ENC_NAME))
        writeDocument(treeUri, ENC_NAME, "application/octet-stream", bytes)
        deleteNamed(treeUri, setOf(ENC_TEMP, FILE_NAME, "$FILE_NAME.csv"))
    }

    private fun writeDocument(treeUri: Uri, displayName: String, mime: String, bytes: ByteArray) {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        val existingId = findNamed(treeUri, treeId, setOf(displayName))
        if (existingId != null) {
            val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, existingId)
            val replaced = runCatching {
                resolver.openOutputStream(document, "wt")?.use { it.write(bytes) }
            }.getOrNull()
            if (replaced != null) return
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
        }
        val created = DocumentsContract.createDocument(resolver, parent, mime, displayName)
            ?: DocumentsContract.createDocument(resolver, parent, "application/octet-stream", displayName)
            ?: error("Could not create $displayName")
        resolver.openOutputStream(created, "wt")?.use { it.write(bytes) }
            ?: error("Could not open $displayName")
    }

    private fun deleteNamed(treeUri: Uri, names: Set<String>) {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        val ids = mutableListOf<String>()
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                if (name in names) ids += cursor.getString(idColumn)
            }
        }
        ids.forEach { id ->
            val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
        }
    }

    private fun findNamed(treeUri: Uri, treeId: String, names: Set<String>): String? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                if (name in names) return cursor.getString(idColumn)
            }
        }
        return null
    }

    private fun writeToTree(treeUri: Uri, csv: String) {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        val existingId = findExport(treeUri, treeId)
        val bytes = csv.toByteArray(Charsets.UTF_8)
        if (existingId != null) {
            val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, existingId)
            val replaced = runCatching {
                resolver.openOutputStream(document, "wt")?.use { it.write(bytes) }
            }.getOrNull()
            if (replaced != null) return
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
        }
        val created = DocumentsContract.createDocument(resolver, parent, "text/csv", FILE_NAME)
            ?: DocumentsContract.createDocument(resolver, parent, "application/octet-stream", FILE_NAME)
            ?: error("Could not create $FILE_NAME")
        resolver.openOutputStream(created, "wt")?.use { it.write(bytes) }
            ?: error("Could not open $FILE_NAME")
    }

    private fun findExport(treeUri: Uri, treeId: String): String? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        val matches = mutableListOf<Pair<String, String>>()
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn) ?: continue
                if (name == FILE_NAME || name == "$FILE_NAME.csv") {
                    matches += name to cursor.getString(idColumn)
                }
            }
        }
        return matches.firstOrNull { it.first == FILE_NAME }?.second ?: matches.firstOrNull()?.second
    }

    private companion object {
        const val FILE_NAME = "stool-sentry.csv"
        const val ENC_NAME = "stool-sentry.ssenc"
        const val ENC_TEMP = "stool-sentry.ssenc.tmp"
        const val PERSIST_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
