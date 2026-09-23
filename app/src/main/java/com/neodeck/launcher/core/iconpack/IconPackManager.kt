package com.neodeck.launcher.core.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.neodeck.launcher.core.model.AppItem
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable? = null,
    val isBuiltin: Boolean = false
)

class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val iconMap = ConcurrentHashMap<String, String>() // "pkg/activity" -> drawableName
    var currentPackPackage: String? = null
        private set
    private var currentPackRes: Resources? = null

    companion object {
        const val PACK_DEFAULT = "builtin:default"
        const val PACK_MATERIAL_YOU = "builtin:material_you"
        const val PACK_CYBER_NEON = "builtin:cyber_neon"
        const val PACK_NORDIC_SLATE = "builtin:nordic_slate"
        const val PACK_SQUIRCLE = "builtin:squircle"

        private val ICON_PACK_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.teslacoilsw.launcher.THEME"
        )
    }

    fun isBuiltinPack(): Boolean {
        val pack = currentPackPackage ?: return true
        return pack.startsWith("builtin:")
    }

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val builtinList = listOf(
            IconPackInfo(PACK_DEFAULT, "Standard (System)", isBuiltin = true),
            IconPackInfo(PACK_MATERIAL_YOU, "Material You Dynamic (Monochrome)", isBuiltin = true),
            IconPackInfo(PACK_CYBER_NEON, "Cyber Neon (Neo Deck)", isBuiltin = true),
            IconPackInfo(PACK_NORDIC_SLATE, "Nordic Slate (Minimalist)", isBuiltin = true),
            IconPackInfo(PACK_SQUIRCLE, "Squircle Modern", isBuiltin = true)
        )

        val packages = mutableSetOf<String>()
        val externalList = mutableListOf<IconPackInfo>()

        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (resolveInfo in activities) {
                val pkg = resolveInfo.activityInfo.packageName
                if (packages.add(pkg)) {
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val icon = resolveInfo.loadIcon(packageManager)
                    externalList.add(IconPackInfo(pkg, label, icon, isBuiltin = false))
                }
            }
        }
        return builtinList + externalList.sortedBy { it.name }
    }

    fun applyIconPack(packageName: String?) {
        iconMap.clear()
        currentPackPackage = packageName

        if (packageName.isNullOrBlank() || packageName == "default" || packageName.startsWith("builtin:")) {
            currentPackRes = null
            return
        }

        try {
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
            val appFilterId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            val parser: XmlPullParser = if (appFilterId != 0) {
                res.getXml(appFilterId)
            } else {
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
        if (packPkg.startsWith("builtin:")) return null

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

    fun transformIcon(baseDrawable: Drawable): Drawable {
        val pack = currentPackPackage ?: return baseDrawable
        if (pack == PACK_DEFAULT || !pack.startsWith("builtin:")) return baseDrawable

        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (pack) {
            PACK_MATERIAL_YOU -> {
                // Material You Dynamic / Monochrome styling
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(255, 34, 43, 54) // Material dynamic dark surface
                }
                val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
                canvas.drawRoundRect(rect, 48f, 48f, bgPaint)

                // Monochrome tint paint
                val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }

                val padding = (size * 0.22f).toInt()
                val innerBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val innerCanvas = Canvas(innerBitmap)
                baseDrawable.setBounds(padding, padding, size - padding, size - padding)
                baseDrawable.draw(innerCanvas)

                // Apply tinted monochrome overlay
                val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(
                        floatArrayOf(
                            0f, 0f, 0f, 0f, 0f,       // Red -> 0
                            0.8f, 0.8f, 0.8f, 0f, 220f, // Green -> Cyan tint
                            0.9f, 0.9f, 0.9f, 0f, 255f, // Blue -> Cyan tint
                            0f, 0f, 0f, 1f, 0f        // Alpha
                        )
                    )
                }
                canvas.drawBitmap(innerBitmap, 0f, 0f, tintPaint)
            }

            PACK_CYBER_NEON -> {
                // Dark background
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(255, 18, 16, 26)
                }
                val rect = RectF(4f, 4f, size.toFloat() - 4f, size.toFloat() - 4f)
                canvas.drawRoundRect(rect, 44f, 44f, bgPaint)

                // Glowing Neon Cyan Stroke
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(255, 0, 229, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                canvas.drawRoundRect(rect, 44f, 44f, strokePaint)

                // Centered icon
                val padding = (size * 0.20f).toInt()
                baseDrawable.setBounds(padding, padding, size - padding, size - padding)
                baseDrawable.draw(canvas)
            }

            PACK_NORDIC_SLATE -> {
                // Slate background
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(255, 42, 48, 56)
                }
                val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
                canvas.drawRoundRect(rect, 46f, 46f, bgPaint)

                // Desaturated icon
                val colorMatrix = ColorMatrix().apply { setSaturation(0.15f) }
                val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }
                val innerBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val innerCanvas = Canvas(innerBitmap)
                val padding = (size * 0.22f).toInt()
                baseDrawable.setBounds(padding, padding, size - padding, size - padding)
                baseDrawable.draw(innerCanvas)

                canvas.drawBitmap(innerBitmap, 0f, 0f, iconPaint)
            }

            PACK_SQUIRCLE -> {
                // Mask base icon inside a smooth squircle
                val padding = (size * 0.08f).toInt()
                val innerBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val innerCanvas = Canvas(innerBitmap)
                baseDrawable.setBounds(padding, padding, size - padding, size - padding)
                baseDrawable.draw(innerCanvas)

                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
                canvas.drawRoundRect(rect, 44f, 44f, maskPaint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                }
                canvas.drawBitmap(innerBitmap, 0f, 0f, compositePaint)
            }

            else -> {
                baseDrawable.setBounds(0, 0, size, size)
                baseDrawable.draw(canvas)
            }
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
