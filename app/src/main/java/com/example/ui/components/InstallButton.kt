package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.domain.InstallState
import com.example.ui.theme.AppButtonShape
import com.example.ui.theme.AppMotion
import com.example.ui.theme.dimensions
import com.example.ui.theme.spacing

@Composable
fun InstallButton(
    installState: InstallState,
    onInstallClick: () -> Unit,
    onOpenClick: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.buttonHeight)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AppMotion.Medium2,
                    easing = AppMotion.EmphasizedEasing
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when (installState) {
            is InstallState.NotInstalled -> {
                Button(
                    onClick = onInstallClick,
                    shape = AppButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .testTag("install_button")
                ) {
                    Text(stringResource(R.string.btn_install), style = MaterialTheme.typography.labelLarge)
                }
            }

            is InstallState.UpdateAvailable -> {
                Button(
                    onClick = onInstallClick,
                    shape = AppButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .testTag("update_button")
                ) {
                    Text(stringResource(R.string.btn_update), style = MaterialTheme.typography.labelLarge)
                }
            }

            is InstallState.UpToDate -> {
                OutlinedButton(
                    onClick = onOpenClick,
                    shape = AppButtonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .testTag("open_button")
                ) {
                    Text(stringResource(R.string.btn_open), style = MaterialTheme.typography.labelLarge)
                }
            }

            is InstallState.Downloading -> {
                Surface(
                    shape = AppButtonShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (installState.progress >= 0) {
                                    stringResource(R.string.status_downloading, installState.progress)
                                } else {
                                    stringResource(R.string.status_waiting)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(spacing.xs))
                            if (installState.progress >= 0) {
                                LinearProgressIndicator(
                                    progress = { installState.progress / 100f },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface,
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dimensions.progressHeight)
                                        .clip(AppButtonShape)
                                )
                            } else {
                                LinearProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface,
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dimensions.progressHeight)
                                        .clip(AppButtonShape)
                                )
                            }
                        }

                        Spacer(Modifier.width(spacing.sm))

                        TextButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.testTag("cancel_download_button")
                        ) {
                            Text(
                                stringResource(R.string.btn_cancel),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            is InstallState.Installing -> {
                Surface(
                    shape = AppButtonShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = dimensions.strokeMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(dimensions.iconButton)
                        )
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            stringResource(R.string.status_installing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
