package com.neodeck.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neodeck.launcher.R
import com.neodeck.launcher.core.iconpack.IconPackInfo
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.ThemeMode
import com.neodeck.launcher.core.update.UpdateInfo
import com.neodeck.launcher.core.update.UpdateState
import java.io.File

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    availableIconPacks: List<IconPackInfo> = emptyList(),
    updateState: UpdateState = UpdateState.Idle,
    currentVersionName: String = "2.0",
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateGridSize: (Int, Int) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateIconPack: (String?) -> Unit,
    onUpdateWallpaper: (String) -> Unit,
    onUpdateHaptics: (Boolean) -> Unit,
    onUnhideApp: (String) -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onStartDownloadUpdate: (UpdateInfo) -> Unit = {},
    onInstallDownloadedUpdate: (File) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showIconPackDialog by remember { mutableStateOf(false) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }

    BackHandler {
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.launcher_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Appearance & Theming Group
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.theme_mode),
                        subtitle = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        },
                        onClick = { showThemeDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Image,
                        title = "Hintergrundbild",
                        subtitle = "System-Hintergrundbild (Tippen zum Ändern)",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                context.startActivity(Intent.createChooser(intent, "Hintergrundbild wählen"))
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_WALLPAPER_SETTINGS))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Apps,
                        title = stringResource(R.string.icon_pack),
                        subtitle = availableIconPacks.find { it.packageName == settings.iconPackPackage }?.name
                            ?: stringResource(R.string.default_icons),
                        onClick = { showIconPackDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.GridOn,
                        title = stringResource(R.string.grid_size),
                        subtitle = "${settings.gridRows} x ${settings.gridCols}",
                        onClick = { showGridDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.language),
                        subtitle = when (settings.languageCode) {
                            "de" -> "Deutsch"
                            "fr" -> "Français"
                            "es" -> "Español"
                            "en" -> "English"
                            else -> stringResource(R.string.system_default)
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SmartHome & Features
            Text(
                text = "SmartHome & App-Drawer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Verborgene Apps",
                        subtitle = "${settings.hiddenApps.size} Apps ausgeblendet",
                        onClick = { showHiddenAppsDialog = true }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Haptisches Feedback",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Vibration bei Gesten & Tippen",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = { onUpdateHaptics(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // System Integrations Group
            Text(
                text = "System",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Home,
                        title = "Standard-Launcher festlegen",
                        subtitle = "Immer mit Neo Launcher öffnen",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "Nach Updates suchen",
                        subtitle = "Installiert: v$currentVersionName • GitHub Releases",
                        onClick = onCheckForUpdates
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Neo Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version $currentVersionName (Widgets, Icon-Packs, Edge-to-Edge & Android 16 Ready)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }


    // Icon Pack Dialog
    if (showIconPackDialog) {
        AlertDialog(
            onDismissRequest = { showIconPackDialog = false },
            title = { Text(stringResource(R.string.icon_pack)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    availableIconPacks.forEach { pack ->
                        val isSelected = (settings.iconPackPackage == pack.packageName) ||
                                (settings.iconPackPackage == null && pack.packageName == "builtin:default")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateIconPack(if (pack.packageName == "builtin:default") null else pack.packageName)
                                    showIconPackDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onUpdateIconPack(if (pack.packageName == "builtin:default") null else pack.packageName)
                                    showIconPackDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = pack.name, style = MaterialTheme.typography.bodyLarge)
                                if (pack.isBuiltin) {
                                    Text(
                                        text = "Integriertes Design",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPackDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Hidden Apps Dialog
    if (showHiddenAppsDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenAppsDialog = false },
            title = { Text("Verborgene Apps") },
            text = {
                if (settings.hiddenApps.isEmpty()) {
                    Text("Keine Apps ausgeblendet. Tippe im Drawer lange auf eine App, um sie zu verbergen.")
                } else {
                    Column {
                        settings.hiddenApps.forEach { pkg ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = pkg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onUnhideApp(pkg) }) {
                                    Text("Einblenden")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenAppsDialog = false }) {
                    Text("Schließen")
                }
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        val languages = listOf(
            "system" to stringResource(R.string.system_default),
            "de" to "Deutsch",
            "en" to "English",
            "fr" to "Français",
            "es" to "Español"
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    languages.forEach { (code, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = settings.languageCode == code,
                                onClick = {
                                    onUpdateLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        val themes = listOf(
            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
            ThemeMode.LIGHT to stringResource(R.string.theme_light),
            ThemeMode.DARK to stringResource(R.string.theme_dark)
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_mode)) },
            text = {
                Column {
                    themes.forEach { (mode, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateTheme(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = settings.themeMode == mode,
                                onClick = {
                                    onUpdateTheme(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Grid Size Dialog
    if (showGridDialog) {
        val gridOptions = listOf(
            (4 to 4) to "4 x 4",
            (5 to 4) to "5 x 4",
            (5 to 5) to "5 x 5"
        )
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text(stringResource(R.string.grid_size)) },
            text = {
                Column {
                    gridOptions.forEach { (size, label) ->
                        val selected = settings.gridRows == size.first && settings.gridCols == size.second
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateGridSize(size.first, size.second)
                                    showGridDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    onUpdateGridSize(size.first, size.second)
                                    showGridDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGridDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Update States & Dialogs
    when (val state = updateState) {
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Update-Prüfung") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Text("Suche nach neuen Releases auf GitHub…", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text("Neues Update verfügbar! 🎉") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "Version: v${state.info.versionName}" + if (state.info.fileSizeFormatted.isNotBlank()) " (${state.info.fileSizeFormatted})" else "",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Changelog / Versionshinweise:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.info.changelog.ifBlank { "Fehlerbehebungen und Performance-Optimierungen." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onStartDownloadUpdate(state.info) }) {
                        Text("Jetzt herunterladen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text("Später")
                    }
                }
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Update wird heruntergeladen…") },
                text = {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(state.progress * 100).toInt()}% heruntergeladen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text("Update bereit") },
                text = {
                    Text("Das Update v${state.info.versionName} wurde erfolgreich heruntergeladen. Möchten Sie die Installation jetzt starten?")
                },
                confirmButton = {
                    TextButton(onClick = { onInstallDownloadedUpdate(state.apkFile) }) {
                        Text("Installieren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text("Auf neuestem Stand") },
                text = {
                    Text("Neo Launcher ist mit Version v$currentVersionName auf dem aktuellsten Stand. Keine neuen Updates verfügbar.")
                },
                confirmButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text("OK")
                    }
                }
            )
        }

        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismissUpdate,
                title = { Text("Update-Hinweis") },
                text = {
                    Text(state.message)
                },
                confirmButton = {
                    TextButton(onClick = onDismissUpdate) {
                        Text("OK")
                    }
                }
            )
        }

        is UpdateState.Idle -> {}
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
