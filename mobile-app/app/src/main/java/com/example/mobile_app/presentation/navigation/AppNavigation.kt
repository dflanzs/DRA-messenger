package com.example.mobile_app.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mobile_app.presentation.auth.rememberAuthCoordinator
import com.example.mobile_app.presentation.chat.rememberChatCoordinator
import com.example.mobile_app.presentation.signal.rememberSignalCoordinator
import com.example.mobile_app.presentation.screens.ChatDetailScreen
import com.example.mobile_app.presentation.screens.ChatMembersScreen
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
    const val ChatDetail = "chat/{chatKey}"
    const val ChatMembers = "chat-members/{chatKey}"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val authCoordinator = rememberAuthCoordinator()
    val signalCoordinator = rememberSignalCoordinator()
    val chatCoordinator = rememberChatCoordinator(authCoordinator.tokenManager, authCoordinator.currentUserManager)
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
                onRegisterSuccess = { _ ->
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
                authCoordinator = authCoordinator,
                chatCoordinator = chatCoordinator,
                navController = navController,
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
        composable(Routes.ChatDetail) { backStackEntry ->
            val chatKey = Uri.decode(backStackEntry.arguments?.getString("chatKey").orEmpty())
            ChatDetailScreen(
                chatKey = chatKey,
                navController = navController,
                chatCoordinator = chatCoordinator,
            )
        }
        composable(Routes.ChatMembers) { backStackEntry ->
            val chatKey = Uri.decode(backStackEntry.arguments?.getString("chatKey").orEmpty())
            ChatMembersScreen(
                chatKey = chatKey,
                navController = navController,
                chatCoordinator = chatCoordinator,
            )
        }
    }
}

