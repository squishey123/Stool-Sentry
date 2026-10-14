package com.squishey.stoolsentry.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bowel_logs",
    indices = [Index("localDate")],
)
data class BowelLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAtMillis: Long,
    val zoneId: String,
    val localDate: String,
    val bristolType: Int,
    val size: String,
    val color: String,
    val urgent: Boolean,
    val blood: Boolean,
    val painful: Boolean,
    val straining: Boolean,
    val incomplete: Boolean,
    val mucus: Boolean,
    val notes: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
