@file:Suppress("unused")

package com.example.mobile_app.domain.usecase

import com.example.mobile_app.data.model.signal.SignalBootstrapRequestDto
import com.example.mobile_app.data.model.signal.SignalBootstrapResponseDto
import com.example.mobile_app.data.repository.SignalRepository
import com.example.mobile_app.domain.signal.SignalKeyGenerationService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException
import android.util.Log

class BootstrapSignalKeysUseCase(
    private val keyGenerationService: SignalKeyGenerationService,
    private val signalRepository: SignalRepository,
) {
    suspend operator fun invoke(): SignalBootstrapResponseDto {
        val bootstrapRequest: SignalBootstrapRequestDto = keyGenerationService.generateAndBootstrap()

        try {
            // log serialized request size for debugging
            runCatching {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val json = moshi.adapter(SignalBootstrapRequestDto::class.java).toJson(bootstrapRequest)
                Log.i("BootstrapSignal", "Sending bootstrap request, size=${json.length}")
            }

            val response = signalRepository.bootstrapKeys(bootstrapRequest)
            keyGenerationService.persistBootstrapResult(response)
            return response
        } catch (e: Throwable) {
            // If it's an HTTP error include request JSON + response body to help debugging
            if (e is HttpException) {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val json = runCatching {
                    moshi.adapter(SignalBootstrapRequestDto::class.java).toJson(bootstrapRequest)
                }.getOrNull()
                val responseBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                val code = e.code()
                throw RuntimeException(
                    "Bootstrap failed (HTTP $code): ${responseBody ?: e.message()}\nRequestJson: ${json ?: "<failed to serialize>"}",
                    e,
                )
            }
            throw e
        }
    }
}

