package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class CsvDecode(
    val logs: List<BowelLog>,
    val skipped: Int,
)

object CsvCodec {
    val columns: List<String> = listOf(
        "local_date",
        "local_time",
        "zone",
        "type",
        "size",
        "color",
        "urgent",
        "blood",
        "painful",
        "straining",
        "incomplete",
        "mucus",
        "notes",
    )

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun encode(logs: List<BowelLog>): String {
        val builder = StringBuilder()
        builder.append(columns.joinToString(","))
        builder.append('\n')
        logs.forEach { log ->
            val fields = listOf(
                log.localDate.toString(),
                log.localTime().format(timeFormatter),
                log.zoneId,
                log.bristolType.number.toString(),
                log.size.name,
                log.color.name,
                log.symptoms.urgent.toString(),
                log.symptoms.blood.toString(),
                log.symptoms.painful.toString(),
                log.symptoms.straining.toString(),
                log.symptoms.incomplete.toString(),
                log.symptoms.mucus.toString(),
                log.notes,
            )
            builder.append(fields.joinToString(",") { escape(it) })
            builder.append('\n')
        }
        return builder.toString()
    }

    fun decode(csv: String, now: Instant): CsvDecode {
        val text = csv.removePrefix("\uFEFF").trimEnd()
        if (text.isBlank()) return CsvDecode(emptyList(), 0)
        val rows = parseRows(text)
        if (rows.isEmpty()) return CsvDecode(emptyList(), 0)
        if (rows.first() != columns) {
            return CsvDecode(emptyList(), rows.count { it.any(String::isNotBlank) })
        }
        val logs = mutableListOf<BowelLog>()
        var skipped = 0
        rows.drop(1).forEach { row ->
            if (row.all { it.isBlank() }) return@forEach
            val log = parseLog(row, now)
            if (log == null) skipped += 1 else logs += log
        }
        return CsvDecode(logs, skipped)
    }

    private fun parseLog(row: List<String>, now: Instant): BowelLog? {
        if (row.size != columns.size) return null
        val date = runCatching { LocalDate.parse(row[0]) }.getOrNull() ?: return null
        val time = parseTime(row[1]) ?: return null
        val zone = runCatching { ZoneId.of(row[2]) }.getOrNull() ?: return null
        val type = row[3].toIntOrNull()?.let(BristolType::fromNumber) ?: return null
        val size = runCatching { StoolSize.valueOf(row[4]) }.getOrNull() ?: return null
        val color = runCatching { StoolColor.valueOf(row[5]) }.getOrNull() ?: return null
        val urgent = parseBoolean(row[6]) ?: return null
        val blood = parseBoolean(row[7]) ?: return null
        val painful = parseBoolean(row[8]) ?: return null
        val straining = parseBoolean(row[9]) ?: return null
        val incomplete = parseBoolean(row[10]) ?: return null
        val mucus = parseBoolean(row[11]) ?: return null
        val notes = row[12]
        if (notes.length > LogValidator.MAX_NOTES) return null
        val occurred = ZonedDateTime.of(date, time, zone).toInstant()
        if (LogValidator.isFuture(occurred.toEpochMilli(), now.toEpochMilli())) return null
        val occurredMillis = occurred.toEpochMilli()
        return BowelLog(
            id = 0,
            occurredAtMillis = occurredMillis,
            zoneId = zone.id,
            localDate = date,
            bristolType = type,
            size = size,
            color = color,
            symptoms = Symptoms(
                urgent = urgent,
                blood = blood,
                painful = painful,
                straining = straining,
                incomplete = incomplete,
                mucus = mucus,
            ),
            notes = notes,
            createdAtMillis = occurredMillis,
            updatedAtMillis = occurredMillis,
        )
    }

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value, timeFormatter) }.getOrNull()
            ?: runCatching { LocalTime.parse(value) }.getOrNull()

    private fun parseBoolean(value: String): Boolean? = when (value.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun parseRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < csv.length) {
            val character = csv[index]
            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < csv.length && csv[index + 1] == '"') {
                        field.append('"')
                        index += 1
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(character)
                }
            } else {
                when (character) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString())
                        field.setLength(0)
                    }
                    '\n' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                    }
                    '\r' -> Unit
                    else -> field.append(character)
                }
            }
            index += 1
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
