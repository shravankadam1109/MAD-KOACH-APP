package com.koach.studio.settings

import com.koach.studio.core.settings.AppPreferences
import com.koach.studio.core.settings.InMemoryThemePreferencesStore
import com.koach.studio.core.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferencesTest {

    @Test
    fun themePreferencesStore_defaultsToDarkAndUpdatesImmediately() {
        val store = InMemoryThemePreferencesStore()

        // Default brand direction is Dark-first
        assertEquals(ThemeMode.DARK, store.preferences.value.themeMode)

        // Switch to Light and verify immediate state update
        store.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, store.preferences.value.themeMode)

        // Switch to System default and verify state update
        store.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, store.preferences.value.themeMode)
    }

    @Test
    fun themeMode_fromStorageKey_restoresSavedModeAndFallsBackToDarkForUnknownKeys() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("INVALID_MODE"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageKey("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("SYSTEM"))

        // Simulate app restart by hydrating initial AppPreferences from stored key
        val restored = InMemoryThemePreferencesStore(
            AppPreferences(themeMode = ThemeMode.fromStorageKey("LIGHT"))
        )
        assertEquals(ThemeMode.LIGHT, restored.preferences.value.themeMode)
    }

    @Test
    fun settingsViewModel_updatesAppearanceNotificationAndPrivacyPreferences() {
        val store = InMemoryThemePreferencesStore()
        val viewModel = SettingsViewModel(store)

        assertTrue(viewModel.preferences.value.projectNotificationsEnabled)
        assertTrue(viewModel.preferences.value.deadlineRemindersEnabled)
        assertFalse(viewModel.preferences.value.shareCrashDiagnostics)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setProjectNotifications(false)
        viewModel.setDeadlineReminders(false)
        viewModel.setShareCrashDiagnostics(true)

        assertEquals(ThemeMode.LIGHT, viewModel.preferences.value.themeMode)
        assertFalse(viewModel.preferences.value.projectNotificationsEnabled)
        assertFalse(viewModel.preferences.value.deadlineRemindersEnabled)
        assertTrue(viewModel.preferences.value.shareCrashDiagnostics)
    }
}
