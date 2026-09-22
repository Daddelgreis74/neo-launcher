package com.neodeck.launcher.ui.home

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neodeck.launcher.R
import com.neodeck.launcher.core.data.AppRepository
import com.neodeck.launcher.core.model.AppGridItem
import com.neodeck.launcher.core.model.AppItem
import com.neodeck.launcher.core.model.FolderGridItem
import com.neodeck.launcher.core.model.GridItem
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.WidgetGridItem
import com.neodeck.launcher.core.widget.AppWidgetContainer
import com.neodeck.launcher.core.widget.LauncherWidgetHost
import com.neodeck.launcher.ui.components.AppIconView

@Composable
fun HomeScreen(
    gridItems: List<GridItem>,
    dockItems: List<AppItem>,
    settings: LauncherSettings,
    appRepository: AppRepository,
    widgetHost: LauncherWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    onAppClick: (AppItem) -> Unit,
    onFolderClick: (FolderGridItem) -> Unit,
    onRemoveGridItem: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSmartHome: () -> Unit,
    onAddWidgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    var selectedItemForMenu by remember { mutableStateOf<GridItem?>(null) }
    var showEmptySpaceMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    // Swipe up detected -> open AppDrawer
                    if (dragAmount.y < -40f) {
                        onOpenDrawer()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // At-a-Glance Smart Bar with SmartHome button
            SmartBar(
                onSearchClick = onOpenDrawer,
                onSettingsClick = onOpenSettings,
                showSmartHome = settings.smartHomeEnabled,
                onSmartHomeClick = onOpenSmartHome,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Workspace Grid with Horizontal Pager
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val totalWidth = maxWidth
                val totalHeight = maxHeight
                val rows = settings.gridRows
                val cols = settings.gridCols

                val cellWidth = totalWidth / cols
                val cellHeight = totalHeight / rows

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = { showEmptySpaceMenu = true }
                            )
                    ) {
                        val pageItems = gridItems.filter { it.page == page }

                        pageItems.forEach { item ->
                            val xOffset = cellWidth * item.col
                            val yOffset = cellHeight * item.row
                            val itemWidth = cellWidth * item.spanX
                            val itemHeight = cellHeight * item.spanY

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(itemWidth, itemHeight)
                                    .offset(x = xOffset, y = yOffset)
                            ) {
                                when (item) {
                                    is AppGridItem -> {
                                        AppIconView(
                                            app = item.app,
                                            appRepository = appRepository,
                                            onClick = { onAppClick(item.app) },
                                            onLongClick = { selectedItemForMenu = item },
                                            showLabel = settings.showAppLabels,
                                            iconSize = 52
                                        )
                                    }
                                    is FolderGridItem -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onFolderClick(item) }
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = item.title,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            if (settings.showAppLabels) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                    is WidgetGridItem -> {
                                        if (widgetHost != null && appWidgetManager != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .combinedClickable(
                                                        onClick = {},
                                                        onLongClick = { selectedItemForMenu = item }
                                                    )
                                            ) {
                                                AppWidgetContainer(
                                                    appWidgetId = item.appWidgetId,
                                                    widgetHost = widgetHost,
                                                    appWidgetManager = appWidgetManager
                                                )
                                            }
                                        }
                                    }
                                }

                                // Context Menu for grid items
                                if (selectedItemForMenu == item) {
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { selectedItemForMenu = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.remove_from_home)) },
                                            onClick = {
                                                onRemoveGridItem(item.id)
                                                selectedItemForMenu = null
                                            }
                                        )
                                        if (item is AppGridItem) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.app_info)) },
                                                onClick = {
                                                    appRepository.openAppInfo(item.app)
                                                    selectedItemForMenu = null
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.uninstall)) },
                                                onClick = {
                                                    appRepository.uninstallApp(item.app)
                                                    selectedItemForMenu = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Long-Press Empty Space Menu
                        if (showEmptySpaceMenu) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showEmptySpaceMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Add, null) },
                                    text = { Text(stringResource(R.string.add_widget)) },
                                    onClick = {
                                        showEmptySpaceMenu = false
                                        onAddWidgetClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                                    text = { Text(stringResource(R.string.launcher_settings)) },
                                    onClick = {
                                        showEmptySpaceMenu = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Swipe Up Arrow Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDrawer() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.app_drawer),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Persistent Dock
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                dockItems.take(5).forEach { app ->
                    AppIconView(
                        app = app,
                        appRepository = appRepository,
                        onClick = { onAppClick(app) },
                        showLabel = false,
                        iconSize = 48
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
