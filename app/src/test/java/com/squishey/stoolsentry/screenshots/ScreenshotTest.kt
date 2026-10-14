package com.squishey.stoolsentry.screenshots

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.squishey.stoolsentry.domain.BristolType
import com.squishey.stoolsentry.domain.CalendarIndex
import com.squishey.stoolsentry.domain.StatsCalculator
import com.squishey.stoolsentry.domain.StoolColor
import com.squishey.stoolsentry.domain.StoolSize
import com.squishey.stoolsentry.domain.SummaryPeriod
import com.squishey.stoolsentry.domain.Symptoms
import com.squishey.stoolsentry.domain.UserSettings
import com.squishey.stoolsentry.domain.WizardState
import com.squishey.stoolsentry.domain.WizardStep
import com.squishey.stoolsentry.domain.sampleLog
import com.squishey.stoolsentry.ui.calendar.CalendarScreen
import com.squishey.stoolsentry.ui.calendar.CalendarUiState
import com.squishey.stoolsentry.ui.components.throneBackground
import com.squishey.stoolsentry.ui.day.DayScreen
import com.squishey.stoolsentry.ui.lock.LockScreen
import com.squishey.stoolsentry.ui.log.LogWizardScreen
import com.squishey.stoolsentry.ui.settings.SettingsScreen
import com.squishey.stoolsentry.ui.summary.SummaryScreen
import com.squishey.stoolsentry.ui.summary.SummaryUiState
import com.squishey.stoolsentry.ui.theme.StoolSentryTheme
import com.squishey.stoolsentry.ui.welcome.WelcomeScreen
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each screen to build/screenshots for a visual check. Run with -Pscreenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h880dp-xhdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 14)

    private val logs = listOf(
        sampleLog(today, LocalTime.of(7, 42), id = 1, type = BristolType.TYPE_4, size = StoolSize.MEDIUM_LARGE),
        sampleLog(
            today,
            LocalTime.of(19, 5),
            id = 2,
            type = BristolType.TYPE_5,
            size = StoolSize.SMALL_MEDIUM,
            color = StoolColor.DARK_BROWN,
            symptoms = Symptoms(urgent = true, straining = true),
            notes = "Spicy ramen last night. Worth it.",
        ),
        sampleLog(today.minusDays(1), LocalTime.of(8, 10), id = 3, type = BristolType.TYPE_3),
        sampleLog(today.minusDays(3), LocalTime.of(9, 30), id = 4, type = BristolType.TYPE_4),
        sampleLog(today.minusDays(3), LocalTime.of(21, 0), id = 5, type = BristolType.TYPE_6, color = StoolColor.YELLOW),
        sampleLog(today.minusDays(5), LocalTime.of(6, 50), id = 6, type = BristolType.TYPE_2, size = StoolSize.SMALL),
        sampleLog(today.minusDays(6), LocalTime.of(12, 15), id = 7, type = BristolType.TYPE_4),
        sampleLog(
            today.minusDays(8),
            LocalTime.of(7, 5),
            id = 8,
            type = BristolType.TYPE_1,
            color = StoolColor.BLACK,
            symptoms = Symptoms(painful = true, blood = true),
        ),
        sampleLog(today.minusDays(9), LocalTime.of(10, 0), id = 9, type = BristolType.TYPE_4),
        sampleLog(today.minusDays(11), LocalTime.of(14, 30), id = 10, type = BristolType.TYPE_7, color = StoolColor.GREEN),
        sampleLog(today.minusDays(12), LocalTime.of(8, 0), id = 11, type = BristolType.TYPE_3),
    ).sortedByDescending { it.occurredAtMillis }

    private fun capture(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            StoolSentryTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .throneBackground(),
                ) { content() }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun calendarState(withLogs: Boolean) = CalendarUiState(
        today = today,
        visibleMonth = YearMonth.from(today),
        selectedDate = today,
        countsByDate = if (withLogs) CalendarIndex.countsByDate(logs) else emptyMap(),
        weekStart = DayOfWeek.SUNDAY,
        latestLog = if (withLogs) CalendarIndex.latest(logs) else null,
        monthTally = CalendarIndex.monthTally(
            if (withLogs) CalendarIndex.countsByDate(logs) else emptyMap(),
            YearMonth.from(today),
        ),
        loaded = true,
    )

    @Test
    fun calendar() = capture("01_calendar") {
        CalendarScreen(calendarState(true), 19, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun calendarFirstRun() = capture("02_calendar_first_run") {
        CalendarScreen(calendarState(false), 9, {}, {}, {}, {}, {}, {}, {})
    }

    private fun draft(step: WizardStep): WizardState {
        val base = WizardState.newLog(
            com.squishey.stoolsentry.domain.FixedClock(
                today.atTime(19, 5).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
            ),
        )
        return when (step) {
            WizardStep.TYPE -> base.withType(BristolType.TYPE_4)
            WizardStep.SIZE -> base.withType(BristolType.TYPE_4).withSize(StoolSize.MEDIUM)
            WizardStep.COLOR -> base.withType(BristolType.TYPE_4).withSize(StoolSize.MEDIUM).withColor(StoolColor.BROWN)
            WizardStep.NOTES -> base.withType(BristolType.TYPE_4).withSize(StoolSize.MEDIUM)
                .withColor(StoolColor.BROWN)
                .withSymptoms(Symptoms(straining = true))
                .withNotes("Felt great after the morning coffee.")
        }.copy(step = step)
    }

    @Composable
    private fun Wizard(step: WizardStep) {
        LogWizardScreen(
            draft = draft(step),
            saving = false,
            editing = false,
            isInFuture = false,
            canSave = true,
            today = today,
            onSelectType = {},
            onSelectSize = {},
            onSelectColor = {},
            onSymptoms = {},
            onNotes = {},
            onDateTime = { _, _ -> },
            onNext = {},
            onBack = {},
            onSave = {},
        )
    }

    @Test
    fun wizardType() = capture("03_wizard_type") { Wizard(WizardStep.TYPE) }

    @Test
    fun wizardSize() = capture("04_wizard_size") { Wizard(WizardStep.SIZE) }

    @Test
    fun wizardColor() = capture("05_wizard_color") { Wizard(WizardStep.COLOR) }

    @Test
    fun wizardDetails() = capture("06_wizard_details") { Wizard(WizardStep.NOTES) }

    @Test
    fun day() = capture("07_day") {
        DayScreen(today, today, logs.filter { it.localDate == today }, {}, {}, {}, {})
    }

    @Test
    fun dayEmpty() = capture("08_day_empty") {
        DayScreen(today.minusDays(2), today, emptyList(), {}, {}, {}, {})
    }

    @Test
    fun summary() = capture("09_summary") {
        SummaryScreen(
            SummaryUiState(SummaryPeriod.DAYS_30, StatsCalculator.summarize(logs, SummaryPeriod.DAYS_30, today)),
            {},
            {},
        )
    }

    @Test
    @Config(qualifiers = "w400dp-h2200dp-xhdpi")
    fun summaryFull() = capture("09b_summary_full") {
        SummaryScreen(
            SummaryUiState(SummaryPeriod.DAYS_30, StatsCalculator.summarize(logs, SummaryPeriod.DAYS_30, today)),
            {},
            {},
        )
    }

    @Test
    fun summaryEmpty() = capture("10_summary_empty") {
        SummaryScreen(
            SummaryUiState(SummaryPeriod.DAYS_30, StatsCalculator.summarize(emptyList(), SummaryPeriod.DAYS_30, today)),
            {},
            {},
        )
    }

    @Test
    fun settings() = capture("11_settings") {
        SettingsScreen(
            settings = UserSettings(DayOfWeek.SUNDAY, disclaimerAccepted = true, appLockEnabled = false),
            lockAvailable = true,
            onBack = {},
            onExport = {},
            onImport = {},
            onLockChange = {},
            onWeekStart = {},
        )
    }

    @Test
    fun welcome() = capture("12_welcome") { WelcomeScreen(onAccept = {}) }

    @Test
    fun lock() = capture("13_lock") { LockScreen(onUnlock = {}) }
}
