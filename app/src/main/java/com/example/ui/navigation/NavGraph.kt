package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.StoreViewModel
import com.example.ui.details.DetailsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.installed.InstalledScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.request.RequestScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.AppMotion
import com.example.ui.theme.dimensions
import com.example.ui.theme.onSurfaceMuted
import com.example.ui.theme.spacing

data class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
    val testTag: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, R.string.nav_home, Icons.Rounded.Home, "nav_item_home"),
    BottomNavItem(Screen.Installed.route, R.string.nav_installed, Icons.Rounded.CheckCircle, "nav_item_installed"),
    BottomNavItem(Screen.Settings.route, R.string.nav_settings, Icons.Rounded.Settings, "nav_item_settings")
)

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: StoreViewModel,
    onRequestInstallPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Installed.route,
        Screen.Settings.route
    )

    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions
    val slideDistancePx = with(LocalDensity.current) {
        dimensions.navSlideDistance.toPx().roundToInt()
    }

    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.install_permission_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.install_permission_message),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInstallPermissionDialog = false
                        onRequestInstallPermission()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.grant_permission),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceMuted
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(AppMotion.Short4)
                ) + fadeIn(animationSpec = tween(AppMotion.Short4)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(AppMotion.Short4)
                ) + fadeOut(animationSpec = tween(AppMotion.Short4))
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = spacing.sm
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.titleRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceMuted,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceMuted
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            enterTransition = {
                fadeIn(animationSpec = tween(AppMotion.Short4)) +
                    slideInHorizontally(
                        initialOffsetX = { slideDistancePx },
                        animationSpec = tween(AppMotion.Short4)
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(AppMotion.Short4)) +
                    slideOutHorizontally(
                        targetOffsetX = { -slideDistancePx },
                        animationSpec = tween(AppMotion.Short4)
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(AppMotion.Short4)) +
                    slideInHorizontally(
                        initialOffsetX = { -slideDistancePx },
                        animationSpec = tween(AppMotion.Short4)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(AppMotion.Short4)) +
                    slideOutHorizontally(
                        targetOffsetX = { slideDistancePx },
                        animationSpec = tween(AppMotion.Short4)
                    )
            }
        ) {
            // 1. Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Home Screen
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onAppClick = { pkg ->
                        navController.navigate(Screen.Details.createRoute(pkg))
                    },
                    onNavigateToInstalled = {
                        navController.navigate(Screen.Installed.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            // 3. Installed Screen
            composable(Screen.Installed.route) {
                InstalledScreen(
                    viewModel = viewModel,
                    onAppClick = { pkg ->
                        navController.navigate(Screen.Details.createRoute(pkg))
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRequestPermission = {
                        showInstallPermissionDialog = true
                    }
                )
            }

            // 4. Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToRequest = {
                        navController.navigate(Screen.Request.route)
                    }
                )
            }

            // 5. Details Screen
            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("package") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("package") ?: ""
                DetailsScreen(
                    packageName = packageName,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onRequestPermission = {
                        showInstallPermissionDialog = true
                    }
                )
            }

            // 6. Request Screen
            composable(Screen.Request.route) {
                RequestScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. Notifications Screen
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAppClick = { pkg ->
                        navController.navigate(Screen.Details.createRoute(pkg))
                    }
                )
            }
        }
    }
}
