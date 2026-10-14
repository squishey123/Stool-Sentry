package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

enum class WizardStep {
    TYPE,
    SIZE,
    COLOR,
    NOTES,
}

data class WizardState(
    val step: WizardStep,
    val existingId: Long?,
    val bristolType: BristolType?,
    val size: StoolSize?,
    val color: StoolColor?,
    val occurredAtMillis: Long,
    val zoneId: String,
    val localDate: LocalDate,
    val symptoms: Symptoms,
    val notes: String,
    val createdAtMillis: Long,
) {
    fun canAdvance(): Boolean = when (step) {
        WizardStep.TYPE -> bristolType != null
        WizardStep.SIZE -> size != null
        WizardStep.COLOR -> color != null
        WizardStep.NOTES -> false
    }

    fun canSave(nowMillis: Long): Boolean {
        return bristolType != null &&
            size != null &&
            color != null &&
            notes.length <= LogValidator.MAX_NOTES &&
            !LogValidator.isFuture(occurredAtMillis, nowMillis)
    }

    fun next(): WizardState {
        if (!canAdvance()) return this
        val nextStep = when (step) {
            WizardStep.TYPE -> WizardStep.SIZE
            WizardStep.SIZE -> WizardStep.COLOR
            WizardStep.COLOR -> WizardStep.NOTES
            WizardStep.NOTES -> WizardStep.NOTES
        }
        return copy(step = nextStep)
    }

    /** Returns null when back should discard the draft and leave the wizard. */
    fun back(): WizardState? {
        val previous = when (step) {
            WizardStep.TYPE -> return null
            WizardStep.SIZE -> WizardStep.TYPE
            WizardStep.COLOR -> WizardStep.SIZE
            WizardStep.NOTES -> WizardStep.COLOR
        }
        return copy(step = previous)
    }

    fun withType(type: BristolType): WizardState = copy(bristolType = type)

    fun withSize(value: StoolSize): WizardState = copy(size = value)

    fun withColor(value: StoolColor): WizardState = copy(color = value)

    fun withSymptoms(value: Symptoms): WizardState = copy(symptoms = value)

    fun withNotes(value: String): WizardState = copy(notes = value)

    fun withDateTime(date: LocalDate, time: LocalTime): WizardState {
        val occurred = ZonedDateTime.of(date, time.withNano(0), ZoneId.of(zoneId)).toInstant()
        return copy(localDate = date, occurredAtMillis = occurred.toEpochMilli())
    }

    fun localTime(): LocalTime =
        Instant.ofEpochMilli(occurredAtMillis).atZone(ZoneId.of(zoneId)).toLocalTime()

    fun toBowelLog(nowMillis: Long): BowelLog? {
        if (!canSave(nowMillis)) return null
        return BowelLog(
            id = existingId ?: 0L,
            occurredAtMillis = occurredAtMillis,
            zoneId = zoneId,
            localDate = localDate,
            bristolType = checkNotNull(bristolType),
            size = checkNotNull(size),
            color = checkNotNull(color),
            symptoms = symptoms,
            notes = notes,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = nowMillis,
        )
    }

    fun encode(): String {
        val notesEncoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(notes.toByteArray(Charsets.UTF_8))
        return listOf(
            step.name,
            existingId?.toString().orEmpty(),
            bristolType?.name.orEmpty(),
            size?.name.orEmpty(),
            color?.name.orEmpty(),
            occurredAtMillis.toString(),
            zoneId,
            localDate.toString(),
            symptoms.urgent.toString(),
            symptoms.blood.toString(),
            symptoms.painful.toString(),
            symptoms.straining.toString(),
            symptoms.incomplete.toString(),
            symptoms.mucus.toString(),
            createdAtMillis.toString(),
            notesEncoded,
        ).joinToString("\n")
    }

    companion object {
        fun newLog(clock: AppClock, presetDate: LocalDate? = null): WizardState {
            val zone = clock.zone()
            val now = clock.instant()
            val nowLocal = now.atZone(zone)
            val date = presetDate ?: nowLocal.toLocalDate()
            val time = nowLocal.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
            val occurred = ZonedDateTime.of(date, time, zone).toInstant()
            return WizardState(
                step = WizardStep.TYPE,
                existingId = null,
                bristolType = null,
                size = null,
                color = null,
                occurredAtMillis = occurred.toEpochMilli(),
                zoneId = zone.id,
                localDate = date,
                symptoms = Symptoms(),
                notes = "",
                createdAtMillis = now.toEpochMilli(),
            )
        }

        fun fromLog(log: BowelLog): WizardState = WizardState(
            step = WizardStep.TYPE,
            existingId = log.id,
            bristolType = log.bristolType,
            size = log.size,
            color = log.color,
            occurredAtMillis = log.occurredAtMillis,
            zoneId = log.zoneId,
            localDate = log.localDate,
            symptoms = log.symptoms,
            notes = log.notes,
            createdAtMillis = log.createdAtMillis,
        )

        fun decode(encoded: String): WizardState? {
            val parts = encoded.split("\n")
            if (parts.size != 16) return null
            return runCatching {
                val notes = if (parts[15].isEmpty()) {
                    ""
                } else {
                    String(Base64.getUrlDecoder().decode(parts[15]), Charsets.UTF_8)
                }
                WizardState(
                    step = WizardStep.valueOf(parts[0]),
                    existingId = parts[1].toLongOrNull(),
                    bristolType = parts[2].takeIf { it.isNotEmpty() }?.let(BristolType::valueOf),
                    size = parts[3].takeIf { it.isNotEmpty() }?.let(StoolSize::valueOf),
                    color = parts[4].takeIf { it.isNotEmpty() }?.let(StoolColor::valueOf),
                    occurredAtMillis = parts[5].toLong(),
                    zoneId = parts[6],
                    localDate = LocalDate.parse(parts[7]),
                    symptoms = Symptoms(
                        urgent = parts[8].toBooleanStrict(),
                        blood = parts[9].toBooleanStrict(),
                        painful = parts[10].toBooleanStrict(),
                        straining = parts[11].toBooleanStrict(),
                        incomplete = parts[12].toBooleanStrict(),
                        mucus = parts[13].toBooleanStrict(),
                    ),
                    createdAtMillis = parts[14].toLong(),
                    notes = notes,
                )
            }.getOrNull()
        }
    }
}
