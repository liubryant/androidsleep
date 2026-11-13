package cn.cjym.timesleep.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.round
import kotlin.math.roundToInt

@Serializable data class WeatherConditions(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("weather_code") val code: Int,
    @SerialName("is_day") val isDay: Int,
) {
    val temperatureText get() = "${temperature.roundToInt()}°"
    val description get() = when(code) {
        0 -> "晴"; 1,2 -> "多云"; 3 -> "阴"; 45,48 -> "雾"
        51,53,55 -> "毛毛雨"; 56,57,66,67 -> "冻雨"; 61,63,80,81 -> "降雨"
        65,82 -> "强降雨"; 71,73,75,77,85,86 -> "降雪"; 95 -> "雷雨"; 96,99 -> "雷雨伴冰雹"; else -> "天气"
    }
}
@Serializable private data class WeatherForecast(val current: WeatherConditions)

/** Foreground coarse location only. Coordinates are rounded to two decimal places before querying. */
class HomeWeather(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("home_weather", Context.MODE_PRIVATE)
    private val manager = this.context.getSystemService(LocationManager::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
    private val _current = MutableStateFlow<WeatherConditions?>(null)
    val current = _current.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun wasAsked() = prefs.getBoolean("asked",false)
    fun markAsked() { prefs.edit().putBoolean("asked",true).apply() }
    suspend fun refresh() {
        if (!hasPermission()) {
            _current.value=null; prefs.edit().remove("conditions").apply(); return
        }
        if (_loading.value) return
        _loading.value=true; _error.value=null
        if (_current.value==null) _current.value=runCatching { json.decodeFromString<WeatherConditions>(prefs.getString("conditions", "")!!) }.getOrNull()?.takeIf { it.temperature.isFinite() }
        try {
            val location=withTimeoutOrNull(15_000) { locate() }
            if(location==null) { _error.value="暂时无法获取位置，请开启手机定位后刷新"; return }
            val conditions=withContext(Dispatchers.IO) {
                val url="https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", (round(location.latitude*100)/100).toString())
                    .addQueryParameter("longitude", (round(location.longitude*100)/100).toString())
                    .addQueryParameter("current","temperature_2m,weather_code,is_day")
                    .addQueryParameter("temperature_unit","celsius").build()
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    check(response.isSuccessful)
                    json.decodeFromString<WeatherForecast>(response.body!!.string()).current.also { check(it.temperature.isFinite()) }
                }
            }
            if(hasPermission()) {
                _current.value=conditions
                prefs.edit().putString("conditions",json.encodeToString(WeatherConditions.serializer(),conditions)).apply()
            }
        } catch(e:CancellationException) { throw e }
        catch(e:Exception) { _error.value="天气暂时无法更新，请稍后重试" }
        finally { _loading.value=false }
    }
    @SuppressLint("MissingPermission")
    private suspend fun locate(): Location? {
        val providers=manager.getProviders(true).filter { it==LocationManager.NETWORK_PROVIDER || it==LocationManager.PASSIVE_PROVIDER }
        val cached=providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis()-it.time in 0..30*60*1000L }.maxByOrNull { it.time }
        if(cached!=null) return cached
        if(!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null
        return suspendCancellableCoroutine { continuation ->
            val listener=object:LocationListener {
                override fun onLocationChanged(location:Location) {
                    manager.removeUpdates(this)
                    if(continuation.isActive) continuation.resume(location)
                }
                override fun onProviderDisabled(provider:String) { manager.removeUpdates(this); if(continuation.isActive) continuation.resume(null) }
                override fun onProviderEnabled(provider:String) = Unit
                @Deprecated("Legacy callback") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?) = Unit
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0L,0f,listener,Looper.getMainLooper())
        }
    }
}
