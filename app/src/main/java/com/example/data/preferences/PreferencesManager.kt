package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dompetku_settings")

class PreferencesManager(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CYCLE_START_DAY = intPreferencesKey("cycle_start_day")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    /** Default monthly cycle start day is 26 (26th to 25th of next month). */
    val cycleStartDayFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[Keys.CYCLE_START_DAY] ?: DEFAULT_CYCLE_START_DAY).coerceIn(1, 31)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setCycleStartDay(day: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CYCLE_START_DAY] = day.coerceIn(1, 31)
        }
    }

    companion object {
        const val DEFAULT_CYCLE_START_DAY = 26
    }
}
