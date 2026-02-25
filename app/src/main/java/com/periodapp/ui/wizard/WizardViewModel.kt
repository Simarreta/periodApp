package com.periodapp.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.periodapp.data.OnboardingDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

data class WizardState(
    val step: Int = 0,
    val lastPeriodStart: Long? = null,
    val lastPeriodEnd: Long? = null,
    val periodLengthDays: Int = 0
)

class WizardViewModel(private val dataStore: OnboardingDataStore) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    fun setLastPeriodStart(dateMs: Long) {
        _state.value = _state.value.copy(lastPeriodStart = dateMs)
    }

    fun setLastPeriodEnd(dateMs: Long) {
        val current = _state.value
        val start = current.lastPeriodStart ?: return
        val days = ((dateMs - start) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
        _state.value = current.copy(
            lastPeriodEnd = dateMs,
            periodLengthDays = days
        )
    }

    fun nextStep() {
        val step = _state.value.step
        if (step < 2) _state.value = _state.value.copy(step = step + 1)
    }

    suspend fun completeOnboarding() {
        val s = _state.value
        val start = s.lastPeriodStart ?: return
        val end = s.lastPeriodEnd ?: return
        val days = s.periodLengthDays.coerceAtLeast(1)
        dataStore.setOnboardingComplete(start, end, days)
    }

    fun back() {
        val step = _state.value.step
        if (step > 0) {
            _state.value = _state.value.copy(step = step - 1)
        }
    }
}
