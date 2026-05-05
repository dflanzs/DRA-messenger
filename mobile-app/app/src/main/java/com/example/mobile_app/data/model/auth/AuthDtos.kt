package com.example.mobile_app.data.model.auth

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

data class VerifyEmailDto(
    val token: String,
)

data class LoginDto(
    val email: String,
    val password: String,
)

data class AuthResponseDto(
    val token: String,
    val user: UserResponseDto,
)

data class VerifyEmailResponseDto(
    val message: String,
    val user: UserResponseDto,
)

data class UserResponseDto(
    val id: Long,
    val name: String,
    val email: String,
    val publicKey: String,
    val onlineStatus: Boolean,
    val role: String,
    val createdAt: String,
)

data class MessageResponseDto(
    val message: String,
)

