@file:Suppress("unused")

package com.example.mobile_app.presentation.signal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.mobile_app.data.repository.RetrofitSignalRepository
import com.example.mobile_app.data.network.RetrofitProvider
import com.example.mobile_app.domain.signal.SignalStore
import com.example.mobile_app.domain.signal.SignalKeyGenerationService
import com.example.mobile_app.domain.usecase.BootstrapSignalKeysUseCase

class SignalCoordinator(
    val bootstrapSignalKeysUseCase: BootstrapSignalKeysUseCase,
    val signalStore: SignalStore,
)

@Composable
fun rememberSignalCoordinator(): SignalCoordinator {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val signalStore = SignalStore(context)
        val signalRepository = RetrofitSignalRepository(
            RetrofitProvider.createSignalApiService(context),
        )
        val keyGenerationService = SignalKeyGenerationService(signalStore)

        SignalCoordinator(
            bootstrapSignalKeysUseCase = BootstrapSignalKeysUseCase(
                keyGenerationService,
                signalRepository,
            ),
            signalStore = signalStore,
        )
    }
}

