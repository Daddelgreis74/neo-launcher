package com.neodeck.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.neodeck.launcher.core.model.ThemeMode
import com.neodeck.launcher.ui.LauncherViewModel
import com.neodeck.launcher.ui.drawer.AppDrawer
import com.neodeck.launcher.ui.folder.FolderDialog
import com.neodeck.launcher.ui.home.HomeScreen
import com.neodeck.launcher.ui.settings.SettingsScreen
import com.neodeck.launcher.ui.theme.NeoLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val gridItems by viewModel.gridItems.collectAsState()
            val dockItems by viewModel.dockItems.collectAsState()
            val filteredApps by viewModel.filteredApps.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
            val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
            val activeFolder by viewModel.activeFolder.collectAsState()

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
                        // Main Homescreen
                        HomeScreen(
                            gridItems = gridItems,
                            dockItems = dockItems,
                            settings = settings,
                            appRepository = viewModel.appRepository,
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onFolderClick = { folder -> viewModel.openFolder(folder) },
                            onRemoveGridItem = { id -> viewModel.removeGridItem(id) },
                            onOpenDrawer = { viewModel.openDrawer() },
                            onOpenSettings = { viewModel.openSettings() }
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
                                onAppClick = { app -> viewModel.launchApp(app) },
                                onAddToHome = { app -> viewModel.addAppToHome(app) },
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
                                onUpdateTheme = { mode -> viewModel.updateTheme(mode) },
                                onUpdateGridSize = { rows, cols -> viewModel.updateGridSize(rows, cols) },
                                onUpdateLanguage = { code -> viewModel.updateLanguage(code) },
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
}
