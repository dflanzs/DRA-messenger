package com.example.mobile_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mobile_app.presentation.auth.rememberAuthCoordinator
import com.example.mobile_app.presentation.signal.rememberSignalCoordinator
import com.example.mobile_app.presentation.screens.HomeScreen
import com.example.mobile_app.presentation.screens.LoginScreen
import com.example.mobile_app.presentation.screens.RegisterScreen
import com.example.mobile_app.presentation.screens.RegistrationSuccessScreen
import kotlinx.coroutines.launch

private object Routes {
    const val Login = "login"
    const val Register = "register"
    const val RegistrationSuccess = "registration_success"
    const val Home = "home"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val authCoordinator = rememberAuthCoordinator()
    val signalCoordinator = rememberSignalCoordinator()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = if (authCoordinator.tokenManager.hasToken()) Routes.Home else Routes.Login,
    ) {
        composable(Routes.Login) {
            LoginScreen(
                authCoordinator = authCoordinator,
                onLoginSuccess = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Routes.Register)
                },
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                authCoordinator = authCoordinator,
                onRegisterBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = { message ->
                    navController.navigate(Routes.RegistrationSuccess) {
                        popUpTo(Routes.Register) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.RegistrationSuccess) {
            RegistrationSuccessScreen(
                onBackToLogin = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.RegistrationSuccess) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                signalCoordinator = signalCoordinator,
                tokenManager = authCoordinator.tokenManager,
                onLogout = {
                    scope.launch {
                        runCatching {
                            authCoordinator.logoutUseCase()
                        }
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}

