package com.example.mobile_app.network

import android.content.Context
import android.os.Build
import com.example.mobile_app.BuildConfig

/**
 * Resuelve la URL del backend. La IP se guarda en SharedPreferences y se
 * configura al arrancar (ver ServerConfigScreen). El puerto es fijo (8080).
 */
object NetworkConfig {
    private const val PREFS = "network_config"
    private const val KEY_IP = "backend_ip"
    private const val PORT = 8080

    /** Alias del emulador de Android Studio para el localhost del anfitrión. */
    private const val EMULATOR_IP = "10.0.2.2"

    /** Heurística para detectar el emulador de Android Studio. */
    fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

    /** IP por defecto, extraída de BuildConfig (emulador: 10.0.2.2). */
    private val defaultIp: String =
        BuildConfig.BACKEND_BASE_URL
            .substringAfter("://")
            .substringBefore(":")
            .substringBefore("/")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * IP del backend. En el emulador siempre 10.0.2.2; en dispositivo real, la
     * IP guardada o la de por defecto (de local.properties vía BuildConfig).
     */
    fun getIp(context: Context): String {
        if (isEmulator()) return EMULATOR_IP
        return prefs(context).getString(KEY_IP, null)?.takeIf { it.isNotBlank() } ?: defaultIp
    }

    fun setIp(context: Context, ip: String) {
        prefs(context).edit().putString(KEY_IP, ip.trim()).apply()
    }

    fun baseUrlForIp(ip: String): String = "http://${ip.trim()}:$PORT/"

    fun resolveBaseUrl(context: Context? = null): String =
        if (context != null) baseUrlForIp(getIp(context)) else BuildConfig.BACKEND_BASE_URL
}
