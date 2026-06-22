package com.katib.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Single DataStore instance for the whole app (shared by app UI and the IME).
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "katib_prefs")

/**
 * Central preference + lightweight stats store. Backed by DataStore so the
 * keyboard service and the main app read the same values. (On iOS this role
 * was filled by App Groups; on Android a shared DataStore in the same app
 * process group is enough.)
 */
class Prefs(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("writing_mode")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CORRECTIONS_WEEK = intPreferencesKey("corrections_week")
        val SUGGESTIONS_ACCEPTED = intPreferencesKey("suggestions_accepted")
        val TOP_ERROR = stringPreferencesKey("top_error")
        val DAY_STAMP = stringPreferencesKey("day_stamp")
        val CORRECTIONS_TODAY = intPreferencesKey("corrections_today")
    }

    val mode: Flow<WritingMode> = context.dataStore.data.map {
        WritingMode.fromWire(it[Keys.MODE])
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.IS_PREMIUM] ?: false
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDED] ?: false
    }

    val stats: Flow<WritingStats> = context.dataStore.data.map {
        WritingStats(
            correctionsThisWeek = it[Keys.CORRECTIONS_WEEK] ?: 0,
            suggestionsAccepted = it[Keys.SUGGESTIONS_ACCEPTED] ?: 0,
            topErrorType = it[Keys.TOP_ERROR] ?: "—",
            correctionsToday = if (it[Keys.DAY_STAMP] == today()) it[Keys.CORRECTIONS_TODAY] ?: 0 else 0,
        )
    }

    suspend fun setMode(mode: WritingMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.wire }
    }

    suspend fun setPremium(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_PREMIUM] = value }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = value }
    }

    /** Increment today's correction counter (rolling the day stamp over at midnight). */
    suspend fun recordCorrectionShown() {
        context.dataStore.edit { p ->
            if (p[Keys.DAY_STAMP] != today()) {
                p[Keys.DAY_STAMP] = today()
                p[Keys.CORRECTIONS_TODAY] = 0
            }
            p[Keys.CORRECTIONS_TODAY] = (p[Keys.CORRECTIONS_TODAY] ?: 0) + 1
            p[Keys.CORRECTIONS_WEEK] = (p[Keys.CORRECTIONS_WEEK] ?: 0) + 1
        }
    }

    suspend fun recordSuggestionAccepted() {
        context.dataStore.edit { it[Keys.SUGGESTIONS_ACCEPTED] = (it[Keys.SUGGESTIONS_ACCEPTED] ?: 0) + 1 }
    }

    suspend fun recordErrorType(type: String) {
        context.dataStore.edit { it[Keys.TOP_ERROR] = type }
    }

    /** How many free corrections remain today; null if premium (unlimited). */
    suspend fun correctionsRemainingToday(premium: Boolean): Int? {
        if (premium) return null
        var used = 0
        context.dataStore.edit { p ->
            used = if (p[Keys.DAY_STAMP] == today()) p[Keys.CORRECTIONS_TODAY] ?: 0 else 0
        }
        return (FreeTier.DAILY_CORRECTION_LIMIT - used).coerceAtLeast(0)
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
