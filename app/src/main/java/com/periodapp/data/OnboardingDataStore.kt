package com.periodapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
        }
    }

    fun getLastPeriodStartMs(): Flow<Long?> = context.dataStore.data.map {
        it[KEY_LAST_PERIOD_START_MS]
    }

    fun getPeriodLengthDays(): Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERIOD_LENGTH_DAYS]?.toInt()
    }
}
