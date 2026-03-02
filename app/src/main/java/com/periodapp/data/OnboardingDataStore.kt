package com.periodapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

class OnboardingDataStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_LAST_PERIOD_START_MS = longPreferencesKey("last_period_start_ms")
        private val KEY_LAST_PERIOD_END_MS = longPreferencesKey("last_period_end_ms")
        private val KEY_PERIOD_LENGTH_DAYS = longPreferencesKey("period_length_days")
        private val KEY_CYCLE_LENGTH_DAYS = intPreferencesKey("cycle_length_days")
        private val KEY_PERIOD_HISTORY = stringPreferencesKey("period_history")
        private const val DEFAULT_CYCLE_DAYS = 27
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete(
        lastPeriodStartMs: Long,
        lastPeriodEndMs: Long,
        periodLengthDays: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = true
            prefs[KEY_LAST_PERIOD_START_MS] = lastPeriodStartMs
            prefs[KEY_LAST_PERIOD_END_MS] = lastPeriodEndMs
            prefs[KEY_PERIOD_LENGTH_DAYS] = periodLengthDays.toLong()
            prefs[KEY_CYCLE_LENGTH_DAYS] = DEFAULT_CYCLE_DAYS
            prefs[KEY_PERIOD_HISTORY] = serializeHistory(
                listOf(PeriodRecord(lastPeriodStartMs, lastPeriodEndMs))
            )
        }
    }

    fun getLastPeriodEndMs(): Flow<Long?> = context.dataStore.data.map {
        it[KEY_LAST_PERIOD_END_MS]
    }

    fun getCycleLengthDays(): Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CYCLE_LENGTH_DAYS]
    }

    suspend fun updatePeriodInfo(
        lastPeriodStartMs: Long,
        lastPeriodEndMs: Long,
        periodLengthDays: Int,
        cycleLengthDays: Int
    ) {
        addPeriodAndUpdateLast(
            lastPeriodStartMs,
            lastPeriodEndMs,
            periodLengthDays,
            cycleLengthDays.coerceIn(21, 45)
        )
    }

    fun getLastPeriodStartMs(): Flow<Long?> = context.dataStore.data.map {
        it[KEY_LAST_PERIOD_START_MS]
    }

    fun getPeriodLengthDays(): Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERIOD_LENGTH_DAYS]?.toInt()
    }

    fun getPeriodHistory(): Flow<List<PeriodRecord>> = context.dataStore.data.map { prefs ->
        parseHistory(prefs[KEY_PERIOD_HISTORY] ?: "")
    }

    /**
     * Saves this period to history (if not already the last entry) and updates
     * "last period" keys used for prediction. Call when the user confirms period start/end.
     */
    suspend fun addPeriodAndUpdateLast(
        lastPeriodStartMs: Long,
        lastPeriodEndMs: Long,
        periodLengthDays: Int,
        cycleLengthDays: Int
    ) {
        context.dataStore.edit { prefs ->
            var list = parseHistory(prefs[KEY_PERIOD_HISTORY] ?: "")
            if (list.isEmpty()) {
                val existingStart = prefs[KEY_LAST_PERIOD_START_MS]
                val existingEnd = prefs[KEY_LAST_PERIOD_END_MS]
                if (existingStart != null && existingEnd != null) {
                    list = listOf(PeriodRecord(existingStart, existingEnd))
                }
            }
            val newRecord = PeriodRecord(lastPeriodStartMs, lastPeriodEndMs)
            val last = list.lastOrNull()
            if (last == null || last.startMs != newRecord.startMs) {
                list = list + newRecord
            }
            prefs[KEY_PERIOD_HISTORY] = serializeHistory(list)
            prefs[KEY_LAST_PERIOD_START_MS] = lastPeriodStartMs
            prefs[KEY_LAST_PERIOD_END_MS] = lastPeriodEndMs
            prefs[KEY_PERIOD_LENGTH_DAYS] = periodLengthDays.toLong()
            prefs[KEY_CYCLE_LENGTH_DAYS] = cycleLengthDays.coerceIn(21, 45)
        }
    }

    /**
     * Updates an existing period in history. If it was the last period, updates "last" keys too.
     */
    suspend fun updatePeriodInHistory(
        oldStartMs: Long,
        newStartMs: Long,
        newEndMs: Long
    ) {
        context.dataStore.edit { prefs ->
            val list = parseHistory(prefs[KEY_PERIOD_HISTORY] ?: "").toMutableList()
            val idx = list.indexOfFirst { it.startMs == oldStartMs }
            if (idx < 0) return@edit
            val newRecord = PeriodRecord(newStartMs, newEndMs)
            list[idx] = newRecord
            prefs[KEY_PERIOD_HISTORY] = serializeHistory(list)
            val last = list.lastOrNull()
            if (last?.startMs == newStartMs) {
                prefs[KEY_LAST_PERIOD_START_MS] = newStartMs
                prefs[KEY_LAST_PERIOD_END_MS] = newEndMs
                prefs[KEY_PERIOD_LENGTH_DAYS] = newRecord.periodLengthDays.toLong()
            }
        }
    }

    /**
     * Removes a period from history by start timestamp. If it was the last period, updates "last" keys to the new last record.
     */
    suspend fun removePeriodFromHistory(startMs: Long) {
        context.dataStore.edit { prefs ->
            val list = parseHistory(prefs[KEY_PERIOD_HISTORY] ?: "").filter { it.startMs != startMs }
            prefs[KEY_PERIOD_HISTORY] = serializeHistory(list)
            val last = list.lastOrNull()
            if (last != null) {
                prefs[KEY_LAST_PERIOD_START_MS] = last.startMs
                prefs[KEY_LAST_PERIOD_END_MS] = last.endMs
                prefs[KEY_PERIOD_LENGTH_DAYS] = last.periodLengthDays.toLong()
            }
        }
    }

    private fun parseHistory(s: String): List<PeriodRecord> {
        if (s.isBlank()) return emptyList()
        return s.lineSequence()
            .map { line ->
                val parts = line.split(',')
                if (parts.size >= 2) {
                    val start = parts[0].toLongOrNull()
                    val end = parts[1].toLongOrNull()
                    if (start != null && end != null) PeriodRecord(start, end) else null
                } else null
            }
            .filterNotNull()
            .toList()
    }

    private fun serializeHistory(list: List<PeriodRecord>): String =
        list.joinToString("\n") { "${it.startMs},${it.endMs}" }
}
