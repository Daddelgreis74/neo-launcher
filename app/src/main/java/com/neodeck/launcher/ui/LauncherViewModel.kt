package com.neodeck.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neodeck.launcher.core.data.AppRepository
import com.neodeck.launcher.core.data.GridRepository
import com.neodeck.launcher.core.data.LauncherPreferences
import com.neodeck.launcher.core.haptic.HapticHelper
import com.neodeck.launcher.core.iconpack.IconPackInfo
import com.neodeck.launcher.core.model.AppGridItem
import com.neodeck.launcher.core.model.AppItem
import com.neodeck.launcher.core.model.FolderGridItem
import com.neodeck.launcher.core.model.GridItem
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.ThemeMode
import com.neodeck.launcher.core.model.WidgetGridItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val appRepository = AppRepository(application)
    val preferences = LauncherPreferences(application)
    val gridRepository = GridRepository(application, appRepository)
    val hapticHelper = HapticHelper(application)

    val allApps: StateFlow<List<AppItem>> = appRepository.apps
    val settings: StateFlow<LauncherSettings> = preferences.settings
    val gridItems: StateFlow<List<GridItem>> = gridRepository.gridItems
    val dockItems: StateFlow<List<AppItem>> = gridRepository.dockItems

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _activeFolder = MutableStateFlow<FolderGridItem?>(null)
    val activeFolder: StateFlow<FolderGridItem?> = _activeFolder.asStateFlow()

    private val _selectedDrawerTab = MutableStateFlow(0) // 0: All, 1: Favorites
    val selectedDrawerTab: StateFlow<Int> = _selectedDrawerTab.asStateFlow()

    private val _availableIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val availableIconPacks: StateFlow<List<IconPackInfo>> = _availableIconPacks.asStateFlow()

    private val _currentScreenPage = MutableStateFlow(0)
    val currentScreenPage: StateFlow<Int> = _currentScreenPage.asStateFlow()

    fun setCurrentScreenPage(page: Int) {
        _currentScreenPage.value = page
    }

    init {
        // Load icon pack if configured
        viewModelScope.launch {
            preferences.settings.collect { currentSettings ->
                appRepository.applyIconPack(currentSettings.iconPackPackage)
            }
        }
        loadInstalledIconPacks()
    }

    fun loadInstalledIconPacks() {
        viewModelScope.launch {
            _availableIconPacks.value = appRepository.iconPackManager.getInstalledIconPacks()
        }
    }

    val filteredApps: StateFlow<List<AppItem>> = combine(
        allApps,
        searchQuery,
        settings,
        selectedDrawerTab,
        dockItems
    ) { apps, query, currentSettings, tab, currentDock ->
        // Filter out hidden apps unless searching
        val nonHidden = if (query.isBlank()) {
            apps.filter { it.packageName !in currentSettings.hiddenApps }
        } else {
            apps
        }

        val tabFiltered = if (tab == 1 && query.isBlank()) {
            // Favorites tab shows dock apps and first page apps
            (currentDock + nonHidden.take(8)).distinctBy { it.packageName }
        } else {
            nonHidden
        }

        if (query.isBlank()) {
            tabFiltered
        } else {
            val q = query.trim().lowercase()
            tabFiltered.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDrawerTab(tab: Int) {
        _selectedDrawerTab.value = tab
        triggerHapticTick()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
        triggerHapticClick()
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
        _searchQuery.value = ""
    }

    fun openSettings() {
        _isSettingsOpen.value = true
        triggerHapticClick()
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openFolder(folder: FolderGridItem) {
        _activeFolder.value = folder
        triggerHapticClick()
    }

    fun closeFolder() {
        _activeFolder.value = null
    }

    fun launchApp(app: AppItem) {
        triggerHapticClick()
        appRepository.launchApp(app)
        closeDrawer()
        closeFolder()
    }

    fun openAppInfo(app: AppItem) {
        appRepository.openAppInfo(app)
    }

    fun uninstallApp(app: AppItem) {
        appRepository.uninstallApp(app)
    }

    fun hideApp(app: AppItem) {
        preferences.hideApp(app.packageName)
        triggerHapticClick()
    }

    fun unhideApp(packageName: String) {
        preferences.unhideApp(packageName)
        triggerHapticClick()
    }

    fun addAppToHome(app: AppItem, targetPage: Int? = null) {
        val preferredPage = targetPage ?: _currentScreenPage.value
        val maxRows = settings.value.gridRows
        val maxCols = settings.value.gridCols

        // Check preferred page first, then other pages (0..2)
        val pagesToCheck = listOf(preferredPage) + (0..2).filter { it != preferredPage }
        for (page in pagesToCheck) {
            val currentItems = gridItems.value.filter { it.page == page }
            for (r in 0 until maxRows) {
                for (c in 0 until maxCols) {
                    val occupied = currentItems.any { it.row == r && it.col == c }
                    if (!occupied) {
                        val item = AppGridItem(
                            id = UUID.randomUUID().toString(),
                            page = page,
                            row = r,
                            col = c,
                            app = app
                        )
                        gridRepository.addItemToGrid(item)
                        triggerHapticClick()
                        return
                    }
                }
            }
        }
    }

    fun addWidgetToHome(appWidgetId: Int, page: Int = 0, spanX: Int = 2, spanY: Int = 2) {
        val item = WidgetGridItem(
            id = UUID.randomUUID().toString(),
            page = page,
            row = 1,
            col = 0,
            appWidgetId = appWidgetId,
            spanX = spanX,
            spanY = spanY
        )
        gridRepository.addItemToGrid(item)
        triggerHapticClick()
    }

    fun removeGridItem(id: String) {
        gridRepository.removeItemFromGrid(id)
        triggerHapticClick()
    }

    fun moveGridItem(id: String, newPage: Int, newRow: Int, newCol: Int) {
        gridRepository.moveItem(id, newPage, newRow, newCol)
        triggerHapticTick()
    }

    fun addAppToDock(app: AppItem) {
        gridRepository.addAppToDock(app)
        triggerHapticClick()
    }

    fun removeAppFromDock(app: AppItem) {
        gridRepository.removeAppFromDock(app)
        triggerHapticClick()
    }

    fun updateGridSize(rows: Int, cols: Int) {
        preferences.updateGridSize(rows, cols)
    }

    fun updateTheme(mode: ThemeMode) {
        preferences.updateThemeMode(mode)
    }

    fun updateLanguage(code: String) {
        preferences.updateLanguage(code)
    }

    fun updateIconPack(packageName: String?) {
        preferences.updateIconPack(packageName)
        triggerHapticClick()
    }

    fun updateWallpaper(wallpaperKey: String) {
        preferences.updateWallpaper(wallpaperKey)
        triggerHapticClick()
    }

    fun updateHapticFeedback(enabled: Boolean) {
        preferences.updateHapticFeedback(enabled)
    }

    fun triggerHapticClick() {
        hapticHelper.performClick(settings.value.hapticFeedbackEnabled)
    }

    fun triggerHapticTick() {
        hapticHelper.performTick(settings.value.hapticFeedbackEnabled)
    }

    fun triggerHapticHeavy() {
        hapticHelper.performHeavyClick(settings.value.hapticFeedbackEnabled)
    }
}
