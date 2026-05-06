@file:Suppress("unused")

package com.example.mobile_app.domain.usecase

import com.example.mobile_app.data.model.signal.SignalBootstrapResponseDto
import com.example.mobile_app.data.repository.SignalRepository
import com.example.mobile_app.domain.signal.SignalKeyGenerationService

class BootstrapSignalKeysUseCase(
    private val keyGenerationService: SignalKeyGenerationService,
    private val signalRepository: SignalRepository,
) {
    suspend operator fun invoke(): SignalBootstrapResponseDto {
        val bootstrapRequest = keyGenerationService.generateAndBootstrap()
        val response = signalRepository.bootstrapKeys(bootstrapRequest)
        keyGenerationService.persistBootstrapResult(response)
        return response
    }
}

