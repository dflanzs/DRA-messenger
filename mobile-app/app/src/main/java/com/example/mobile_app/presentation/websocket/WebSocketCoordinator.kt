package com.example.mobile_app.presentation.websocket

import android.util.Log
import com.example.mobile_app.data.network.RetrofitProvider
import com.example.mobile_app.data.repository.websocket.WebSocketRepository
import com.example.mobile_app.domain.usecase.websocket.WebSocketUseCases
import com.example.mobile_app.security.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Coordinador de inyección de dependencias para WebSocket
 */
object WebSocketCoordinator {
    private var webSocketUseCases: WebSocketUseCases? = null
    private val TAG = "WebSocketCoordinator"

    /**
     * Obtener instancia de WebSocketUseCases
     * Inicializa las dependencias si es necesario
     */
    fun getWebSocketUseCases(
        tokenManager: TokenManager,
        baseUrl: String
    ): WebSocketUseCases {
        if (webSocketUseCases == null) {
            try {
                val token = tokenManager.getToken()
                if (token == null) {
                    Log.e(TAG, "Token no disponible")
                    throw Exception("Token no disponible para WebSocket")
                }
                
                Log.d(TAG, "Token obtenido, longitud: ${token.length}")
                
                val okHttpClient = RetrofitProvider.getOkHttpClient(tokenManager)
                val moshi = RetrofitProvider.getMoshi()

                val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

                val webSocketRepository = WebSocketRepository(
                    baseUrl = baseUrl,
                    token = token,
                    okHttpClient = okHttpClient,
                    moshi = moshi,
                    scope = scope
                )

                webSocketUseCases = WebSocketUseCases(webSocketRepository)
                Log.d(TAG, "WebSocketUseCases inicializado con baseUrl: $baseUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando WebSocketUseCases: ${e.message}", e)
                throw e
            }
        }
        return webSocketUseCases!!
    }

    /**
     * Limpiar recursos
     */
    fun cleanup() {
        webSocketUseCases = null
        Log.d(TAG, "WebSocketCoordinator limpiado")
    }
}


