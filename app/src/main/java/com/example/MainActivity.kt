package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.example.ui.StoreViewModel
import com.example.ui.navigation.AppNavHost
import com.example.ui.navigation.Screen
import com.example.ui.theme.RoraxTheme
import com.example.ui.theme.onSurfaceMuted

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAV_PACKAGE = "extra_nav_package"
        const val EXTRA_NAV_LINK = "extra_nav_link"
    }

    private val viewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val navController = rememberNavController()

            // Observe ON_RESUME to re-resolve install states and sync notifications
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.recheckInstalled()
                        (application as? RoraxApp)?.container?.syncScheduledNotifications()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }

            // Notification permission request for Android 13+
            var showNotificationPermissionDialog by remember { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Proceed silently whether granted or denied
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!isGranted) {
                        showNotificationPermissionDialog = true
                    }
                }
            }

            // Unknown apps install permission launcher
            val installPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.recheckInstalled()
            }

            // Notification permission explanation dialog
            if (showNotificationPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationPermissionDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.notification_permission_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.notification_permission_desc),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showNotificationPermissionDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.notification_permission_allow),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationPermissionDialog = false }) {
                            Text(
                                text = stringResource(R.string.notification_permission_not_now),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceMuted
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
            }

            // Handle deep link / notification target navigation
            LaunchedEffect(intent) {
                handleIntentNavigation(intent) { pkg ->
                    navController.navigate(Screen.Details.createRoute(pkg))
                }
            }

            RoraxTheme(themeMode = themeMode) {
                AppNavHost(
                    navController = navController,
                    viewModel = viewModel,
                    onRequestInstallPermission = {
                        val container = (application as RoraxApp).container
                        installPermissionLauncher.launch(container.apkInstaller.getManageUnknownAppSourcesIntent())
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntentNavigation(intent: Intent?, onNavigate: (String) -> Unit) {
        val targetPackage = intent?.getStringExtra(EXTRA_NAV_PACKAGE)
        if (!targetPackage.isNullOrBlank()) {
            onNavigate(targetPackage)
            intent.removeExtra(EXTRA_NAV_PACKAGE)
        }

        val targetLink = intent?.getStringExtra(EXTRA_NAV_LINK)
        if (!targetLink.isNullOrBlank()) {
            intent.removeExtra(EXTRA_NAV_LINK)
            try {
                val uri = android.net.Uri.parse(targetLink)
                if (uri.scheme == "http" || uri.scheme == "https") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } catch (_: Exception) {}
        }
    }
}
