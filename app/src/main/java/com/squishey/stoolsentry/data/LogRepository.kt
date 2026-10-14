package com.squishey.stoolsentry.data

import com.squishey.stoolsentry.domain.AppClock
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.domain.CsvCodec
import com.squishey.stoolsentry.domain.ImportResult
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LogRepository(
    private val dao: BowelLogDao,
    private val clock: AppClock,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeAll(): Flow<List<BowelLog>> =
        dao.observeAll().map { logs -> logs.map { it.toDomain() } }.flowOn(io)

    fun observeByDate(date: LocalDate): Flow<List<BowelLog>> =
        dao.observeByDate(date.toString()).map { logs -> logs.map { it.toDomain() } }.flowOn(io)

    suspend fun get(id: Long): BowelLog? = withContext(io) {
        dao.get(id)?.toDomain()
    }

    suspend fun save(log: BowelLog): Long = withContext(io) {
        val entity = log.toEntity()
        if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun delete(id: Long) = withContext(io) {
        dao.delete(id)
    }

    suspend fun exportCsv(): String = withContext(io) {
        CsvCodec.encode(dao.getAll().map { it.toDomain() })
    }

    suspend fun import(csvText: String): ImportResult = withContext(io) {
        val decoded = CsvCodec.decode(csvText, clock.instant())
        if (decoded.logs.isNotEmpty()) {
            val now = clock.instant().toEpochMilli()
            dao.insertAll(
                decoded.logs.map { log ->
                    log.copy(id = 0, createdAtMillis = now, updatedAtMillis = now).toEntity()
                },
            )
        }
        ImportResult(imported = decoded.logs.size, skipped = decoded.skipped)
    }
}
