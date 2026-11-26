package com.example.ui.notifications

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.data.Constants
import com.example.data.local.AppNotification
import com.example.ui.StoreViewModel
import com.example.ui.theme.AppBadgeShape
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.AppDotShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.onSurfaceMuted
import com.example.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val notifications by viewModel.notificationsHistory.collectAsState()
    val isSyncing by viewModel.isSyncingNotifications.collectAsState()
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    LaunchedEffect(Unit) {
        viewModel.syncNotifications()
        viewModel.markNotificationsAsRead()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncNotifications() },
                        modifier = Modifier.testTag("notifications_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.syncNotifications() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.xxxl),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(dimensions.emptyIconContainer)
                                .clip(AppDotShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(dimensions.iconHero)
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.xl))

                        Text(
                            text = stringResource(R.string.notifications_all_caught_up),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(spacing.sm))

                        Text(
                            text = stringResource(R.string.notifications_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(dimensions.emptyDescriptionWidth)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = spacing.lg,
                        end = spacing.lg,
                        top = spacing.md,
                        bottom = spacing.xxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                if (item.link.isNotBlank()) {
                                    openNotificationLink(context, item.link)
                                } else if (item.packageName.isNotBlank()) {
                                    onAppClick(item.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions
    val now = System.currentTimeMillis()
    val isScheduled = item.timestamp > now
    val isUpdate = item.type.equals("update", ignoreCase = true) || item.message.startsWith("Update", ignoreCase = true)
    val isAnnouncement = item.type.equals("announcement", ignoreCase = true)

    val (badgeText, badgeColor, iconVector) = when {
        isUpdate -> Triple(
            stringResource(R.string.notification_badge_update),
            MaterialTheme.colorScheme.primary,
            Icons.Rounded.SystemUpdate
        )
        isAnnouncement -> Triple(
            stringResource(R.string.notification_badge_announcement),
            MaterialTheme.colorScheme.tertiary,
            Icons.Rounded.Campaign
        )
        else -> Triple(
            stringResource(R.string.notification_badge_notice),
            MaterialTheme.colorScheme.secondary,
            Icons.Rounded.Notifications
        )
    }

    val cardBorderColor = if (!item.isRead) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = AppCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(dimensions.hairline, cardBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCardShape)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensions.iconHero)
                        .clip(AppBadgeShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(dimensions.iconAction)
                    )
                }

                Spacer(modifier = Modifier.width(spacing.sm))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(AppBadgeShape)
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }

                if (isScheduled) {
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(AppBadgeShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = spacing.sm, vertical = spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(dimensions.iconTiny)
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = stringResource(R.string.notification_scheduled_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatNotificationTimeShort(item.timestamp, now),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.isRead) {
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Box(
                        modifier = Modifier
                            .size(dimensions.badgeDot)
                            .clip(AppDotShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(spacing.xs))
            }

            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isScheduled) {
                Spacer(modifier = Modifier.height(spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(AppBadgeShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = spacing.sm, vertical = spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceMuted,
                        modifier = Modifier.size(dimensions.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = stringResource(
                            R.string.notification_scheduled_time,
                            formatDubaiDateTime(item.timestamp)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceMuted
                    )
                }
            }

            val hasLink = item.link.isNotBlank()
            val hasPackage = item.packageName.isNotBlank()

            if (hasLink || hasPackage) {
                Spacer(modifier = Modifier.height(spacing.md))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = dimensions.dividerThickness
                )
                Spacer(modifier = Modifier.height(spacing.sm))

                val isTelegram = item.link.contains("t.me", ignoreCase = true) ||
                        item.link.contains("telegram", ignoreCase = true)

                val actionLabel = when {
                    isTelegram -> stringResource(R.string.notification_action_telegram)
                    hasLink -> stringResource(R.string.notification_action_open_link)
                    else -> stringResource(R.string.notification_action_view_app)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppBadgeShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onClick() }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTelegram) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(dimensions.iconMedium)
                            )
                        } else if (hasPackage) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(dimensions.iconMedium)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(dimensions.iconMedium)
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Icon(
                        imageVector = if (hasPackage) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(dimensions.iconSmall)
                    )
                }
            }
        }
    }
}

private fun openNotificationLink(context: android.content.Context, link: String) {
    try {
        val uri = Uri.parse(link)
        if (uri.scheme == "http" || uri.scheme == "https") {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (_: Exception) {}
}

fun formatNotificationTimeShort(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp > now) {
        val diffMs = timestamp - now
        val minutes = diffMs / 60000
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "In <1m"
            minutes < 60 -> "In ${minutes}m"
            hours < 24 -> "In ${hours}h"
            days < 7 -> "In ${days}d"
            else -> {
                val sdf = SimpleDateFormat("dd MMM", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone(Constants.DUBAI_ZONE_ID)
                }
                sdf.format(timestamp)
            }
        }
    }

    val diffMs = now - timestamp
    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale.US)
            sdf.format(timestamp)
        }
    }
}

fun formatDubaiDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a (z)", Locale.US).apply {
        timeZone = TimeZone.getTimeZone(Constants.DUBAI_ZONE_ID)
    }
    return sdf.format(timestamp)
}
