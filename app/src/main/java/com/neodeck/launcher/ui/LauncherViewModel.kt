package com.neodeck.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neodeck.launcher.core.data.AppRepository
import com.neodeck.launcher.core.data.GridRepository
import com.neodeck.launcher.core.data.LauncherPreferences
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
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val appRepository = AppRepository(application)
    val preferences = LauncherPreferences(application)
    val gridRepository = GridRepository(application, appRepository)

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

    val filteredApps: StateFlow<List<AppItem>> = combine(allApps, searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            val q = query.trim().lowercase()
            apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
        _searchQuery.value = ""
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openFolder(folder: FolderGridItem) {
        _activeFolder.value = folder
    }

    fun closeFolder() {
        _activeFolder.value = null
    }

    fun launchApp(app: AppItem) {
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

    fun addAppToHome(app: AppItem, page: Int = 0) {
        val currentItems = gridItems.value.filter { it.page == page }
        val maxRows = settings.value.gridRows
        val maxCols = settings.value.gridCols

        // Find first empty cell
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
                    return
                }
            }
        }
    }

    fun removeGridItem(id: String) {
        gridRepository.removeItemFromGrid(id)
    }

    fun addAppToDock(app: AppItem) {
        gridRepository.addAppToDock(app)
    }

    fun removeAppFromDock(app: AppItem) {
        gridRepository.removeAppFromDock(app)
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
}
