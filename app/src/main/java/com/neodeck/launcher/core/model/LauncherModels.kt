package com.neodeck.launcher.core.model

import android.os.UserHandle

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val userHandle: UserHandle? = null,
    val isSystemApp: Boolean = false
)

sealed interface GridItem {
    val id: String
    val page: Int
    val row: Int
    val col: Int
    val spanX: Int
    val spanY: Int
}

data class AppGridItem(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val col: Int,
    val app: AppItem,
    override val spanX: Int = 1,
    override val spanY: Int = 1
) : GridItem

data class FolderGridItem(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val col: Int,
    val title: String,
    val apps: List<AppItem>,
    override val spanX: Int = 1,
    override val spanY: Int = 1
) : GridItem

data class WidgetGridItem(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val col: Int,
    val appWidgetId: Int,
    override val spanX: Int = 2,
    override val spanY: Int = 2
) : GridItem

data class CustomWidgetGridItem(
    override val id: String,
    override val page: Int,
    override val row: Int,
    override val col: Int,
    val widgetType: String, // "weather_clock", "battery", "search_bar"
    override val spanX: Int = 4,
    override val spanY: Int = 2
) : GridItem

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class LauncherSettings(
    val gridRows: Int = 5,
    val gridCols: Int = 5,
    val dockCount: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = "system",
    val showAppLabels: Boolean = true,
    val iconPackPackage: String? = null,
    val hiddenApps: Set<String> = emptySet(),
    val selectedWallpaper: String = "aurora",
    val hapticFeedbackEnabled: Boolean = true
)
