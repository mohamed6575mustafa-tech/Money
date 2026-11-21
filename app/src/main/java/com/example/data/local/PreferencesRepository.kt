package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.ContrastUtils
import com.example.domain.ThemeModePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "masrouf_preferences")

data class UserPreferences(
    val themeMode: ThemeModePreference = ThemeModePreference.SYSTEM,
    val primaryColorArgb: Long = ContrastUtils.DEFAULT_PRIMARY_ARGB,
    val secondaryColorArgb: Long = ContrastUtils.DEFAULT_SECONDARY_ARGB,
    val accentColorArgb: Long = ContrastUtils.DEFAULT_ACCENT_ARGB,
    val localOnlyMode: Boolean = false,
    val currencyCode: String = "EGP",
    val budgetAlertsEnabled: Boolean = true,
    val inventoryAlertsEnabled: Boolean = true,
    val weeklyReviewReminderEnabled: Boolean = true,
    val activeMemberName: String = "رب الأسرة",
    val lastWeeklyReviewMillis: Long = 0L
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PRIMARY_COLOR = longPreferencesKey("primary_color_argb")
        val SECONDARY_COLOR = longPreferencesKey("secondary_color_argb")
        val ACCENT_COLOR = longPreferencesKey("accent_color_argb")
        val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts_enabled")
        val INVENTORY_ALERTS = booleanPreferencesKey("inventory_alerts_enabled")
        val WEEKLY_REMINDER = booleanPreferencesKey("weekly_review_reminder")
        val ACTIVE_MEMBER_NAME = stringPreferencesKey("active_member_name")
        val LAST_WEEKLY_REVIEW = longPreferencesKey("last_weekly_review_millis")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeModePreference.fromRaw(prefs[Keys.THEME_MODE]),
            primaryColorArgb = prefs[Keys.PRIMARY_COLOR] ?: ContrastUtils.DEFAULT_PRIMARY_ARGB,
            secondaryColorArgb = prefs[Keys.SECONDARY_COLOR] ?: ContrastUtils.DEFAULT_SECONDARY_ARGB,
            accentColorArgb = prefs[Keys.ACCENT_COLOR] ?: ContrastUtils.DEFAULT_ACCENT_ARGB,
            localOnlyMode = prefs[Keys.LOCAL_ONLY_MODE] ?: false,
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: "EGP",
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS] ?: true,
            inventoryAlertsEnabled = prefs[Keys.INVENTORY_ALERTS] ?: true,
            weeklyReviewReminderEnabled = prefs[Keys.WEEKLY_REMINDER] ?: true,
            activeMemberName = prefs[Keys.ACTIVE_MEMBER_NAME] ?: "رب الأسرة",
            lastWeeklyReviewMillis = prefs[Keys.LAST_WEEKLY_REVIEW] ?: 0L
        )
    }

    suspend fun setThemeColors(primaryArgb: Long, secondaryArgb: Long, accentArgb: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIMARY_COLOR] = primaryArgb
            prefs[Keys.SECONDARY_COLOR] = secondaryArgb
            prefs[Keys.ACCENT_COLOR] = accentArgb
        }
    }

    suspend fun resetThemeColors() {
        setThemeColors(
            ContrastUtils.DEFAULT_PRIMARY_ARGB,
            ContrastUtils.DEFAULT_SECONDARY_ARGB,
            ContrastUtils.DEFAULT_ACCENT_ARGB
        )
    }

    suspend fun setThemeMode(mode: ThemeModePreference) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLocalOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCAL_ONLY_MODE] = enabled }
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setNotificationSettings(budget: Boolean, inventory: Boolean, weekly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUDGET_ALERTS] = budget
            prefs[Keys.INVENTORY_ALERTS] = inventory
            prefs[Keys.WEEKLY_REMINDER] = weekly
        }
    }

    suspend fun setActiveMemberName(name: String) {
        context.dataStore.edit { it[Keys.ACTIVE_MEMBER_NAME] = name }
    }

    suspend fun markWeeklyReviewCompleted() {
        context.dataStore.edit { it[Keys.LAST_WEEKLY_REVIEW] = System.currentTimeMillis() }
    }
}
