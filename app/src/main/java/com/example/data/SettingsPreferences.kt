package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.wallxDataStore: DataStore<Preferences> by preferencesDataStore(name = "wallx_settings")

enum class ThemeMode {
    LIGHT,
    DARK
}

enum class GalleryLayout {
    GRID_TWO_COL,
    SHOWCASE_SINGLE
}

enum class ApplyTarget {
    ASK_ALWAYS,
    HOME_SCREEN,
    LOCK_SCREEN,
    BOTH_SCREENS
}

enum class SortOrder {
    NEWEST,
    OLDEST,
    NAME
}

@androidx.compose.runtime.Immutable
data class WallxSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val galleryLayout: GalleryLayout = GalleryLayout.GRID_TWO_COL,
    val defaultApplyTarget: ApplyTarget = ApplyTarget.ASK_ALWAYS,
    val showResolutionBadge: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val userRating: Int = 0
)

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode_v4")
        val GALLERY_LAYOUT = stringPreferencesKey("gallery_layout")
        val DEFAULT_APPLY_TARGET = stringPreferencesKey("default_apply_target")
        val SHOW_RESOLUTION_BADGE = booleanPreferencesKey("show_resolution_badge")
        val BUILT_IN_16K_SEEDED = booleanPreferencesKey("built_in_16k_seeded")
        val BUILT_IN_PACK_VERSION = intPreferencesKey("built_in_pack_version")
        val ACCOUNT_NOTIFICATIONS = booleanPreferencesKey("account_notifications_enabled")
        val USER_RATING = intPreferencesKey("user_app_rating")
    }

    val settingsFlow: Flow<WallxSettings> = context.wallxDataStore.data.map { prefs ->
        val resolvedTheme = prefs[Keys.THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.LIGHT

        WallxSettings(
            themeMode = resolvedTheme,
            galleryLayout = GalleryLayout.GRID_TWO_COL,
            defaultApplyTarget = prefs[Keys.DEFAULT_APPLY_TARGET]
                ?.let { runCatching { ApplyTarget.valueOf(it) }.getOrNull() }
                ?: ApplyTarget.ASK_ALWAYS,
            showResolutionBadge = prefs[Keys.SHOW_RESOLUTION_BADGE] ?: true,
            notificationsEnabled = prefs[Keys.ACCOUNT_NOTIFICATIONS] ?: true,
            userRating = (prefs[Keys.USER_RATING] ?: 0).coerceIn(0, 5)
        )
    }

    suspend fun isBuiltIn16kSeeded(): Boolean {
        return context.wallxDataStore.data.first()[Keys.BUILT_IN_16K_SEEDED] ?: false
    }

    suspend fun markBuiltIn16kSeeded() {
        context.wallxDataStore.edit { it[Keys.BUILT_IN_16K_SEEDED] = true }
    }

    suspend fun getBuiltInPackVersion(): Int {
        return context.wallxDataStore.data.first()[Keys.BUILT_IN_PACK_VERSION] ?: 0
    }

    suspend fun setBuiltInPackVersion(version: Int) {
        context.wallxDataStore.edit {
            it[Keys.BUILT_IN_PACK_VERSION] = version
            it[Keys.BUILT_IN_16K_SEEDED] = true
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.wallxDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setGalleryLayout(layout: GalleryLayout) {
        context.wallxDataStore.edit { it[Keys.GALLERY_LAYOUT] = layout.name }
    }

    suspend fun setDefaultApplyTarget(target: ApplyTarget) {
        context.wallxDataStore.edit { it[Keys.DEFAULT_APPLY_TARGET] = target.name }
    }

    suspend fun setShowResolutionBadge(show: Boolean) {
        context.wallxDataStore.edit { it[Keys.SHOW_RESOLUTION_BADGE] = show }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.wallxDataStore.edit { it[Keys.ACCOUNT_NOTIFICATIONS] = enabled }
    }

    /**
     * Strict one-time app rating: once recorded (> 0), it cannot be overwritten.
     */
    suspend fun setUserRatingOnce(stars: Int): Boolean {
        var saved = false
        context.wallxDataStore.edit { prefs ->
            val existing = prefs[Keys.USER_RATING] ?: 0
            if (existing == 0) {
                prefs[Keys.USER_RATING] = stars.coerceIn(1, 5)
                saved = true
            }
        }
        return saved
    }
}
