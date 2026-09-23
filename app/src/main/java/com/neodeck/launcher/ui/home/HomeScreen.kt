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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    onResizeGridItem: (String, Int, Int) -> Unit = { _, _, _ -> },
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

            // Header Bar: Edit Mode Action Bar only
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
                            onClick = { onAddWidgetClick() }
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
                Spacer(modifier = Modifier.height(8.dp))
            }

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

                val density = LocalDensity.current
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }

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
                                val isWidget = item is CustomWidgetGridItem || item is WidgetGridItem

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(itemWidth, itemHeight)
                                        .offset(x = xOffset, y = yOffset)
                                        .zIndex(if (isEditMode && isSelected) 10f else 1f)
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

                                    // Size Badge and Interactive Resize Handles (for resizable widgets in Edit Mode)
                                    if (isEditMode && isSelected && isWidget) {
                                        // Dimension Badge at Top-Center
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .offset(y = (-14).dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                .zIndex(15f)
                                        ) {
                                            Text(
                                                text = "${item.spanX} × ${item.spanY}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }

                                        // Right Edge Handle (Resize Width / spanX)
                                        var rightDragDx by remember(item.id) { mutableFloatStateOf(0f) }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .offset(x = 7.dp)
                                                .size(width = 14.dp, height = 36.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(7.dp))
                                                .zIndex(15f)
                                                .pointerInput(item.id, item.spanX, item.col) {
                                                    detectDragGestures(
                                                        onDragStart = { rightDragDx = 0f },
                                                        onDragEnd = { rightDragDx = 0f },
                                                        onDragCancel = { rightDragDx = 0f },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            rightDragDx += dragAmount.x
                                                            val deltaCols = (rightDragDx / cellWidthPx).toInt()
                                                            if (deltaCols != 0) {
                                                                val maxSpanX = settings.gridCols - item.col
                                                                val targetSpanX = (item.spanX + deltaCols).coerceIn(1, maxSpanX)
                                                                if (targetSpanX != item.spanX) {
                                                                    onResizeGridItem(item.id, targetSpanX, item.spanY)
                                                                    rightDragDx -= (targetSpanX - item.spanX) * cellWidthPx
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(14.dp)
                                                    .background(MaterialTheme.colorScheme.onPrimary)
                                            )
                                        }

                                        // Bottom Edge Handle (Resize Height / spanY)
                                        var bottomDragDy by remember(item.id) { mutableFloatStateOf(0f) }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .offset(y = 7.dp)
                                                .size(width = 36.dp, height = 14.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(7.dp))
                                                .zIndex(15f)
                                                .pointerInput(item.id, item.spanY, item.row) {
                                                    detectDragGestures(
                                                        onDragStart = { bottomDragDy = 0f },
                                                        onDragEnd = { bottomDragDy = 0f },
                                                        onDragCancel = { bottomDragDy = 0f },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            bottomDragDy += dragAmount.y
                                                            val deltaRows = (bottomDragDy / cellHeightPx).toInt()
                                                            if (deltaRows != 0) {
                                                                val maxSpanY = settings.gridRows - item.row
                                                                val targetSpanY = (item.spanY + deltaRows).coerceIn(1, maxSpanY)
                                                                if (targetSpanY != item.spanY) {
                                                                    onResizeGridItem(item.id, item.spanX, targetSpanY)
                                                                    bottomDragDy -= (targetSpanY - item.spanY) * cellHeightPx
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(14.dp)
                                                    .height(2.dp)
                                                    .background(MaterialTheme.colorScheme.onPrimary)
                                            )
                                        }

                                        // Bottom-Right Corner Handle (Resize Both)
                                        var cornerDx by remember(item.id) { mutableFloatStateOf(0f) }
                                        var cornerDy by remember(item.id) { mutableFloatStateOf(0f) }
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .offset(x = 6.dp, y = 6.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                .zIndex(16f)
                                                .pointerInput(item.id, item.spanX, item.spanY, item.col, item.row) {
                                                    detectDragGestures(
                                                        onDragStart = { cornerDx = 0f; cornerDy = 0f },
                                                        onDragEnd = { cornerDx = 0f; cornerDy = 0f },
                                                        onDragCancel = { cornerDx = 0f; cornerDy = 0f },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            cornerDx += dragAmount.x
                                                            cornerDy += dragAmount.y
                                                            val deltaCols = (cornerDx / cellWidthPx).toInt()
                                                            val deltaRows = (cornerDy / cellHeightPx).toInt()

                                                            val maxSpanX = settings.gridCols - item.col
                                                            val maxSpanY = settings.gridRows - item.row
                                                            val targetSpanX = (item.spanX + deltaCols).coerceIn(1, maxSpanX)
                                                            val targetSpanY = (item.spanY + deltaRows).coerceIn(1, maxSpanY)

                                                            if (targetSpanX != item.spanX || targetSpanY != item.spanY) {
                                                                onResizeGridItem(item.id, targetSpanX, targetSpanY)
                                                                if (targetSpanX != item.spanX) cornerDx -= (targetSpanX - item.spanX) * cellWidthPx
                                                                if (targetSpanY != item.spanY) cornerDy -= (targetSpanY - item.spanY) * cellHeightPx
                                                            }
                                                        }
                                                    )
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onPrimary)
                                            )
                                        }
                                    }

                                    // Quick Remove button in Edit Mode
                                    if (isEditMode) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                                .zIndex(16f)
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
                                    text = { Text(stringResource(R.string.widgets_title)) },
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
    }
}
