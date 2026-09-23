package com.neodeck.launcher.core.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.neodeck.launcher.core.iconpack.IconPackManager
import com.neodeck.launcher.core.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AppRepository(
    private val context: Context,
    val iconPackManager: IconPackManager = IconPackManager(context)
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val iconCache = ConcurrentHashMap<String, Drawable>()

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            reloadApps()
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            reloadApps()
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            reloadApps()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            reloadApps()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            reloadApps()
        }
    }

    init {
        launcherApps.registerCallback(launcherCallback, Handler(Looper.getMainLooper()))
        reloadApps()
    }

    fun reloadApps() {
        scope.launch {
            val appList = mutableListOf<AppItem>()
            val profiles = userManager.userProfiles

            for (profile in profiles) {
                val activities = launcherApps.getActivityList(null, profile)
                for (activityInfo in activities) {
                    val label = activityInfo.label?.toString() ?: activityInfo.applicationInfo.name ?: "Unknown"
                    val pkgName = activityInfo.applicationInfo.packageName
                    val activityName = activityInfo.name

                    appList.add(
                        AppItem(
                            label = label,
                            packageName = pkgName,
                            activityName = activityName,
                            userHandle = profile
                        )
                    )
                }
            }

            // Sort alphabetically by label
            val sortedList = appList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            _apps.value = sortedList
        }
    }

    fun applyIconPack(iconPackPackage: String?) {
        iconPackManager.applyIconPack(iconPackPackage)
        iconCache.clear()
        reloadApps()
    }

    fun getAppIcon(app: AppItem): Drawable? {
        val cacheKey = "${app.packageName}/${app.activityName}/${app.userHandle?.hashCode() ?: 0}/${iconPackManager.currentPackPackage}"
        iconCache[cacheKey]?.let { return it }

        // Check active third-party icon pack first
        if (!iconPackManager.isBuiltinPack()) {
            val iconPackDrawable = iconPackManager.getIconForApp(app)
            if (iconPackDrawable != null) {
                iconCache[cacheKey] = iconPackDrawable
                return iconPackDrawable
            }
        }

        val baseDrawable = try {
            val user = app.userHandle ?: Process.myUserHandle()
            val activities = launcherApps.getActivityList(app.packageName, user)
            val info = activities.find { it.name == app.activityName }
                ?: activities.firstOrNull()
            info?.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                ?: context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        } ?: return null

        val finalDrawable = if (iconPackManager.isBuiltinPack()) {
            iconPackManager.transformIcon(baseDrawable)
        } else {
            baseDrawable
        }

        iconCache[cacheKey] = finalDrawable
        return finalDrawable
    }

    fun launchApp(app: AppItem) {
        try {
            val user = app.userHandle ?: Process.myUserHandle()
            val component = ComponentName(app.packageName, app.activityName)
            launcherApps.startMainActivity(component, user, null, null)
        } catch (e: Exception) {
            // Fallback via standard Intent
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
    }

    fun openAppInfo(app: AppItem) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun uninstallApp(app: AppItem) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
