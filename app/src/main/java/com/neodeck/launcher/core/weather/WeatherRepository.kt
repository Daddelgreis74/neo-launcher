package com.neodeck.launcher.core.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone

data class WeatherInfo(
    val temperature: Double,
    val weatherCode: Int,
    val description: String,
    val locationName: String = "",
    val isDay: Boolean = true
)

class WeatherRepository(private val context: Context) {

    private var cachedWeather: WeatherInfo? = null
    private var lastFetchTime: Long = 0
    private val CACHE_DURATION_MS = 15 * 60 * 1000 // 15 minutes

    suspend fun getWeather(): WeatherInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedWeather != null && (now - lastFetchTime < CACHE_DURATION_MS)) {
            return@withContext cachedWeather
        }

        // Get coordinates (or fallback to defaults based on TimeZone/Germany)
        val (lat, lon) = getCoordinates()

        try {
            val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day&timezone=auto"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val code = current.getInt("weather_code")
                val isDay = current.optInt("is_day", 1) == 1

                val desc = getWeatherDescription(code)
                val info = WeatherInfo(
                    temperature = temp,
                    weatherCode = code,
                    description = desc,
                    isDay = isDay
                )
                cachedWeather = info
                lastFetchTime = now
                return@withContext info
            }
        } catch (_: Exception) {}

        return@withContext cachedWeather
    }

    private fun getCoordinates(): Pair<Double, Double> {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCoarse) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                if (loc != null) {
                    return Pair(loc.latitude, loc.longitude)
                }
            } catch (_: Exception) {}
        }

        // Fallback: Default to Central Europe (Berlin) if location not yet granted
        return Pair(52.52, 13.405)
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Klar"
            1, 2 -> "Leicht bewölkt"
            3 -> "Bedeckt"
            45, 48 -> "Nebel"
            51, 53, 55 -> "Nieselregen"
            61, 63, 65 -> "Regen"
            71, 73, 75 -> "Schneefall"
            80, 81, 82 -> "Regenschauer"
            95, 96, 99 -> "Gewitter"
            else -> "Heiter"
        }
    }
}
