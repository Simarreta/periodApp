package com.periodapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.periodapp.data.OnboardingDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_CYCLE_DAYS = 27
private const val MS_PER_DAY = 24 * 60 * 60 * 1000L

data class DashboardState(
    val daysUntilNextPeriod: Int? = null,
    val progressInCycle: Float = 0f, // 0f = period start, 1f = next period
    val lastPeriodStartMs: Long? = null,
    val isLoading: Boolean = true
)

class MainViewModel(private val dataStore: OnboardingDataStore) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                dataStore.getLastPeriodStartMs(),
                dataStore.getPeriodLengthDays(),
                dataStore.getCycleLengthDays()
            ) { lastStartMs, periodLengthDays, cycleLengthDays ->
                computeDashboard(
                    lastStartMs,
                    periodLengthDays ?: 5,
                    cycleLengthDays ?: DEFAULT_CYCLE_DAYS
                )
            }.collect { result ->
                _state.update {
                    it.copy(
                        daysUntilNextPeriod = result.daysUntil,
                        progressInCycle = result.progress,
                        lastPeriodStartMs = result.lastStartMs,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun computeDashboard(
        lastPeriodStartMs: Long?,
        periodLengthDays: Int,
        cycleLengthDays: Int
    ): DashboardResult {
        if (lastPeriodStartMs == null) {
            return DashboardResult(daysUntil = null, progress = 0f, lastStartMs = null)
        }
        val now = System.currentTimeMillis()
        val cycleMs = cycleLengthDays * MS_PER_DAY
        var nextPeriodStartMs = lastPeriodStartMs
        while (nextPeriodStartMs <= now) {
            nextPeriodStartMs += cycleMs
        }
        val daysUntil = ((nextPeriodStartMs - now) / MS_PER_DAY).toInt().coerceAtLeast(0)
        val cycleStartMs = nextPeriodStartMs - cycleMs
        val progress = if (cycleMs > 0) {
            ((now - cycleStartMs).toFloat() / cycleMs).coerceIn(0f, 1f)
        } else 0f
        return DashboardResult(
            daysUntil = daysUntil,
            progress = progress,
            lastStartMs = lastPeriodStartMs
        )
    }

    private data class DashboardResult(
        val daysUntil: Int?,
        val progress: Float,
        val lastStartMs: Long?
    )
}
