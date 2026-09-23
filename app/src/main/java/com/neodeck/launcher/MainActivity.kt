package com.neodeck.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
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

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: LauncherWidgetHost

    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    // Launcher for picking a widget
    private val pickWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val appWidgetId = result.data?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                configureOrAddWidget(appWidgetId)
            }
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    // Launcher for configuring a widget (if needed)
    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val span = calculateWidgetSpan(pendingWidgetId)
            viewModel.addWidgetToHome(pendingWidgetId, spanX = span.first, spanY = span.second)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
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
                            widgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onFolderClick = { folder -> viewModel.openFolder(folder) },
                            onRemoveGridItem = { id -> viewModel.removeGridItem(id) },
                            onOpenDrawer = { viewModel.openDrawer() },
                            onOpenSettings = { viewModel.openSettings() },
                            onAddWidgetClick = { startWidgetPick() },
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
                                onClose = { viewModel.closeDrawer() }
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
                                onDismiss = { viewModel.closeFolder() }
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
        try {
            appWidgetHost.deleteHost()
        } catch (_: Exception) {}
    }

    private fun startWidgetPick() {
        try {
            pendingWidgetId = appWidgetHost.allocateAppWidgetId()
            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
            }
            pickWidgetLauncher.launch(pickIntent)
        } catch (_: Exception) {}
    }

    private fun configureOrAddWidget(appWidgetId: Int) {
        val info: AppWidgetProviderInfo? = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info?.configure != null) {
            pendingWidgetId = appWidgetId
            val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            try {
                configureWidgetLauncher.launch(configIntent)
            } catch (_: Exception) {
                // If configure fails to launch, add directly
                val span = calculateWidgetSpan(appWidgetId)
                viewModel.addWidgetToHome(appWidgetId, spanX = span.first, spanY = span.second)
            }
        } else {
            val span = calculateWidgetSpan(appWidgetId)
            viewModel.addWidgetToHome(appWidgetId, spanX = span.first, spanY = span.second)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    private fun calculateWidgetSpan(appWidgetId: Int): Pair<Int, Int> {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return 2 to 2
        val spanX = ((info.minWidth + 30) / 70).coerceIn(1, 4)
        val spanY = ((info.minHeight + 30) / 70).coerceIn(1, 4)
        return spanX to spanY
    }
}
