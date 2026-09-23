package com.neodeck.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.neodeck.launcher.core.model.ThemeMode
import com.neodeck.launcher.core.widget.LauncherWidgetHost
import com.neodeck.launcher.ui.LauncherViewModel
import com.neodeck.launcher.ui.drawer.AppDrawer
import com.neodeck.launcher.ui.folder.FolderDialog
import com.neodeck.launcher.ui.home.HomeScreen
import com.neodeck.launcher.ui.home.WallpaperBackground
import com.neodeck.launcher.ui.settings.SettingsScreen
import com.neodeck.launcher.ui.theme.NeoLauncherTheme
import com.neodeck.launcher.ui.widgets.WidgetPickerSheet

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: LauncherWidgetHost

    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProviderInfo: AppWidgetProviderInfo? = null

    // Launcher for binding permission (if system prompt is required)
    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val provider = pendingProviderInfo
            if (provider != null) {
                configureOrAddAllocatedWidget(pendingWidgetId, provider)
            }
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingProviderInfo = null
        }
    }

    // Launcher for configuring a widget (if needed)
    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val provider = pendingProviderInfo
            val span = calculateWidgetSpan(pendingWidgetId, provider)
            viewModel.addWidgetToHome(pendingWidgetId, page = viewModel.currentScreenPage.value, spanX = span.first, spanY = span.second)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingProviderInfo = null
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingProviderInfo = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = LauncherWidgetHost(this)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val gridItems by viewModel.gridItems.collectAsState()
            val dockItems by viewModel.dockItems.collectAsState()
            val filteredApps by viewModel.filteredApps.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
            val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
            val activeFolder by viewModel.activeFolder.collectAsState()
            val selectedDrawerTab by viewModel.selectedDrawerTab.collectAsState()
            val availableIconPacks by viewModel.availableIconPacks.collectAsState()
            val updateState by viewModel.updateState.collectAsState()

            val currentView = LocalView.current
            LaunchedEffect(currentView) {
                viewModel.hapticHelper.attachView(currentView)
            }

            val isDark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            NeoLauncherTheme(darkTheme = isDark) {
                var isWidgetPickerOpen by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Wallpaper Layer (Built-in or System)
                        WallpaperBackground(selectedWallpaper = settings.selectedWallpaper)

                        // Main Homescreen
                        HomeScreen(
                            gridItems = gridItems,
                            dockItems = dockItems,
                            settings = settings,
                            appRepository = viewModel.appRepository,
                            weatherRepository = viewModel.weatherRepository,
                            widgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onFolderClick = { folder -> viewModel.openFolder(folder) },
                            onRemoveGridItem = { id -> viewModel.removeGridItem(id) },
                            onMoveGridItem = { id, page, row, col -> viewModel.moveGridItem(id, page, row, col) },
                            onResizeGridItem = { id, spanX, spanY -> viewModel.resizeGridItem(id, spanX, spanY) },
                            onAddCustomWidget = { type, spanX, spanY, page -> viewModel.addCustomWidgetToHome(type, spanX, spanY, page) },
                            onOpenDrawer = { viewModel.openDrawer() },
                            onOpenSettings = { viewModel.openSettings() },
                            onAddWidgetClick = { isWidgetPickerOpen = true },
                            onPageChanged = { page -> viewModel.setCurrentScreenPage(page) }
                        )

                        // App Drawer Overlay with Slide Animation
                        AnimatedVisibility(
                            visible = isDrawerOpen,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            AppDrawer(
                                apps = filteredApps,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { q -> viewModel.onSearchQueryChange(q) },
                                appRepository = viewModel.appRepository,
                                selectedTab = selectedDrawerTab,
                                onTabSelect = { tab -> viewModel.selectDrawerTab(tab) },
                                onAppClick = { app -> viewModel.launchApp(app) },
                                onAddToHome = { app -> viewModel.addAppToHome(app) },
                                onHideApp = { app -> viewModel.hideApp(app) },
                                onOpenSettings = {
                                    viewModel.closeDrawer()
                                    viewModel.openSettings()
                                },
                                onClose = { viewModel.closeDrawer() },
                                iconPackPackage = settings.iconPackPackage
                            )
                        }

                        // Settings Screen Overlay with Slide Animation
                        AnimatedVisibility(
                            visible = isSettingsOpen,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            SettingsScreen(
                                settings = settings,
                                availableIconPacks = availableIconPacks,
                                updateState = updateState,
                                currentVersionName = viewModel.updateManager.getCurrentVersionName(),
                                onUpdateTheme = { mode -> viewModel.updateTheme(mode) },
                                onUpdateGridSize = { rows, cols -> viewModel.updateGridSize(rows, cols) },
                                onUpdateLanguage = { code -> viewModel.updateLanguage(code) },
                                onUpdateIconPack = { pkg -> viewModel.updateIconPack(pkg) },
                                onUpdateWallpaper = { wp -> viewModel.updateWallpaper(wp) },
                                onUpdateHaptics = { enabled -> viewModel.updateHapticFeedback(enabled) },
                                onUnhideApp = { pkg -> viewModel.unhideApp(pkg) },
                                onCheckForUpdates = { viewModel.checkForUpdates() },
                                onStartDownloadUpdate = { info -> viewModel.startDownloadUpdate(info) },
                                onInstallDownloadedUpdate = { file -> viewModel.installDownloadedUpdate(file) },
                                onDismissUpdate = { viewModel.dismissUpdate() },
                                onClose = { viewModel.closeSettings() }
                            )
                        }

                        // Folder Dialog Overlay
                        activeFolder?.let { folder ->
                            FolderDialog(
                                folder = folder,
                                appRepository = viewModel.appRepository,
                                onAppClick = { app -> viewModel.launchApp(app) },
                                onDismiss = { viewModel.closeFolder() },
                                iconPackPackage = settings.iconPackPackage
                            )
                        }

                        // Widget Picker Sheet Overlay
                        if (isWidgetPickerOpen) {
                            WidgetPickerSheet(
                                appWidgetManager = appWidgetManager,
                                onSelectCustomWidget = { type, spanX, spanY ->
                                    viewModel.addCustomWidgetToHome(type, spanX, spanY, viewModel.currentScreenPage.value)
                                },
                                onSelectSystemWidget = { info ->
                                    handleSystemWidgetSelected(info)
                                },
                                onDismiss = { isWidgetPickerOpen = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not call deleteHost() here, as it unbinds all widgets on Activity recreation!
    }

    private fun handleSystemWidgetSelected(providerInfo: AppWidgetProviderInfo) {
        try {
            val appWidgetId = appWidgetHost.allocateAppWidgetId()
            pendingWidgetId = appWidgetId
            pendingProviderInfo = providerInfo

            val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
            if (canBind) {
                configureOrAddAllocatedWidget(appWidgetId, providerInfo)
            } else {
                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                }
                bindWidgetLauncher.launch(bindIntent)
            }
        } catch (_: Exception) {
            if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                try {
                    appWidgetHost.deleteAppWidgetId(pendingWidgetId)
                } catch (_: Exception) {}
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                pendingProviderInfo = null
            }
        }
    }

    private fun configureOrAddAllocatedWidget(appWidgetId: Int, providerInfo: AppWidgetProviderInfo) {
        if (providerInfo.configure != null) {
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            try {
                configureWidgetLauncher.launch(configIntent)
            } catch (_: Exception) {
                val span = calculateWidgetSpan(appWidgetId, providerInfo)
                viewModel.addWidgetToHome(appWidgetId, page = viewModel.currentScreenPage.value, spanX = span.first, spanY = span.second)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                pendingProviderInfo = null
            }
        } else {
            val span = calculateWidgetSpan(appWidgetId, providerInfo)
            viewModel.addWidgetToHome(appWidgetId, page = viewModel.currentScreenPage.value, spanX = span.first, spanY = span.second)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            pendingProviderInfo = null
        }
    }

    private fun calculateWidgetSpan(appWidgetId: Int, info: AppWidgetProviderInfo?): Pair<Int, Int> {
        val providerInfo = info ?: appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return 2 to 2
        val spanX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && providerInfo.targetCellWidth > 0) {
            providerInfo.targetCellWidth.coerceIn(1, 5)
        } else {
            ((providerInfo.minWidth + 30) / 70).coerceIn(1, 5)
        }
        val spanY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && providerInfo.targetCellHeight > 0) {
            providerInfo.targetCellHeight.coerceIn(1, 5)
        } else {
            ((providerInfo.minHeight + 30) / 70).coerceIn(1, 5)
        }
        return spanX to spanY
    }
}
