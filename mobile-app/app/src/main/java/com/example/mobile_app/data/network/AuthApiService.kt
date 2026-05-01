package com.example.mobile_app.data.network

import com.example.mobile_app.data.model.auth.AuthResponseDto
import com.example.mobile_app.data.model.auth.LoginDto
import com.example.mobile_app.data.model.auth.MessageResponseDto
import com.example.mobile_app.data.model.auth.RegisterRequestDto
import com.example.mobile_app.data.model.auth.VerifyEmailDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): MessageResponseDto

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailDto): AuthResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginDto): AuthResponseDto

    @POST("api/auth/logout")
    suspend fun logout(): MessageResponseDto
}

