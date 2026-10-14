package com.squishey.stoolsentry.data

import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.domain.BristolType
import com.squishey.stoolsentry.domain.StoolColor
import com.squishey.stoolsentry.domain.StoolSize
import com.squishey.stoolsentry.domain.Symptoms
import java.time.LocalDate

fun BowelLog.toEntity(): BowelLogEntity = BowelLogEntity(
    id = id,
    occurredAtMillis = occurredAtMillis,
    zoneId = zoneId,
    localDate = localDate.toString(),
    bristolType = bristolType.number,
    size = size.name,
    color = color.name,
    urgent = symptoms.urgent,
    blood = symptoms.blood,
    painful = symptoms.painful,
    straining = symptoms.straining,
    incomplete = symptoms.incomplete,
    mucus = symptoms.mucus,
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun BowelLogEntity.toDomain(): BowelLog = BowelLog(
    id = id,
    occurredAtMillis = occurredAtMillis,
    zoneId = zoneId,
    localDate = LocalDate.parse(localDate),
    bristolType = requireNotNull(BristolType.fromNumber(bristolType)) {
        "Unknown Bristol type $bristolType"
    },
    size = StoolSize.valueOf(size),
    color = StoolColor.valueOf(color),
    symptoms = Symptoms(
        urgent = urgent,
        blood = blood,
        painful = painful,
        straining = straining,
        incomplete = incomplete,
        mucus = mucus,
    ),
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)
