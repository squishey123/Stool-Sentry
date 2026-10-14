package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WizardStateTest {
    private val now = Instant.parse("2026-10-14T15:30:00Z")
    private val clock = FixedClock(now, ZoneOffset.UTC)

    @Test
    fun newLogDefaultsSymptomsToNo() {
        val draft = WizardState.newLog(clock)
        assertFalse(draft.symptoms.urgent)
        assertFalse(draft.symptoms.blood)
        assertFalse(draft.symptoms.painful)
        assertFalse(draft.symptoms.straining)
        assertFalse(draft.symptoms.incomplete)
        assertFalse(draft.symptoms.mucus)
        assertEquals(LocalDate.of(2026, 10, 14), draft.localDate)
        assertEquals(LocalTime.of(15, 30), draft.localTime())
    }

    @Test
    fun nextStaysPutUntilTheStepHasAChoice() {
        val draft = WizardState.newLog(clock)
        assertEquals(WizardStep.TYPE, draft.next().step)
        assertEquals(WizardStep.SIZE, draft.withType(BristolType.TYPE_4).next().step)
    }

    @Test
    fun backKeepsEarlierAnswersAndDiscardsFromTheFirstStep() {
        val sized = WizardState.newLog(clock)
            .withType(BristolType.TYPE_4)
            .next()
            .withSize(StoolSize.LARGE)
            .next()
        val backToSize = sized.back()
        assertEquals(WizardStep.SIZE, backToSize?.step)
        assertEquals(StoolSize.LARGE, backToSize?.size)
        assertEquals(BristolType.TYPE_4, backToSize?.bristolType)
        assertNull(WizardState.newLog(clock).back())
    }

    @Test
    fun changingTypeDuringEditKeepsTheOtherAnswers() {
        val existing = sampleLog(
            date = LocalDate.of(2026, 10, 2),
            time = LocalTime.of(9, 0),
            type = BristolType.TYPE_3,
            size = StoolSize.SMALL,
            color = StoolColor.YELLOW,
            symptoms = Symptoms(urgent = true, mucus = true),
            notes = "morning",
            id = 4,
        )
        val edited = WizardState.fromLog(existing).withType(BristolType.TYPE_1)
        assertEquals(StoolSize.SMALL, edited.size)
        assertEquals(StoolColor.YELLOW, edited.color)
        assertEquals(existing.symptoms, edited.symptoms)
        assertEquals("morning", edited.notes)
        assertEquals(4L, edited.existingId)
    }

    @Test
    fun changingTheDateMovesTheLogOntoThatDay() {
        val moved = completeDraft().withDateTime(LocalDate.of(2026, 10, 1), LocalTime.of(8, 15))
        assertEquals(LocalDate.of(2026, 10, 1), moved.localDate)
        assertEquals(LocalTime.of(8, 15), moved.localTime())
        assertEquals(LocalDate.of(2026, 10, 1), moved.toBowelLog(now.toEpochMilli())?.localDate)
    }

    @Test
    fun futureDateAndOverlongNotesCannotBeSaved() {
        val draft = completeDraft()
        assertTrue(draft.canSave(now.toEpochMilli()))
        assertFalse(draft.withDateTime(LocalDate.of(2026, 10, 15), LocalTime.NOON).canSave(now.toEpochMilli()))
        assertNull(draft.withDateTime(LocalDate.of(2026, 10, 15), LocalTime.NOON).toBowelLog(now.toEpochMilli()))
        assertTrue(draft.withNotes("n".repeat(LogValidator.MAX_NOTES)).canSave(now.toEpochMilli()))
        assertFalse(draft.withNotes("n".repeat(LogValidator.MAX_NOTES + 1)).canSave(now.toEpochMilli()))
        assertFalse(LogValidator.isFuture(now.toEpochMilli(), now.toEpochMilli()))
        assertTrue(LogValidator.isFuture(now.plusSeconds(1).toEpochMilli(), now.toEpochMilli()))
    }

    @Test
    fun encodedDraftSurvivesARoundTrip() {
        val draft = completeDraft().withNotes("line\nsaid \"hi\"")
        assertEquals(draft, WizardState.decode(draft.encode()))
    }

    private fun completeDraft(): WizardState =
        WizardState.newLog(clock)
            .withType(BristolType.TYPE_4)
            .withSize(StoolSize.MEDIUM)
            .withColor(StoolColor.BROWN)
}
