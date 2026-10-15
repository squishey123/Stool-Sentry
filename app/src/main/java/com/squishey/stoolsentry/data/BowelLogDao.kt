package com.squishey.stoolsentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BowelLogDao {
    @Query("SELECT * FROM bowel_logs ORDER BY occurredAtMillis DESC")
    fun observeAll(): Flow<List<BowelLogEntity>>

    @Query("SELECT * FROM bowel_logs WHERE localDate = :localDate ORDER BY occurredAtMillis DESC")
    fun observeByDate(localDate: String): Flow<List<BowelLogEntity>>

    @Query("SELECT * FROM bowel_logs ORDER BY occurredAtMillis ASC")
    suspend fun getAll(): List<BowelLogEntity>

    @Query("SELECT COUNT(*) FROM bowel_logs")
    suspend fun count(): Int

    @Query("SELECT * FROM bowel_logs WHERE id = :id")
    suspend fun get(id: Long): BowelLogEntity?

    @Insert
    suspend fun insert(entity: BowelLogEntity): Long

    @Insert
    suspend fun insertAll(entities: List<BowelLogEntity>)

    @Update
    suspend fun update(entity: BowelLogEntity)

    @Query("DELETE FROM bowel_logs WHERE id = :id")
    suspend fun delete(id: Long)
}
