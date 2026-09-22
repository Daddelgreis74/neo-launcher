package com.neodeck.launcher

import com.neodeck.launcher.core.model.AppGridItem
import com.neodeck.launcher.core.model.AppItem
import com.neodeck.launcher.core.model.FolderGridItem
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.ThemeMode
import com.neodeck.launcher.core.model.WidgetGridItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherModelTest {

    @Test
    fun testAppItemCreation() {
        val app = AppItem(
            label = "Neo Dashboard",
            packageName = "com.neodeck.dashboard",
            activityName = "com.neodeck.dashboard.MainActivity"
        )
        assertEquals("Neo Dashboard", app.label)
        assertEquals("com.neodeck.dashboard", app.packageName)
    }

    @Test
    fun testGridItemCoordinates() {
        val app = AppItem("Test", "com.test", "com.test.Main")
        val gridItem = AppGridItem(
            id = "1",
            page = 0,
            row = 2,
            col = 3,
            app = app
        )
        assertEquals(0, gridItem.page)
        assertEquals(2, gridItem.row)
        assertEquals(3, gridItem.col)
    }

    @Test
    fun testWidgetGridItem() {
        val widget = WidgetGridItem(
            id = "w1",
            page = 0,
            row = 1,
            col = 0,
            appWidgetId = 42,
            spanX = 3,
            spanY = 2
        )
        assertEquals(42, widget.appWidgetId)
        assertEquals(3, widget.spanX)
        assertEquals(2, widget.spanY)
    }

    @Test
    fun testFolderContainsApps() {
        val app1 = AppItem("App 1", "com.app1", "com.app1.Main")
        val app2 = AppItem("App 2", "com.app2", "com.app2.Main")
        val folder = FolderGridItem(
            id = "f1",
            page = 0,
            row = 1,
            col = 1,
            title = "Tools",
            apps = listOf(app1, app2)
        )
        assertEquals(2, folder.apps.size)
        assertEquals("Tools", folder.title)
    }

    @Test
    fun testDefaultSettings() {
        val settings = LauncherSettings()
        assertEquals(5, settings.gridRows)
        assertEquals(5, settings.gridCols)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.showAppLabels)
        assertTrue(settings.smartHomeEnabled)
        assertEquals("aurora", settings.selectedWallpaper)
        assertTrue(settings.hapticFeedbackEnabled)
    }
}
