package com.example.mobile_app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mobile_app.data.network.ConnectivityDiagnostics
import com.example.mobile_app.network.NetworkConfig
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * Pantalla de configuración del servidor. Pide la IP del backend, prueba la
 * conexión y solo continúa si responde. Se muestra en cada arranque en
 * dispositivos reales (en el emulador se omite, ver MobileApp / NetworkConfig).
 */
@Composable
fun ServerConfigScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var ip by remember { mutableStateOf(NetworkConfig.getIp(context)) }
    var testing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Configuración del servidor",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Introduce la IP del ordenador donde corre el backend. El puerto es 8080.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = ip,
            onValueChange = {
                ip = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IP del servidor") },
            singleLine = true,
            isError = error != null,
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val trimmed = ip.trim()
                if (trimmed.isBlank()) {
                    error = "Introduce una IP"
                    return@Button
                }
                testing = true
                error = null
                scope.launch {
                    val baseUrl = NetworkConfig.baseUrlForIp(trimmed)
                    val reachable = runCatching {
                        ConnectivityDiagnostics.testHttpConnectivity(baseUrl, OkHttpClient())
                    }.getOrDefault(false)
                    testing = false
                    if (reachable) {
                        NetworkConfig.setIp(context, trimmed)
                        onContinue()
                    } else {
                        error = "No se pudo conectar a $baseUrl. Revisa la IP y la red."
                    }
                }
            },
            enabled = !testing,
        ) {
            Text(if (testing) "Probando conexión..." else "Continuar")
        }
        if (testing) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
