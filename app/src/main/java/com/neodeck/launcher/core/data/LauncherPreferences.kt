package com.neodeck.launcher.core.data

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LauncherPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("neo_launcher_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private fun loadSettings(): LauncherSettings {
        val themeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        return LauncherSettings(
            gridRows = prefs.getInt("grid_rows", 5),
            gridCols = prefs.getInt("grid_cols", 5),
            dockCount = prefs.getInt("dock_count", 5),
            themeMode = themeMode,
            languageCode = prefs.getString("language_code", "system") ?: "system",
            showAppLabels = prefs.getBoolean("show_app_labels", true)
        )
    }

    fun updateGridSize(rows: Int, cols: Int) {
        prefs.edit {
            putInt("grid_rows", rows)
            putInt("grid_cols", cols)
        }
        _settings.value = loadSettings()
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs.edit {
            putString("theme_mode", mode.name)
        }
        _settings.value = loadSettings()
    }

    fun updateShowAppLabels(show: Boolean) {
        prefs.edit {
            putBoolean("show_app_labels", show)
        }
        _settings.value = loadSettings()
    }

    fun updateLanguage(languageCode: String) {
        prefs.edit {
            putString("language_code", languageCode)
        }
        _settings.value = loadSettings()

        // Apply language via Per-App Language Preferences (Android 13+ / API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (languageCode == "system") {
                localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
            } else {
                localeManager?.applicationLocales = LocaleList(Locale.forLanguageTag(languageCode))
            }
        }
    }
}
