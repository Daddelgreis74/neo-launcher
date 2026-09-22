package com.neodeck.launcher

import com.neodeck.launcher.core.model.AppGridItem
import com.neodeck.launcher.core.model.AppItem
import com.neodeck.launcher.core.model.FolderGridItem
import com.neodeck.launcher.core.model.LauncherSettings
import com.neodeck.launcher.core.model.ThemeMode
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
    }
}
