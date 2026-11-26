package com.example.ui.installed

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppItem
import com.example.data.repository.StoreUiState
import com.example.domain.InstallState
import com.example.ui.StoreViewModel
import com.example.ui.components.InstallButton
import com.example.ui.components.shimmerBrush
import com.example.ui.theme.AppButtonShape
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.AppIconShape
import com.example.ui.theme.onSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledScreen(
    viewModel: StoreViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val installStates by viewModel.installStates.collectAsState()

    val apps = (uiState as? StoreUiState.Success)?.apps ?: emptyList()

    // Apps installed on this device
    val updatesAvailableApps = remember(apps, installStates) {
        apps.filter { app ->
            val state = installStates[app.packageName]
            state is InstallState.UpdateAvailable ||
                    (state is InstallState.Downloading && app.versionCode > 0) ||
                    (state is InstallState.Installing && app.versionCode > 0)
        }
    }

    val upToDateApps = remember(apps, installStates) {
        apps.filter { app ->
            val state = installStates[app.packageName]
            state is InstallState.UpToDate
        }
    }

    val totalInstalled = updatesAvailableApps.size + upToDateApps.size

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.installed_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (totalInstalled == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_installed_apps),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceMuted
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToHome,
                        shape = AppButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("browse_store_button")
                    ) {
                        Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.browse_store), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Updates available section
                if (updatesAvailableApps.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.updates_available_section, updatesAvailableApps.size),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { viewModel.updateAll() },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("update_all_button")
                            ) {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.update_all),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    items(updatesAvailableApps, key = { "update_${it.packageName}" }) { app ->
                        val state = installStates[app.packageName] ?: InstallState.NotInstalled
                        InstalledAppRow(
                            app = app,
                            installState = state,
                            isUpdate = true,
                            onClick = { onAppClick(app.packageName) },
                            onInstallClick = { viewModel.handleInstallClick(app, onRequestPermission) },
                            onOpenClick = {
                                viewModel.openApp(app.packageName) {}
                            },
                            onCancelDownload = { viewModel.cancelDownload(app) }
                        )
                    }
                }

                // Up to date section
                if (upToDateApps.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.up_to_date_section, upToDateApps.size),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(upToDateApps, key = { "uptodate_${it.packageName}" }) { app ->
                        val state = installStates[app.packageName] ?: InstallState.UpToDate
                        InstalledAppRow(
                            app = app,
                            installState = state,
                            isUpdate = false,
                            onClick = { onAppClick(app.packageName) },
                            onInstallClick = { viewModel.handleInstallClick(app, onRequestPermission) },
                            onOpenClick = {
                                viewModel.openApp(app.packageName) {}
                            },
                            onCancelDownload = { viewModel.cancelDownload(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstalledAppRow(
    app: AppItem,
    installState: InstallState,
    isUpdate: Boolean,
    onClick: () -> Unit,
    onInstallClick: () -> Unit,
    onOpenClick: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, AppCardShape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(app.icon)
                    .crossfade(true)
                    .build(),
                contentDescription = app.name,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.size(56.dp).background(shimmerBrush()))
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = app.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(AppIconShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (isUpdate) {
                    val installedCode = (installState as? InstallState.UpdateAvailable)?.installedCode
                    Text(
                        text = if (installedCode != null) {
                            stringResource(R.string.installed_tag, "v$installedCode") + "  •  " +
                                    stringResource(R.string.new_tag, app.versionName.ifBlank { "v${app.versionCode}" })
                        } else {
                            stringResource(R.string.new_tag, app.versionName.ifBlank { "v${app.versionCode}" })
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(R.string.installed_tag, app.versionName.ifBlank { "1.0.0" }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action button (UPDATE / OPEN / live progress)
            Box(modifier = Modifier.width(108.dp)) {
                InstallButton(
                    installState = installState,
                    onInstallClick = onInstallClick,
                    onOpenClick = onOpenClick,
                    onCancelDownload = onCancelDownload,
                    modifier = Modifier.height(44.dp)
                )
            }
        }
    }
}
