package com.example.mobile_app.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.mobile_app.data.network.RetrofitProvider
import com.example.mobile_app.data.repository.RetrofitAuthRepository
import com.example.mobile_app.domain.usecase.LoginUseCase
import com.example.mobile_app.domain.usecase.LogoutUseCase
import com.example.mobile_app.domain.usecase.RegisterUseCase
import com.example.mobile_app.domain.usecase.VerifyEmailUseCase
import com.example.mobile_app.security.CurrentUserManager
import com.example.mobile_app.security.TokenManager

class AuthCoordinator(
    val tokenManager: TokenManager,
    val currentUserManager: CurrentUserManager,
    val registerUseCase: RegisterUseCase,
    val verifyEmailUseCase: VerifyEmailUseCase,
    val loginUseCase: LoginUseCase,
    val logoutUseCase: LogoutUseCase,
)

@Composable
fun rememberAuthCoordinator(): AuthCoordinator {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        val tokenManager = TokenManager(context)
        val currentUserManager = CurrentUserManager(context)
        val repository = RetrofitAuthRepository(
            RetrofitProvider.createAuthApiService(context),
        )

        AuthCoordinator(
            tokenManager = tokenManager,
            currentUserManager = currentUserManager,
            registerUseCase = RegisterUseCase(repository),
            verifyEmailUseCase = VerifyEmailUseCase(repository),
            loginUseCase = LoginUseCase(repository, tokenManager, currentUserManager),
            logoutUseCase = LogoutUseCase(repository, tokenManager, currentUserManager),
        )
    }
}

