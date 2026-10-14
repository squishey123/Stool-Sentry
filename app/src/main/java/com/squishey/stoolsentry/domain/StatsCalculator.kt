package com.squishey.stoolsentry.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class SummaryPeriod {
    DAYS_7,
    DAYS_30,
    DAYS_90,
    ALL,
}

enum class TimeOfDay(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night"),
    ;

    companion object {
        fun fromHour(hour: Int): TimeOfDay = when (hour) {
            in 5..10 -> MORNING
            in 11..16 -> AFTERNOON
            in 17..22 -> EVENING
            else -> NIGHT
        }
    }
}

data class CountShare(
    val key: String,
    val label: String,
    val count: Int,
    val fraction: Double,
)

data class SymptomRates(
    val urgent: Double,
    val blood: Double,
    val painful: Double,
    val straining: Double,
    val incomplete: Double,
    val mucus: Double,
)

data class SummarySnapshot(
    val period: SummaryPeriod,
    val rangeStart: LocalDate?,
    val rangeEnd: LocalDate,
    val isEmpty: Boolean,
    val logCount: Int,
    val dayCount: Int,
    val averagePerDay: Double,
    val daysSinceLast: Int?,
    val longestGapDays: Int?,
    val bristol: List<CountShare>,
    val sizes: List<CountShare>,
    val colors: List<CountShare>,
    val timeOfDay: List<CountShare>,
    val symptomRates: SymptomRates,
    val showClinicianNote: Boolean,
)

object StatsCalculator {
    fun summarize(
        logs: List<BowelLog>,
        period: SummaryPeriod,
        today: LocalDate,
    ): SummarySnapshot {
        val past = logs.filter { !it.localDate.isAfter(today) }
        val start = when (period) {
            SummaryPeriod.DAYS_7 -> today.minusDays(6)
            SummaryPeriod.DAYS_30 -> today.minusDays(29)
            SummaryPeriod.DAYS_90 -> today.minusDays(89)
            SummaryPeriod.ALL -> past.minOfOrNull { it.localDate }
        }
        val inPeriod = if (start == null) {
            emptyList()
        } else {
            past.filter { !it.localDate.isBefore(start) && !it.localDate.isAfter(today) }
        }
        val dayCount = if (start == null) 0 else ChronoUnit.DAYS.between(start, today).toInt() + 1
        val logCount = inPeriod.size
        val latest = past.maxOfOrNull { it.localDate }
        val distinctDates = inPeriod.map { it.localDate }.distinct().sorted()
        val longestGap = if (distinctDates.size < 2) {
            null
        } else {
            distinctDates.zipWithNext { earlier, later ->
                ChronoUnit.DAYS.between(earlier, later).toInt()
            }.max()
        }
        return SummarySnapshot(
            period = period,
            rangeStart = start,
            rangeEnd = today,
            isEmpty = past.isEmpty(),
            logCount = logCount,
            dayCount = dayCount,
            averagePerDay = if (dayCount == 0) 0.0 else logCount.toDouble() / dayCount,
            daysSinceLast = latest?.let { ChronoUnit.DAYS.between(it, today).toInt() },
            longestGapDays = longestGap,
            bristol = shares(
                present = inPeriod.map { it.bristolType },
                universe = BristolType.entries,
                key = { it.number.toString() },
                label = { it.label },
                tieBreak = compareBy { it.number },
            ),
            sizes = shares(
                present = inPeriod.map { it.size },
                universe = StoolSize.entries,
                key = { it.name },
                label = { it.label },
                tieBreak = compareBy { it.label },
            ),
            colors = shares(
                present = inPeriod.map { it.color },
                universe = StoolColor.entries,
                key = { it.name },
                label = { it.label },
                tieBreak = compareBy { it.label },
            ),
            timeOfDay = shares(
                present = inPeriod.map { it.timeOfDay() },
                universe = TimeOfDay.entries,
                key = { it.name },
                label = { it.label },
                tieBreak = compareBy { it.label },
            ),
            symptomRates = symptomRates(inPeriod),
            showClinicianNote = inPeriod.any { it.color.clinicianFlag || it.symptoms.blood },
        )
    }

    private fun symptomRates(logs: List<BowelLog>): SymptomRates {
        if (logs.isEmpty()) return SymptomRates(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val total = logs.size.toDouble()
        return SymptomRates(
            urgent = logs.count { it.symptoms.urgent } / total,
            blood = logs.count { it.symptoms.blood } / total,
            painful = logs.count { it.symptoms.painful } / total,
            straining = logs.count { it.symptoms.straining } / total,
            incomplete = logs.count { it.symptoms.incomplete } / total,
            mucus = logs.count { it.symptoms.mucus } / total,
        )
    }

    private fun <T> shares(
        present: List<T>,
        universe: List<T>,
        key: (T) -> String,
        label: (T) -> String,
        tieBreak: Comparator<T>,
    ): List<CountShare> {
        val counts = present.groupingBy(key).eachCount()
        val total = present.size
        return universe
            .sortedWith(compareByDescending<T> { counts[key(it)] ?: 0 }.then(tieBreak))
            .map { item ->
                val count = counts[key(item)] ?: 0
                CountShare(
                    key = key(item),
                    label = label(item),
                    count = count,
                    fraction = if (total == 0) 0.0 else count.toDouble() / total,
                )
            }
    }
}
