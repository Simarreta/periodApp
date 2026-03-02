package com.periodapp.data

/**
 * A single period record: start and end timestamps.
 * Used for history and to predict next period.
 */
data class PeriodRecord(
    val startMs: Long,
    val endMs: Long
) {
    val periodLengthDays: Int
        get() = ((endMs - startMs) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
}
