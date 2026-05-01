package com.example.mobile_app.domain.usecase

import com.example.mobile_app.data.model.auth.AuthResponseDto
import com.example.mobile_app.data.repository.AuthRepository
import com.example.mobile_app.security.TokenManager

class RegisterUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(name: String, email: String, password: String) {
        repository.register(name, email, password)
    }
}

class VerifyEmailUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(token: String): AuthResponseDto {
        return repository.verifyEmail(token)
    }
}

class LoginUseCase(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager,
) {
    suspend operator fun invoke(email: String, password: String): AuthResponseDto {
        val response = repository.login(email, password)
        tokenManager.saveToken(response.token)
        return response
    }
}

class LogoutUseCase(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager,
) {
    suspend operator fun invoke() {
        repository.logout()
        tokenManager.clearToken()
    }
}

