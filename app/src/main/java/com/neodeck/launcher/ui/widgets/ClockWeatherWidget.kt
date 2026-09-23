package com.neodeck.launcher.ui.widgets

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neodeck.launcher.core.weather.WeatherInfo
import com.neodeck.launcher.core.weather.WeatherRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ClockWeatherWidget(
    weatherRepository: WeatherRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }

    // Update time every second
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d. MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    // Fetch weather
    LaunchedEffect(Unit) {
        weatherInfo = weatherRepository.getWeather()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        val isCompactHeight = maxHeight < 110.dp
        val isCompactWidth = maxWidth < 210.dp

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompactHeight) 10.dp else 16.dp)
        ) {
            // Clock & Date (tap opens Clock / Alarms)
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        try {
                            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
            ) {
                Text(
                    text = currentTime.ifEmpty { "12:00" },
                    style = if (isCompactHeight) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    } else {
                        MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isCompactHeight || !isCompactWidth) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDate,
                        style = if (isCompactHeight) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Weather Pill
            weatherInfo?.let { weather ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(
                            horizontal = if (isCompactHeight) 8.dp else 12.dp,
                            vertical = if (isCompactHeight) 6.dp else 8.dp
                        )
                ) {
                    val icon = getWeatherIcon(weather.weatherCode)
                    Icon(
                        imageVector = icon,
                        contentDescription = weather.description,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isCompactHeight) 20.dp else 24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${weather.temperature.roundToInt()}°",
                            style = if (isCompactHeight) {
                                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!isCompactHeight) {
                            Text(
                                text = weather.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0, 1 -> Icons.Default.WbSunny
        2, 3, 45, 48 -> Icons.Default.Cloud
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.WaterDrop
        71, 73, 75 -> Icons.Default.AcUnit
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbSunny
    }
}
