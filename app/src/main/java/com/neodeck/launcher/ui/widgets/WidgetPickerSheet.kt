package com.neodeck.launcher.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neodeck.launcher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppWidgetGroup(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    onSelectCustomWidget: (type: String, spanX: Int, spanY: Int) -> Unit,
    onSelectSystemWidget: (info: AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var widgetGroups by remember { mutableStateOf<List<AppWidgetGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Expanded state for each app group (default: false unless searching)
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val providers = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appWidgetManager.getInstalledProvidersForProfile(Process.myUserHandle())
                } else {
                    appWidgetManager.installedProviders
                }
            } catch (_: Exception) {
                appWidgetManager.installedProviders
            } ?: emptyList()

            val groups = providers.groupBy { it.provider.packageName }
                .mapNotNull { (pkg, widgets) ->
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        AppWidgetGroup(
                            appName = name,
                            packageName = pkg,
                            appIcon = icon,
                            widgets = widgets.sortedBy { it.loadLabel(pm).toString() }
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                .sortedBy { it.appName.lowercase() }

            widgetGroups = groups
            isLoading = false
        }
    }

    val filteredGroups = remember(widgetGroups, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            widgetGroups
        } else {
            val pm = context.packageManager
            widgetGroups.mapNotNull { group ->
                val matchesApp = group.appName.lowercase().contains(q)
                val matchingWidgets = group.widgets.filter {
                    matchesApp || it.loadLabel(pm).toString().lowercase().contains(q)
                }
                if (matchingWidgets.isNotEmpty()) {
                    group.copy(widgets = matchingWidgets)
                } else null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.widgets_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Widgets & Apps durchsuchen…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Section 1: Neo Launcher Built-in Widgets
                if (searchQuery.isEmpty() || "uhr wetter akku suche neo".contains(searchQuery.lowercase())) {
                    item {
                        Text(
                            text = "Neo Launcher Widgets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    item {
                        BuiltInWidgetCard(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.widget_weather_clock),
                            description = "Live-Uhrzeit, Datum & Open-Meteo Wetter",
                            spanBadge = "4 × 2",
                            onClick = {
                                onSelectCustomWidget("weather_clock", 4, 2)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        BuiltInWidgetCard(
                            icon = Icons.Default.BatteryChargingFull,
                            title = stringResource(R.string.widget_battery),
                            description = "Akkustand, Ladestatus & Schnellzugriff",
                            spanBadge = "4 × 1",
                            onClick = {
                                onSelectCustomWidget("battery", 4, 1)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        BuiltInWidgetCard(
                            icon = Icons.Default.Search,
                            title = stringResource(R.string.widget_search_bar),
                            description = "Google-Websuche & Spracheingabe",
                            spanBadge = "4 × 1",
                            onClick = {
                                onSelectCustomWidget("search_bar", 4, 1)
                                onDismiss()
                            }
                        )
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                // Section 2: Installed System Widgets
                item {
                    Text(
                        text = "System Widgets",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Text(
                                text = "Lade Widgets…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (filteredGroups.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Text(
                                text = "Keine passenden Widgets gefunden",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredGroups, key = { it.packageName }) { group ->
                        val isSearching = searchQuery.isNotEmpty()
                        val isExpanded = isSearching || (expandedStates[group.packageName] ?: false)

                        AppWidgetGroupItem(
                            group = group,
                            context = context,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedStates[group.packageName] = !isExpanded
                            },
                            onSelectWidget = { info ->
                                onSelectSystemWidget(info)
                                onDismiss()
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun BuiltInWidgetCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    spanBadge: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = spanBadge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AppWidgetGroupItem(
    group: AppWidgetGroup,
    context: Context,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit
) {
    val pm = context.packageManager
    val appIconBitmap = remember(group.packageName) {
        group.appIcon?.let { drawableToBitmap(it)?.asImageBitmap() }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Row (App Name, Icon, count, chevron)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp)
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = group.appName,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = group.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${group.widgets.size} Widget${if (group.widgets.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded List of Widgets
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    group.widgets.forEach { widgetInfo ->
                        val widgetLabel = remember(widgetInfo) { widgetInfo.loadLabel(pm).toString() }
                        val previewDrawable = remember(widgetInfo) {
                            try {
                                widgetInfo.loadPreviewImage(context, 0)
                                    ?: widgetInfo.loadIcon(context, 0)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        val previewBitmap = remember(previewDrawable) {
                            previewDrawable?.let { drawableToBitmap(it)?.asImageBitmap() }
                        }

                        val spanX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && widgetInfo.targetCellWidth > 0) {
                            widgetInfo.targetCellWidth.coerceIn(1, 5)
                        } else {
                            ((widgetInfo.minWidth + 30) / 70).coerceIn(1, 5)
                        }
                        val spanY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && widgetInfo.targetCellHeight > 0) {
                            widgetInfo.targetCellHeight.coerceIn(1, 5)
                        } else {
                            ((widgetInfo.minHeight + 30) / 70).coerceIn(1, 5)
                        }

                        Surface(
                            onClick = { onSelectWidget(widgetInfo) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap,
                                        contentDescription = widgetLabel,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = widgetLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "$spanX × $spanY",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(300) else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(300) else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
