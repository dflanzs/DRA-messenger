package com.example.mobile_app.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mobile_app.network.NetworkConfig
import com.example.mobile_app.presentation.navigation.AppNavigation
import com.example.mobile_app.presentation.screens.ServerConfigScreen
import com.example.mobile_app.ui.theme.MobileappTheme

@Composable
fun MobileApp() {
    MobileappTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // En el emulador se omite la pantalla (usa 10.0.2.2). En dispositivo
            // real se pide la IP en cada arranque, pre-rellenada con la última.
            // Solo tras confirmar una IP que responde se montan los coordinadores
            // de red, que leen la URL ya guardada en NetworkConfig.
            var configured by remember { mutableStateOf(NetworkConfig.isEmulator()) }
            if (configured) {
                AppNavigation()
            } else {
                ServerConfigScreen(onContinue = { configured = true })
            }
        }
    }
}
