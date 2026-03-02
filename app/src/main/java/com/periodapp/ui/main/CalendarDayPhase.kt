package com.periodapp.ui.main

import com.periodapp.data.PeriodRecord
import java.util.Calendar
import java.util.Locale

enum class DayPhase { PERIOD, OVULATION, WAIT }

private const val MS_PER_DAY = 24 * 60 * 60 * 1000L
private const val OVULATION_WINDOW_DAYS_BEFORE = 2
private const val OVULATION_WINDOW_DAYS_AFTER = 2

/**
 * For each day 1..daysInMonth, compute phase: PERIOD (in a record), OVULATION (estimated window), or WAIT.
 * Ovulation is estimated as (cycleLength - 14) days after each period start; window ± 2 days.
 */
fun dayPhasesInMonth(
    year: Int,
    month: Int,
    records: List<PeriodRecord>,
    cycleLengthDays: Int
): Map<Int, DayPhase> {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(year, month, 1)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = mutableMapOf<Int, DayPhase>()
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStartMs = cal.timeInMillis
        val dayEndMs = dayStartMs + MS_PER_DAY - 1
        val inPeriod = records.any { it.startMs <= dayEndMs && it.endMs >= dayStartMs }
        if (inPeriod) {
            result[day] = DayPhase.PERIOD
            continue
        }
        val inOvulation = records.any { record ->
            val ovDayMs = record.startMs + (cycleLengthDays - 14) * MS_PER_DAY
            val windowStart = ovDayMs - OVULATION_WINDOW_DAYS_BEFORE * MS_PER_DAY
            val windowEnd = ovDayMs + OVULATION_WINDOW_DAYS_AFTER * MS_PER_DAY
            dayStartMs <= windowEnd && dayEndMs >= windowStart
        }
        result[day] = if (inOvulation) DayPhase.OVULATION else DayPhase.WAIT
    }
    return result
}

/** Find the period record that contains this day (if any). */
fun periodRecordForDay(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    records: List<PeriodRecord>
): PeriodRecord? {
    val cal = Calendar.getInstance(Locale.getDefault())
    cal.set(year, month, dayOfMonth)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val dayStartMs = cal.timeInMillis
    val dayEndMs = dayStartMs + MS_PER_DAY - 1
    return records.find { it.startMs <= dayEndMs && it.endMs >= dayStartMs }
}
