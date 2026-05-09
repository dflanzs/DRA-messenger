package com.example.mobile_app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobile_app.presentation.auth.toAuthUserMessage
import com.example.mobile_app.presentation.signal.SignalCoordinator
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    signalCoordinator: SignalCoordinator? = null,
    onLogout: () -> Unit,
) {
    var isBootstrappingKeys by remember { mutableStateOf(false) }
    var bootstrapError by remember { mutableStateOf<String?>(null) }
    var bootstrapSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(signalCoordinator) {
        if (signalCoordinator != null && !bootstrapSuccess) {
            isBootstrappingKeys = true
            bootstrapError = null
            scope.launch {
                runCatching {
                    // Check if bootstrap was already completed
                    if (signalCoordinator.signalStore.isBootstrapCompleted()) {
                        bootstrapSuccess = true
                    } else {
                        signalCoordinator.bootstrapSignalKeysUseCase()
                        bootstrapSuccess = true
                    }
                }.onSuccess {
                    isBootstrappingKeys = false
                }.onFailure { throwable ->
                    bootstrapError = throwable.toAuthUserMessage("No se pudo hacer bootstrap de claves Signal.")
                    isBootstrappingKeys = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isBootstrappingKeys) {
            Text(
                text = "Inicializando claves Signal...",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else if (!bootstrapError.isNullOrBlank()) {
            Text(
                text = "Error en bootstrap: $bootstrapError",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (bootstrapSuccess) {
            Text(
                text = "Claves Signal configuradas correctamente.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(
                text = "La base de la app ya está lista para seguir con autenticación, cifrado y chat.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogout,
            enabled = !isBootstrappingKeys,
        ) {
            Text("Cerrar sesión")
        }
    }
}
