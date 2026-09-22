package com.neodeck.launcher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.neodeck.launcher.R

@Composable
fun WallpaperBackground(
    selectedWallpaper: String,
    modifier: Modifier = Modifier
) {
    val resId = when (selectedWallpaper) {
        "aurora" -> R.drawable.wallpaper_aurora_dark
        "nordic" -> R.drawable.wallpaper_nordic_slate
        "cyber" -> R.drawable.wallpaper_cyber_neon
        "sunset" -> R.drawable.wallpaper_sunset_minimal
        else -> null
    }

    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize()
        )
    } else {
        // System Wallpaper / Transparent
        Box(modifier = modifier.fillMaxSize())
    }
}
