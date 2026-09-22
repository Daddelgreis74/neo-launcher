package com.neodeck.launcher.core.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class LauncherWidgetHost(
    context: Context,
    hostId: Int = HOST_ID
) : AppWidgetHost(context, hostId) {

    companion object {
        const val HOST_ID = 1024
    }

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LauncherAppWidgetHostView(context)
    }
}

class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    // Custom touch handling or styling if needed
}

@Composable
fun AppWidgetContainer(
    appWidgetId: Int,
    widgetHost: LauncherWidgetHost,
    appWidgetManager: AppWidgetManager,
    modifier: Modifier = Modifier
) {
    val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
    if (info == null) {
        Box(modifier = modifier)
        return
    }

    AndroidView(
        factory = { ctx ->
            widgetHost.createView(ctx, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
