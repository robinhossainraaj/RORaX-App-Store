package com.example.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.StoreInfo
import com.example.data.repository.StoreUiState
import com.example.ui.StoreViewModel
import com.example.ui.details.ContactChip
import com.example.ui.details.openUrl
import com.example.ui.details.sendEmail
import com.example.ui.theme.AppButtonShape
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.onSurfaceMuted
import com.example.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StoreViewModel,
    onNavigateToRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    val storeInfo = (uiState as? StoreUiState.Success)?.store ?: StoreInfo()

    val appVersion = rememberAppVersion(context)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.lg,
                    end = spacing.lg,
                    top = spacing.sm,
                    bottom = spacing.lg
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            SettingsGroupCard(
                title = stringResource(R.string.dev_info_title),
                icon = Icons.Rounded.Person
            ) {
                Text(
                    text = storeInfo.developerName.ifBlank { "RORAX" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (storeInfo.developerBio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = storeInfo.developerBio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceMuted
                    )
                }

                Spacer(modifier = Modifier.height(spacing.md))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (storeInfo.telegram.isNotBlank()) {
                        ContactChip(label = "Telegram", iconRes = R.drawable.ic_logo) {
                            openUrl(context, storeInfo.telegram)
                        }
                    }
                    if (storeInfo.email.isNotBlank()) {
                        ContactChip(label = "Email", imageVector = Icons.Rounded.Email) {
                            sendEmail(context, storeInfo.email)
                        }
                    }
                    if (storeInfo.facebook.isNotBlank()) {
                        ContactChip(label = "Facebook", imageVector = Icons.Rounded.Share) {
                            openUrl(context, storeInfo.facebook)
                        }
                    }
                }
            }

            SettingsGroupCard(
                title = stringResource(R.string.theme_title),
                icon = Icons.Rounded.DarkMode
            ) {
                ThemeOptionRow(
                    title = stringResource(R.string.theme_dark),
                    isSelected = themeMode == "dark",
                    onSelect = { viewModel.setTheme("dark") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ThemeOptionRow(
                    title = stringResource(R.string.theme_light),
                    isSelected = themeMode == "light",
                    onSelect = { viewModel.setTheme("light") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ThemeOptionRow(
                    title = stringResource(R.string.theme_system),
                    isSelected = themeMode == "system",
                    onSelect = { viewModel.setTheme("system") }
                )
            }

            SettingsGroupCard(
                title = stringResource(R.string.notifications_toggle_title),
                icon = Icons.Rounded.Notifications
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notifications_toggle_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(dimensions.hairline))
                        Text(
                            text = stringResource(R.string.notifications_toggle_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceMuted
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("notifications_switch")
                    )
                }
            }

            SettingsGroupCard(
                title = stringResource(R.string.request_apk_card_title),
                icon = Icons.Rounded.Build
            ) {
                Text(
                    text = stringResource(R.string.request_apk_card_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceMuted
                )
                Spacer(modifier = Modifier.height(spacing.md))
                Button(
                    onClick = onNavigateToRequest,
                    shape = AppButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_apk_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_request_apk),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.iconButton)
                    )
                }
            }

            SettingsGroupCard(
                title = stringResource(R.string.about_card_title),
                icon = Icons.Rounded.Info
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(dimensions.iconBrand)
                    )
                    Spacer(modifier = Modifier.width(spacing.lg))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(dimensions.hairline))
                        Text(
                            text = stringResource(R.string.about_version, appVersion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceMuted
                        )
                        Text(
                            text = stringResource(R.string.about_package, "com.rorax.appstore"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline), AppCardShape)
            .padding(spacing.lg)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = spacing.md)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensions.iconAction)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            content()
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .minimumInteractiveComponentSize()
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimensions.iconLarge)
        )
    }
}

@Composable
fun rememberAppVersion(context: android.content.Context): String {
    return try {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        pInfo.versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}
