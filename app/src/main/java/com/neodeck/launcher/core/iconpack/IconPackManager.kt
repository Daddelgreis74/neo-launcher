package com.neodeck.launcher.core.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.neodeck.launcher.core.model.AppItem
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable? = null
)

class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val iconMap = ConcurrentHashMap<String, String>() // "pkg/activity" -> drawableName
    private var currentPackPackage: String? = null
    private var currentPackRes: Resources? = null

    companion object {
        private val ICON_PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.teslacoilsw.launcher.THEME"
        )
    }

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val packages = mutableSetOf<String>()
        val result = mutableListOf<IconPackInfo>()

        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (resolveInfo in activities) {
                val pkg = resolveInfo.activityInfo.packageName
                if (packages.add(pkg)) {
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val icon = resolveInfo.loadIcon(packageManager)
                    result.add(IconPackInfo(pkg, label, icon))
                }
            }
        }
        return result.sortedBy { it.name }
    }

    fun applyIconPack(packageName: String?) {
        iconMap.clear()
        if (packageName.isNullOrBlank() || packageName == "default") {
            currentPackPackage = null
            currentPackRes = null
            return
        }

        try {
            currentPackPackage = packageName
            currentPackRes = packageManager.getResourcesForApplication(packageName)
            loadAppFilter(packageName)
        } catch (_: Exception) {
            currentPackPackage = null
            currentPackRes = null
        }
    }

    private fun loadAppFilter(iconPackPackage: String) {
        val res = currentPackRes ?: return
        try {
            // First try res/xml/appfilter.xml
            val appFilterId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            val parser: XmlPullParser = if (appFilterId != 0) {
                res.getXml(appFilterId)
            } else {
                // Try from assets/appfilter.xml
                val assets = res.assets
                val xmlPullParserFactory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                val pullParser = xmlPullParserFactory.newPullParser()
                pullParser.setInput(assets.open("appfilter.xml"), "UTF-8")
                pullParser
            }

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")

                    if (!component.isNullOrEmpty() && !drawable.isNullOrEmpty()) {
                        // ComponentInfo{com.example.pkg/com.example.pkg.MainActivity}
                        val clean = component
                            .removePrefix("ComponentInfo{")
                            .removeSuffix("}")
                        iconMap[clean] = drawable
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
    }

    fun getIconForApp(app: AppItem): Drawable? {
        val packPkg = currentPackPackage ?: return null
        val res = currentPackRes ?: return null

        val key = "${app.packageName}/${app.activityName}"
        val drawableName = iconMap[key] ?: iconMap[app.packageName] ?: return null

        return try {
            val drawableId = res.getIdentifier(drawableName, "drawable", packPkg)
            if (drawableId != 0) {
                ResourcesCompat.getDrawable(res, drawableId, null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
