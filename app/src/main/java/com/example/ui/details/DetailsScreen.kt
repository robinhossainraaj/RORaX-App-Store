package com.example.ui.details

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.StoreInfo
import com.example.data.repository.StoreUiState
import com.example.domain.InstallState
import com.example.ui.StoreViewModel
import com.example.ui.components.InstallButton
import com.example.ui.components.ScreenshotRow
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.AppChipShape
import com.example.ui.theme.AppDetailIconShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.onSurfaceMuted
import com.example.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    packageName: String,
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val installStates by viewModel.installStates.collectAsState()
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    val currentSuccess = uiState as? StoreUiState.Success
    val app = currentSuccess?.apps?.firstOrNull { it.packageName == packageName }
    val storeInfo = currentSuccess?.store ?: StoreInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (app == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_not_found),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(spacing.lg))
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            return@Scaffold
        }

        val installState = installStates[app.packageName] ?: InstallState.NotInstalled
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(bottom = spacing.xxxl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg),
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
                        Box(
                            modifier = Modifier
                                .size(dimensions.avatarDetail)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .size(dimensions.avatarDetail)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = app.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(dimensions.avatarList)
                            )
                        }
                    },
                    modifier = Modifier
                        .size(dimensions.avatarDetail)
                        .clip(AppDetailIconShape)
                )

                Spacer(modifier = Modifier.width(spacing.lg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    val devName = storeInfo.developerName.ifBlank { "RORAX" }
                    Text(
                        text = devName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(dimensions.hairline))

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
                    .clip(AppCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline), AppCardShape)
                    .padding(vertical = spacing.md)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoColumn(
                    label = stringResource(R.string.version_label),
                    value = app.versionName.ifBlank { "1.0.0" },
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = spacing.xs)
                )
                InfoColumn(
                    label = stringResource(R.string.size_label),
                    value = app.formattedSize.ifBlank { "—" },
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = spacing.xs)
                )
                InfoColumn(
                    label = stringResource(R.string.updated_label),
                    value = app.formattedUpdatedAt.ifBlank { "Recent" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
            ) {
                InstallButton(
                    installState = installState,
                    onInstallClick = {
                        viewModel.handleInstallClick(app, onRequestPermission)
                    },
                    onOpenClick = {
                        viewModel.openApp(app.packageName) {
                            Toast.makeText(context, context.getString(R.string.cannot_open_app), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancelDownload = {
                        viewModel.cancelDownload(app)
                    }
                )

                if (installState is InstallState.UpToDate) {
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Text(
                        text = stringResource(R.string.installed_version, app.versionName.ifBlank { "1.0.0" }),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceMuted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (installState is InstallState.UpdateAvailable) {
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Text(
                        text = stringResource(
                            R.string.update_available_version,
                            "v${installState.installedCode}",
                            app.versionName.ifBlank { "v${app.versionCode}" }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xxl))

            if (app.screenshots.isNotEmpty()) {
                ScreenshotRow(screenshots = app.screenshots)
                Spacer(modifier = Modifier.height(spacing.xxl))
            }

            if (app.description.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg)
                ) {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    var isExpanded by remember { mutableStateOf(false) }

                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize()
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = if (isExpanded) stringResource(R.string.read_less) else stringResource(R.string.read_more),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .minimumInteractiveComponentSize()
                            .padding(vertical = spacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(spacing.xxl))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
            ) {
                Text(
                    text = stringResource(R.string.app_info_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppCardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline), AppCardShape)
                        .padding(spacing.lg)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        InfoRow(label = stringResource(R.string.info_package), value = app.packageName)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoRow(label = stringResource(R.string.info_version_name), value = app.versionName.ifBlank { "1.0.0" })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoRow(label = stringResource(R.string.info_version_code), value = app.versionCode.toString())
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoRow(label = stringResource(R.string.info_size), value = app.formattedSize.ifBlank { "—" })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoRow(label = stringResource(R.string.info_last_updated), value = app.formattedUpdatedAt.ifBlank { "Recent" })
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.xxl))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
            ) {
                Text(
                    text = stringResource(R.string.developer_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppCardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline), AppCardShape)
                        .padding(spacing.lg)
                ) {
                    Column {
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
                }
            }
        }
    }
}

@Composable
fun InfoColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceMuted
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ContactChip(
    label: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Box(
        modifier = Modifier
            .clip(AppChipShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = spacing.lg, vertical = spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(dimensions.iconMedium)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
            } else if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(dimensions.iconMedium)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_to_handle), Toast.LENGTH_SHORT).show()
    }
}

fun sendEmail(context: android.content.Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_to_handle), Toast.LENGTH_SHORT).show()
    }
}
