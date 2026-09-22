package com.neodeck.launcher.core.data

import android.content.Context
import com.neodeck.launcher.core.model.AppGridItem
import com.neodeck.launcher.core.model.AppItem
import com.neodeck.launcher.core.model.FolderGridItem
import com.neodeck.launcher.core.model.GridItem
import com.neodeck.launcher.core.model.WidgetGridItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class GridRepository(
    private val context: Context,
    private val appRepository: AppRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gridFile = File(context.filesDir, "neo_grid_items.json")
    private val dockFile = File(context.filesDir, "neo_dock_items.json")

    private val _gridItems = MutableStateFlow<List<GridItem>>(emptyList())
    val gridItems: StateFlow<List<GridItem>> = _gridItems.asStateFlow()

    private val _dockItems = MutableStateFlow<List<AppItem>>(emptyList())
    val dockItems: StateFlow<List<AppItem>> = _dockItems.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            if (gridFile.exists()) {
                _gridItems.value = parseGridItems(gridFile.readText())
            }
            if (dockFile.exists()) {
                _dockItems.value = parseDockItems(dockFile.readText())
            }

            // If dock is empty, initialize default dock apps once apps are loaded
            if (_dockItems.value.isEmpty()) {
                initDefaultDock()
            }
        }
    }

    private fun initDefaultDock() {
        scope.launch {
            // Wait for apps to load if needed
            val availableApps = appRepository.apps.value
            if (availableApps.isNotEmpty()) {
                val defaultDock = mutableListOf<AppItem>()
                // Pick common default apps: Phone, Messages, Browser, Camera
                for (app in availableApps) {
                    val pkg = app.packageName.lowercase()
                    if (defaultDock.size < 5) {
                        if (pkg.contains("dialer") || pkg.contains("phone") ||
                            pkg.contains("message") || pkg.contains("chrome") ||
                            pkg.contains("browser") || pkg.contains("camera")
                        ) {
                            if (!defaultDock.contains(app)) {
                                defaultDock.add(app)
                            }
                        }
                    }
                }
                if (defaultDock.isEmpty()) {
                    defaultDock.addAll(availableApps.take(5))
                }
                _dockItems.value = defaultDock
                saveDock()
            }
        }
    }

    fun addItemToGrid(item: GridItem) {
        val current = _gridItems.value.toMutableList()
        // Remove item at same page/row/col if overlapping
        current.removeAll { it.page == item.page && it.row == item.row && it.col == item.col }
        current.add(item)
        _gridItems.value = current.toList()
        saveGrid()
    }

    fun removeItemFromGrid(itemId: String) {
        val current = _gridItems.value.toMutableList()
        current.removeAll { it.id == itemId }
        _gridItems.value = current.toList()
        saveGrid()
    }

    fun moveItem(itemId: String, newPage: Int, newRow: Int, newCol: Int) {
        val current = _gridItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val item = current[index]
            val updated: GridItem = when (item) {
                is AppGridItem -> item.copy(page = newPage, row = newRow, col = newCol)
                is FolderGridItem -> item.copy(page = newPage, row = newRow, col = newCol)
                is WidgetGridItem -> item.copy(page = newPage, row = newRow, col = newCol)
            }
            current[index] = updated
            _gridItems.value = current.toList()
            saveGrid()
        }
    }

    fun createFolder(title: String, page: Int, row: Int, col: Int, apps: List<AppItem>): FolderGridItem {
        val folder = FolderGridItem(
            id = UUID.randomUUID().toString(),
            page = page,
            row = row,
            col = col,
            title = title,
            apps = apps
        )
        addItemToGrid(folder)
        return folder
    }

    fun updateFolder(folderId: String, newTitle: String, newApps: List<AppItem>) {
        val current = _gridItems.value.toMutableList()
        val index = current.indexOfFirst { it.id == folderId }
        if (index != -1) {
            val existing = current[index]
            if (existing is FolderGridItem) {
                if (newApps.isEmpty()) {
                    current.removeAt(index)
                } else {
                    current[index] = existing.copy(title = newTitle, apps = newApps)
                }
                _gridItems.value = current.toList()
                saveGrid()
            }
        }
    }

    fun addAppToDock(app: AppItem) {
        val current = _dockItems.value.toMutableList()
        if (current.size < 5 && !current.contains(app)) {
            current.add(app)
            _dockItems.value = current.toList()
            saveDock()
        }
    }

    fun removeAppFromDock(app: AppItem) {
        val current = _dockItems.value.toMutableList()
        current.remove(app)
        _dockItems.value = current.toList()
        saveDock()
    }

    private fun saveGrid() {
        scope.launch {
            try {
                val array = JSONArray()
                for (item in _gridItems.value) {
                    val obj = JSONObject()
                    obj.put("id", item.id)
                    obj.put("page", item.page)
                    obj.put("row", item.row)
                    obj.put("col", item.col)
                    obj.put("spanX", item.spanX)
                    obj.put("spanY", item.spanY)

                    when (item) {
                        is AppGridItem -> {
                            obj.put("type", "app")
                            obj.put("label", item.app.label)
                            obj.put("pkg", item.app.packageName)
                            obj.put("activity", item.app.activityName)
                        }
                        is FolderGridItem -> {
                            obj.put("type", "folder")
                            obj.put("title", item.title)
                            val appsArr = JSONArray()
                            for (app in item.apps) {
                                val aObj = JSONObject()
                                aObj.put("label", app.label)
                                aObj.put("pkg", app.packageName)
                                aObj.put("activity", app.activityName)
                                appsArr.put(aObj)
                            }
                            obj.put("apps", appsArr)
                        }
                        is WidgetGridItem -> {
                            obj.put("type", "widget")
                            obj.put("appWidgetId", item.appWidgetId)
                        }
                    }
                    array.put(obj)
                }
                gridFile.writeText(array.toString())
            } catch (_: Exception) {}
        }
    }

    private fun saveDock() {
        scope.launch {
            try {
                val array = JSONArray()
                for (app in _dockItems.value) {
                    val obj = JSONObject()
                    obj.put("label", app.label)
                    obj.put("pkg", app.packageName)
                    obj.put("activity", app.activityName)
                    array.put(obj)
                }
                dockFile.writeText(array.toString())
            } catch (_: Exception) {}
        }
    }

    private fun parseGridItems(json: String): List<GridItem> {
        val list = mutableListOf<GridItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val page = obj.getInt("page")
                val row = obj.getInt("row")
                val col = obj.getInt("col")
                val spanX = obj.optInt("spanX", 1)
                val spanY = obj.optInt("spanY", 1)
                val type = obj.getString("type")

                when (type) {
                    "app" -> {
                        val app = AppItem(
                            label = obj.getString("label"),
                            packageName = obj.getString("pkg"),
                            activityName = obj.getString("activity")
                        )
                        list.add(AppGridItem(id, page, row, col, app, spanX, spanY))
                    }
                    "folder" -> {
                        val title = obj.getString("title")
                        val appsArr = obj.getJSONArray("apps")
                        val folderApps = mutableListOf<AppItem>()
                        for (j in 0 until appsArr.length()) {
                            val aObj = appsArr.getJSONObject(j)
                            folderApps.add(
                                AppItem(
                                    label = aObj.getString("label"),
                                    packageName = aObj.getString("pkg"),
                                    activityName = aObj.getString("activity")
                                )
                            )
                        }
                        list.add(FolderGridItem(id, page, row, col, title, folderApps, spanX, spanY))
                    }
                    "widget" -> {
                        val widgetId = obj.getInt("appWidgetId")
                        list.add(WidgetGridItem(id, page, row, col, widgetId, spanX, spanY))
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseDockItems(json: String): List<AppItem> {
        val list = mutableListOf<AppItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AppItem(
                        label = obj.getString("label"),
                        packageName = obj.getString("pkg"),
                        activityName = obj.getString("activity")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
