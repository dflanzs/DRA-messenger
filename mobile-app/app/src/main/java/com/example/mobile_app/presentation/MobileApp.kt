package com.example.mobile_app.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mobile_app.presentation.navigation.AppNavigation
import com.example.mobile_app.ui.theme.MobileappTheme

@Composable
fun MobileApp() {
    MobileappTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation()
        }
    }
}

