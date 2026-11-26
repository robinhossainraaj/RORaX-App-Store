package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AppItem
import com.example.domain.InstallState
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.AppChipShape
import com.example.ui.theme.AppIconShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.spacing

@Composable
fun AppCard(
    app: AppItem,
    installState: InstallState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Surface(
        shape = AppCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .testTag("app_card_${app.packageName}")
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(app.icon).crossfade(true).build(),
                contentDescription = app.name,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .size(dimensions.avatarCard)
                            .clip(AppIconShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(dimensions.avatarCard)
                            .clip(AppIconShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(dimensions.iconHero)
                        )
                    }
                },
                modifier = Modifier
                    .size(dimensions.avatarCard)
                    .clip(AppIconShape)
            )

            Spacer(Modifier.height(spacing.md))

            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(spacing.xs))

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(spacing.sm))

            if (app.formattedSize.isNotBlank()) {
                Text(
                    text = app.formattedSize,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(spacing.md))
            }

            Spacer(Modifier.height(spacing.md))

            AppStatusChip(state = installState)
        }
    }
}

@Composable
fun AppStatusChip(
    state: InstallState,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions
    val colors = MaterialTheme.colorScheme

    val label = when (state) {
        is InstallState.NotInstalled -> stringResource(R.string.btn_install)
        is InstallState.UpdateAvailable -> stringResource(R.string.btn_update)
        is InstallState.UpToDate -> stringResource(R.string.btn_open)
        is InstallState.Downloading -> if (state.progress >= 0) {
            stringResource(R.string.status_downloading, state.progress)
        } else {
            stringResource(R.string.status_waiting)
        }
        is InstallState.Installing -> stringResource(R.string.status_installing)
    }

    val (container, content) = when (state) {
        is InstallState.NotInstalled, is InstallState.UpdateAvailable ->
            colors.primary to colors.onPrimary
        is InstallState.UpToDate ->
            colors.secondaryContainer to colors.onSecondaryContainer
        is InstallState.Downloading, is InstallState.Installing ->
            colors.surfaceVariant to colors.primary
    }

    Surface(
        shape = AppChipShape,
        color = container,
        contentColor = content,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
        ) {
            if (state is InstallState.Downloading || state is InstallState.Installing) {
                CircularProgressIndicator(
                    strokeWidth = dimensions.strokeThin,
                    color = content,
                    modifier = Modifier.size(dimensions.iconTiny)
                )
                Spacer(Modifier.width(spacing.xs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
