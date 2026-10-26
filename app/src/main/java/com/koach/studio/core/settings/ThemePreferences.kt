package com.koach.studio.core.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Supported appearance modes. KOACH Studio is dark-first by default, with full support
 * for Light and System-default modes.
 */
enum class ThemeMode(val displayName: String, val description: String) {
    DARK("Dark", "Signature KOACH charcoal & lime-green studio theme"),
    LIGHT("Light", "Clean neutral surfaces for bright environments"),
    SYSTEM("System default", "Automatically match your device setting");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: DARK
    }
}

/** User-configurable appearance, notification, and privacy preferences. */
data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val projectNotificationsEnabled: Boolean = true,
    val deadlineRemindersEnabled: Boolean = true,
    val shareCrashDiagnostics: Boolean = false
)

/**
 * Reactive store for user appearance and app preferences.
 * Changes emit immediately to [preferences] and persist across restarts in production.
 */
interface ThemePreferencesStore {
    val preferences: StateFlow<AppPreferences>
    fun setThemeMode(mode: ThemeMode)
    fun setProjectNotifications(enabled: Boolean)
    fun setDeadlineReminders(enabled: Boolean)
    fun setShareCrashDiagnostics(enabled: Boolean)
}

/** In-memory implementation used by unit/UI tests and preview containers. */
class InMemoryThemePreferencesStore(
    initial: AppPreferences = AppPreferences()
) : ThemePreferencesStore {
    private val _preferences = MutableStateFlow(initial)
    override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        _preferences.update { it.copy(themeMode = mode) }
    }

    override fun setProjectNotifications(enabled: Boolean) {
        _preferences.update { it.copy(projectNotificationsEnabled = enabled) }
    }

    override fun setDeadlineReminders(enabled: Boolean) {
        _preferences.update { it.copy(deadlineRemindersEnabled = enabled) }
    }

    override fun setShareCrashDiagnostics(enabled: Boolean) {
        _preferences.update { it.copy(shareCrashDiagnostics = enabled) }
    }
}

/**
 * Persistent [SharedPreferences]-backed implementation so theme and notification settings
 * survive process death and app restarts.
 */
class SharedPreferencesThemePreferencesStore(context: Context) : ThemePreferencesStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadFromDisk())
    override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private fun loadFromDisk(): AppPreferences = AppPreferences(
        themeMode = ThemeMode.fromStorageKey(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)),
        projectNotificationsEnabled = prefs.getBoolean(KEY_PROJECT_NOTIFICATIONS, true),
        deadlineRemindersEnabled = prefs.getBoolean(KEY_DEADLINE_REMINDERS, true),
        shareCrashDiagnostics = prefs.getBoolean(KEY_CRASH_DIAGNOSTICS, false)
    )

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _preferences.update { it.copy(themeMode = mode) }
    }

    override fun setProjectNotifications(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_PROJECT_NOTIFICATIONS, enabled) }
        _preferences.update { it.copy(projectNotificationsEnabled = enabled) }
    }

    override fun setDeadlineReminders(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DEADLINE_REMINDERS, enabled) }
        _preferences.update { it.copy(deadlineRemindersEnabled = enabled) }
    }

    override fun setShareCrashDiagnostics(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CRASH_DIAGNOSTICS, enabled) }
        _preferences.update { it.copy(shareCrashDiagnostics = enabled) }
    }

    companion object {
        const val PREFS_NAME = "koach_preferences"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PROJECT_NOTIFICATIONS = "project_notifications_enabled"
        const val KEY_DEADLINE_REMINDERS = "deadline_reminders_enabled"
        const val KEY_CRASH_DIAGNOSTICS = "share_crash_diagnostics"
    }
}
