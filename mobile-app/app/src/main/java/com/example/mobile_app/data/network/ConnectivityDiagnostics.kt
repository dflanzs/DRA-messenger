package com.example.mobile_app.data.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.Socket
import java.net.InetSocketAddress

/**
 * Utilidad para diagnosticar problemas de conectividad
 */
@Suppress("unused") // Usado para debugging
object ConnectivityDiagnostics {
    private const val TAG = "ConnectivityDiagnostics"

    /**
     * Verificar conectividad básica a un host
     */
    suspend fun checkHostReachability(host: String, port: Int = 80): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(InetAddress.getByName(host), port), 5000)
                socket.close()
                Log.d(TAG, "Host $host:$port es alcanzable")
                true
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo alcanzar $host:$port: ${e.message}")
                false
            }
        }
    }

    /**
     * Verificar que la URL es válida y el formato WebSocket es correcto
     */
    fun validateWebSocketUrl(baseUrl: String): Boolean {
        return try {
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
                .trimEnd('/') + "/ws-chat"

            if (!wsUrl.startsWith("ws://") && !wsUrl.startsWith("wss://")) {
                Log.e(TAG, "URL WebSocket inválida: $wsUrl (debe comenzar con ws:// o wss://)")
                return false
            }

            Log.d(TAG, "URL WebSocket válida: $wsUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error validando URL: ${e.message}")
            false
        }
    }

    /**
     * Test rápido de conexión HTTP (sin WebSocket)
     */
    @Suppress("unused") // Usado para debugging
    suspend fun testHttpConnectivity(baseUrl: String, okHttpClient: OkHttpClient): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                // Para conectividad, cualquier código HTTP indica que el host es alcanzable.
                val isHealthy = response.code in 100..599
                Log.d(TAG, "Health check respondió con código: ${response.code}")
                response.close()
                isHealthy
            } catch (e: Exception) {
                Log.e(TAG, "HTTP connectivity test falló: ${e.message}")
                false
            }
        }
    }
}


