package com.example.mobile_app.presentation.signal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.mobile_app.data.network.RetrofitProvider
import com.example.mobile_app.data.repository.RetrofitSignalRepository
import com.example.mobile_app.domain.signal.SignalEngine
import com.example.mobile_app.security.CurrentUserManager

class SignalCoordinator(
    val engine: SignalEngine,
) {
    /** Genera (si hace falta) y publica las claves del usuario al backend. */
    suspend fun bootstrap() = engine.ensureBootstrapped()
}

@Composable
fun rememberSignalCoordinator(currentUserManager: CurrentUserManager): SignalCoordinator {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val signalRepository = RetrofitSignalRepository(
            RetrofitProvider.createSignalApiService(context),
        )
        SignalCoordinator(SignalEngine.get(context, currentUserManager, signalRepository))
    }
}
