@file:Suppress("unused")

package com.example.mobile_app.data.network

import com.example.mobile_app.data.model.signal.SignalBootstrapRequestDto
import com.example.mobile_app.data.model.signal.SignalBootstrapResponseDto
import com.example.mobile_app.data.model.signal.SignalBundleResponseDto
import com.example.mobile_app.data.model.signal.SignalRefillRequestDto
import com.example.mobile_app.data.model.signal.SignalRefillResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SignalApiService {

    @POST("api/signal/keys/bootstrap")
    suspend fun bootstrapKeys(
        @Body request: SignalBootstrapRequestDto,
    ): SignalBootstrapResponseDto

    @POST("api/signal/keys/one-time/refill")
    suspend fun refillOneTimePreKeys(
        @Body request: SignalRefillRequestDto,
    ): SignalRefillResponseDto

    @GET("api/signal/users/{userId}/bundle")
    suspend fun getUserBundle(
        @Path("userId") userId: Long,
    ): SignalBundleResponseDto
}

