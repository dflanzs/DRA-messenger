@file:Suppress("unused")

package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.signal.SignalBootstrapRequestDto
import com.example.mobile_app.data.model.signal.SignalBootstrapResponseDto
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.network.SignalApiService

interface SignalRepository {
    suspend fun bootstrapKeys(request: SignalBootstrapRequestDto): SignalBootstrapResponseDto
    suspend fun getUserBundle(userId: Long): SignalBundleResponseDto
}

class RetrofitSignalRepository(
    private val signalApiService: SignalApiService,
) : SignalRepository {

    override suspend fun bootstrapKeys(
        request: SignalBootstrapRequestDto,
    ): SignalBootstrapResponseDto {
        return signalApiService.bootstrapKeys(request)
    }

    override suspend fun getUserBundle(userId: Long): SignalBundleResponseDto {
        return signalApiService.getUserBundle(userId)
    }
}

