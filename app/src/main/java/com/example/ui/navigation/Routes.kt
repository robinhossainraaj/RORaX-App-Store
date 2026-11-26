package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Installed : Screen("installed")
    data object Settings : Screen("settings")
    data object Details : Screen("details/{package}") {
        fun createRoute(packageName: String): String = "details/$packageName"
    }
    data object Request : Screen("request")
    data object Notifications : Screen("notifications")
}
