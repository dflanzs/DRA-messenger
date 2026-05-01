package com.example.mobile_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mobile_app.presentation.screens.HomeScreen
import com.example.mobile_app.presentation.screens.LoginScreen
import com.example.mobile_app.presentation.screens.RegisterScreen

private object Routes {
    const val Login = "login"
    const val Register = "register"
    const val Home = "home"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.Login,
    ) {
        composable(Routes.Login) {
            LoginScreen(
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
                onRegisterBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                },
            )
        }
    }
}

