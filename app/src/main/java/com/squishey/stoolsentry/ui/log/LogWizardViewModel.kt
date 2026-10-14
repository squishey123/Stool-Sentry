package com.squishey.stoolsentry.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.domain.AppClock
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.domain.BristolType
import com.squishey.stoolsentry.domain.LogValidator
import com.squishey.stoolsentry.domain.StoolColor
import com.squishey.stoolsentry.domain.StoolSize
import com.squishey.stoolsentry.domain.Symptoms
import com.squishey.stoolsentry.domain.WizardState
import com.squishey.stoolsentry.ui.nav.RoutePatterns
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface WizardUiState {
    data object Loading : WizardUiState

    data class Ready(
        val draft: WizardState,
        val saving: Boolean,
    ) : WizardUiState
}

class LogWizardViewModel(
    private val repository: LogRepository,
    private val clock: AppClock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<WizardUiState>(WizardUiState.Loading)
    val uiState: StateFlow<WizardUiState> = state

    private val savedEvents = MutableSharedFlow<BowelLog>(extraBufferCapacity = 1)
    val saved: SharedFlow<BowelLog> = savedEvents.asSharedFlow()

    init {
        val restored = savedStateHandle.get<String>(WIZARD_KEY)?.let(WizardState::decode)
        if (restored != null) {
            state.value = WizardUiState.Ready(restored, saving = false)
        } else {
            viewModelScope.launch {
                val logId = savedStateHandle.get<Long>(RoutePatterns.ARG_LOG_ID)?.takeIf { it > 0L }
                val presetDate = savedStateHandle.get<String>(RoutePatterns.ARG_DATE)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val existing = logId?.let { repository.get(it) }
                val draft = if (existing != null) {
                    WizardState.fromLog(existing)
                } else {
                    WizardState.newLog(clock, presetDate)
                }
                publish(draft, saving = false)
            }
        }
    }

    fun selectType(type: BristolType) = update { it.withType(type) }

    fun selectSize(size: StoolSize) = update { it.withSize(size) }

    fun selectColor(color: StoolColor) = update { it.withColor(color) }

    fun setSymptoms(symptoms: Symptoms) = update { it.withSymptoms(symptoms) }

    fun setNotes(notes: String) = update { it.withNotes(notes) }

    fun setDateTime(date: LocalDate, time: LocalTime) = update { it.withDateTime(date, time) }

    fun next() = update { it.next() }

    /** True when the draft's date and time are later than now. */
    fun isInFuture(draft: WizardState): Boolean =
        LogValidator.isFuture(draft.occurredAtMillis, clock.instant().toEpochMilli())

    fun canSave(draft: WizardState): Boolean = draft.canSave(clock.instant().toEpochMilli())

    fun today(): LocalDate = clock.today()

    /** Returns true when the screen should close without saving. */
    fun back(): Boolean {
        val ready = state.value as? WizardUiState.Ready ?: return false
        if (ready.saving) return false
        val previous = ready.draft.back()
        if (previous == null) {
            savedStateHandle.remove<String>(WIZARD_KEY)
            return true
        }
        publish(previous, saving = false)
        return false
    }

    fun save() {
        val ready = state.value as? WizardUiState.Ready ?: return
        if (ready.saving) return
        val nowMillis = clock.instant().toEpochMilli()
        val log = ready.draft.toBowelLog(nowMillis) ?: return
        publish(ready.draft, saving = true)
        viewModelScope.launch {
            try {
                val id = repository.save(log)
                val stored = log.copy(id = id)
                publish(ready.draft.copy(existingId = id), saving = false)
                savedEvents.emit(stored)
            } finally {
                val current = state.value
                if (current is WizardUiState.Ready && current.saving) {
                    publish(current.draft, saving = false)
                }
            }
        }
    }

    private fun update(transform: (WizardState) -> WizardState) {
        val ready = state.value as? WizardUiState.Ready ?: return
        if (ready.saving) return
        publish(transform(ready.draft), saving = false)
    }

    private fun publish(draft: WizardState, saving: Boolean) {
        state.value = WizardUiState.Ready(draft, saving)
        savedStateHandle[WIZARD_KEY] = draft.encode()
    }

    private companion object {
        const val WIZARD_KEY = "wizard_state"
    }
}
