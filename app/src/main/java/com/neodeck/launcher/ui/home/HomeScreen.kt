package com.neodeck.launcher.ui.home

import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neodeck.launcher.R
import com.neodeck.launcher.core.data.AppRepository
import com.neodeck.launcher.core.model.AppGridItem
import com.neodeck.launcher.core.model.AppItem
import com.neodeck.launcher.core.model.CustomWidgetGridItem
import com.neodeck.launcher.core.model.FolderGridItem
import com.neodeck.launcher.core.model.GridItem
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.WidgetGridItem
import com.neodeck.launcher.core.weather.WeatherRepository
import com.neodeck.launcher.core.widget.AppWidgetContainer
import com.neodeck.launcher.core.widget.LauncherWidgetHost
import com.neodeck.launcher.ui.components.AppIconView
import com.neodeck.launcher.ui.widgets.BatteryWidget
import com.neodeck.launcher.ui.widgets.ClockWeatherWidget
import com.neodeck.launcher.ui.widgets.SearchBarWidget

@Composable
fun HomeScreen(
    gridItems: List<GridItem>,
    dockItems: List<AppItem>,
    settings: LauncherSettings,
    appRepository: AppRepository,
    weatherRepository: WeatherRepository,
    widgetHost: LauncherWidgetHost? = null,
    appWidgetManager: AppWidgetManager? = null,
    onAppClick: (AppItem) -> Unit,
    onFolderClick: (FolderGridItem) -> Unit,
    onRemoveGridItem: (String) -> Unit,
    onMoveGridItem: (String, Int, Int, Int) -> Unit,
    onAddCustomWidget: (String, Int, Int, Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onPageChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    var selectedItemForMenu by remember { mutableStateOf<GridItem?>(null) }
    var showEmptySpaceMenu by remember { mutableStateOf(false) }
    var showCustomWidgetPicker by remember { mutableStateOf(false) }

    // Edit Mode State
    var isEditMode by remember { mutableStateOf(false) }
    var selectedItemToMove by remember { mutableStateOf<GridItem?>(null) }

    // Jiggle animation when in edit mode
    val infiniteTransition = rememberInfiniteTransition(label = "jiggleTransition")
    val jiggleAngle by infiniteTransition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(120),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleAngle"
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    detectDragGestures { _, dragAmount ->
                        // Swipe up detected -> open AppDrawer
                        if (dragAmount.y < -40f) {
                            onOpenDrawer()
                        }
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

            // Header Bar: SmartBar or Edit Mode Action Bar
            if (isEditMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedItemToMove != null) "Ziel-Zelle antippen" else "Element wählen",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { showCustomWidgetPicker = true }
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Widget", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = {
                                isEditMode = false
                                selectedItemToMove = null
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.edit_mode_done))
                        }
                    }
                }
            } else {
                // At-a-Glance Smart Bar
                SmartBar(
                    onSearchClick = onOpenDrawer,
                    onSettingsClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                    userScrollEnabled = !isEditMode || selectedItemToMove == null,
                    key = { page ->
                        val itemsOnPage = gridItems.filter { it.page == page }
                        "page_${page}_${itemsOnPage.size}_${itemsOnPage.map { it.id }.hashCode()}"
                    },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (isEditMode) {
                                        selectedItemToMove = null
                                    }
                                },
                                onLongClick = {
                                    if (!isEditMode) {
                                        showEmptySpaceMenu = true
                                    }
                                }
                            )
                    ) {
                        val pageItems = gridItems.filter { it.page == page }

                        // Draw Grid Slots in Edit Mode
                        if (isEditMode) {
                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    val isOccupied = pageItems.any { item ->
                                        item != selectedItemToMove &&
                                        c >= item.col && c < item.col + item.spanX &&
                                        r >= item.row && r < item.row + item.spanY
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(cellWidth, cellHeight)
                                            .offset(x = cellWidth * c, y = cellHeight * r)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = 1.dp,
                                                color = if (!isOccupied && selectedItemToMove != null) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = !isOccupied && selectedItemToMove != null) {
                                                selectedItemToMove?.let { movingItem ->
                                                    onMoveGridItem(movingItem.id, page, r, c)
                                                    selectedItemToMove = null
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        // Render Grid Items
                        pageItems.forEach { item ->
                            key(item.id, settings.iconPackPackage) {
                                val xOffset = cellWidth * item.col
                                val yOffset = cellHeight * item.row
                                val itemWidth = cellWidth * item.spanX
                                val itemHeight = cellHeight * item.spanY
                                val isSelected = selectedItemToMove == item

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(itemWidth, itemHeight)
                                        .offset(x = xOffset, y = yOffset)
                                        .rotate(if (isEditMode && !isSelected) jiggleAngle else 0f)
                                        .then(
                                            if (isEditMode && isSelected) {
                                                Modifier.border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(16.dp)
                                                )
                                            } else Modifier
                                        )
                                ) {
                                    when (item) {
                                        is AppGridItem -> {
                                            AppIconView(
                                                app = item.app,
                                                appRepository = appRepository,
                                                onClick = {
                                                    if (isEditMode) {
                                                        selectedItemToMove = if (isSelected) null else item
                                                    } else {
                                                        onAppClick(item.app)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isEditMode) {
                                                        isEditMode = true
                                                        selectedItemToMove = item
                                                    } else {
                                                        selectedItemForMenu = item
                                                    }
                                                },
                                                showLabel = settings.showAppLabels,
                                                iconSize = 52,
                                                iconPackPackage = settings.iconPackPackage
                                            )
                                        }

                                        is FolderGridItem -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        if (isEditMode) {
                                                            selectedItemToMove = if (isSelected) null else item
                                                        } else {
                                                            onFolderClick(item)
                                                        }
                                                    }
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

                                        is CustomWidgetGridItem -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(4.dp)
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (isEditMode) {
                                                                selectedItemToMove = if (isSelected) null else item
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (!isEditMode) {
                                                                isEditMode = true
                                                                selectedItemToMove = item
                                                            } else {
                                                                selectedItemForMenu = item
                                                            }
                                                        }
                                                    )
                                            ) {
                                                when (item.widgetType) {
                                                    "weather_clock" -> ClockWeatherWidget(weatherRepository = weatherRepository)
                                                    "battery" -> BatteryWidget()
                                                    "search_bar" -> SearchBarWidget()
                                                    else -> Box(modifier = Modifier.fillMaxSize())
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
                                                            onClick = {
                                                                if (isEditMode) {
                                                                    selectedItemToMove = if (isSelected) null else item
                                                                }
                                                            },
                                                            onLongClick = {
                                                                if (!isEditMode) {
                                                                    isEditMode = true
                                                                    selectedItemToMove = item
                                                                } else {
                                                                    selectedItemForMenu = item
                                                                }
                                                            }
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

                                    // Quick Remove button in Edit Mode
                                    if (isEditMode) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                                .clickable { onRemoveGridItem(item.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Entfernen",
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Context Menu for grid items (normal mode)
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
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.edit_home)) },
                                                onClick = {
                                                    isEditMode = true
                                                    selectedItemToMove = item
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
                        }

                        // Long-Press Empty Space Menu
                        if (showEmptySpaceMenu) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { showEmptySpaceMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Widgets, null) },
                                    text = { Text(stringResource(R.string.add_launcher_widget)) },
                                    onClick = {
                                        showEmptySpaceMenu = false
                                        showCustomWidgetPicker = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Add, null) },
                                    text = { Text(stringResource(R.string.add_system_widget)) },
                                    onClick = {
                                        showEmptySpaceMenu = false
                                        onAddWidgetClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                                    text = { Text(stringResource(R.string.edit_home)) },
                                    onClick = {
                                        showEmptySpaceMenu = false
                                        isEditMode = true
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

            // Swipe Up Arrow Indicator (only in normal mode)
            if (!isEditMode) {
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
                    key(app.packageName, settings.iconPackPackage) {
                        AppIconView(
                            app = app,
                            appRepository = appRepository,
                            onClick = { onAppClick(app) },
                            showLabel = false,
                            iconSize = 48,
                            iconPackPackage = settings.iconPackPackage
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Custom Widget Picker Dialog
        if (showCustomWidgetPicker) {
            AlertDialog(
                onDismissRequest = { showCustomWidgetPicker = false },
                title = { Text(stringResource(R.string.add_launcher_widget)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    onAddCustomWidget("weather_clock", 4, 2, pagerState.currentPage)
                                    showCustomWidgetPicker = false
                                }
                                .padding(14.dp)
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.widget_weather_clock), fontWeight = FontWeight.Bold)
                                Text("4x2 Kachel mit Live-Wetter", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    onAddCustomWidget("battery", 4, 1, pagerState.currentPage)
                                    showCustomWidgetPicker = false
                                }
                                .padding(14.dp)
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.widget_battery), fontWeight = FontWeight.Bold)
                                Text("4x1 Leiste mit Prozent & Ladekreis", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    onAddCustomWidget("search_bar", 4, 1, pagerState.currentPage)
                                    showCustomWidgetPicker = false
                                }
                                .padding(14.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(stringResource(R.string.widget_search_bar), fontWeight = FontWeight.Bold)
                                Text("4x1 Google-Websuche & Spracheingabe", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomWidgetPicker = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}
