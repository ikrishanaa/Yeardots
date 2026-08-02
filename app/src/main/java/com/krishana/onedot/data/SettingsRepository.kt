package com.krishana.onedot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app settings using DataStore.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        
        val PAST_COLOR_KEY = intPreferencesKey("past_color")
        val TODAY_COLOR_KEY = intPreferencesKey("today_color")
        val FUTURE_COLOR_KEY = intPreferencesKey("future_color")
        val BACKGROUND_COLOR_KEY = intPreferencesKey("background_color")
        val DOT_SHAPE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("dot_shape")
        val DOT_DENSITY_KEY = intPreferencesKey("dot_density")
        val LAST_UPDATE_KEY = longPreferencesKey("last_update_timestamp")

        // Default colors (Updated to match requested UI)
        const val DEFAULT_PAST_COLOR = 0xFFD1D5DB.toInt()      // Light Gray
        const val DEFAULT_TODAY_COLOR = 0xFFF97316.toInt()     // Orange
        const val DEFAULT_FUTURE_COLOR = 0xFF262626.toInt()    // Dark Grey
        const val DEFAULT_BACKGROUND_COLOR = 0xFF050505.toInt() // Almost Black
        const val DEFAULT_DOT_SHAPE = "dot"
        const val DEFAULT_DOT_DENSITY = 1 // 0=Tiny, 1=Small, 2=Medium, 3=Large
    }

    val pastColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PAST_COLOR_KEY] ?: DEFAULT_PAST_COLOR
    }

    val todayColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TODAY_COLOR_KEY] ?: DEFAULT_TODAY_COLOR
    }

    val futureColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FUTURE_COLOR_KEY] ?: DEFAULT_FUTURE_COLOR
    }

    val backgroundColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BACKGROUND_COLOR_KEY] ?: DEFAULT_BACKGROUND_COLOR
    }

    val dotShapeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DOT_SHAPE_KEY] ?: DEFAULT_DOT_SHAPE
    }

    val dotDensityFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DOT_DENSITY_KEY] ?: DEFAULT_DOT_DENSITY
    }

    val lastUpdateFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_UPDATE_KEY] ?: 0L
    }

    suspend fun updatePastColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[PAST_COLOR_KEY] = color
        }
    }

    suspend fun updateTodayColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[TODAY_COLOR_KEY] = color
        }
    }

    suspend fun updateFutureColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[FUTURE_COLOR_KEY] = color
        }
    }

    suspend fun updateBackgroundColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_COLOR_KEY] = color
        }
    }

    suspend fun updateDotShape(shape: String) {
        context.dataStore.edit { preferences ->
            preferences[DOT_SHAPE_KEY] = shape
        }
    }

    suspend fun updateDotDensity(density: Int) {
        context.dataStore.edit { preferences ->
            preferences[DOT_DENSITY_KEY] = density
        }
    }

    suspend fun updateLastUpdateTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_UPDATE_KEY] = timestamp
        }
    }

    suspend fun getAllColors(): Map<String, Int> {
        val preferences = context.dataStore.data.first()
        return mapOf(
            "past" to (preferences[PAST_COLOR_KEY] ?: DEFAULT_PAST_COLOR),
            "today" to (preferences[TODAY_COLOR_KEY] ?: DEFAULT_TODAY_COLOR),
            "future" to (preferences[FUTURE_COLOR_KEY] ?: DEFAULT_FUTURE_COLOR),
            "background" to (preferences[BACKGROUND_COLOR_KEY] ?: DEFAULT_BACKGROUND_COLOR)
        )
    }

    /**
     * Synchronous getters for immediate color retrieval
     */
    suspend fun getPastColor(): Int {
        return context.dataStore.data.first()[PAST_COLOR_KEY] ?: DEFAULT_PAST_COLOR
    }

    suspend fun getTodayColor(): Int {
        return context.dataStore.data.first()[TODAY_COLOR_KEY] ?: DEFAULT_TODAY_COLOR
    }

    suspend fun getFutureColor(): Int {
        return context.dataStore.data.first()[FUTURE_COLOR_KEY] ?: DEFAULT_FUTURE_COLOR
    }

    suspend fun getBackgroundColor(): Int {
        return context.dataStore.data.first()[BACKGROUND_COLOR_KEY] ?: DEFAULT_BACKGROUND_COLOR
    }

    suspend fun getDotShape(): String {
        return context.dataStore.data.first()[DOT_SHAPE_KEY] ?: DEFAULT_DOT_SHAPE
    }

    suspend fun getDotDensity(): Int {
        return context.dataStore.data.first()[DOT_DENSITY_KEY] ?: DEFAULT_DOT_DENSITY
    }
}
