package com.example.mobile_app.data.repository

import com.example.mobile_app.data.model.auth.AuthResponseDto
import com.example.mobile_app.data.model.auth.LoginDto
import com.example.mobile_app.data.model.auth.MessageResponseDto
import com.example.mobile_app.data.model.auth.RegisterRequestDto
import com.example.mobile_app.data.model.auth.VerifyEmailDto
import com.example.mobile_app.data.model.auth.VerifyEmailResponseDto
import com.example.mobile_app.data.network.AuthApiService

interface AuthRepository {
    suspend fun register(name: String, email: String, password: String): MessageResponseDto
    suspend fun verifyEmail(token: String): VerifyEmailResponseDto
    suspend fun login(email: String, password: String): AuthResponseDto
    suspend fun logout()
}

class RetrofitAuthRepository(
    private val api: AuthApiService,
) : AuthRepository {
    override suspend fun register(name: String, email: String, password: String): MessageResponseDto {
        return api.register(RegisterRequestDto(name = name, email = email, password = password))
    }

    override suspend fun verifyEmail(token: String): VerifyEmailResponseDto {
        return api.verifyEmail(VerifyEmailDto(token = token))
    }

    override suspend fun login(email: String, password: String): AuthResponseDto {
        return api.login(LoginDto(email = email, password = password))
    }

    override suspend fun logout() {
        api.logout()
    }
}

