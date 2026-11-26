package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.R
import com.example.data.repository.StoreUiState
import com.example.domain.InstallState
import com.example.ui.StoreViewModel
import com.example.ui.components.AppCard
import com.example.ui.components.AppCardSkeleton
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.AppDotShape
import com.example.ui.theme.AppSearchPillShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.onSurfaceMuted
import com.example.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StoreViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToInstalled: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val installStates by viewModel.installStates.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    val isRefreshing = uiState is StoreUiState.Loading && (uiState as? StoreUiState.Success)?.apps != null

    Scaffold(
        topBar = {
            HomeTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                unreadCount = unreadCount,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onToggleSearch = viewModel::setSearchActive,
                onNotificationsClick = onNavigateToNotifications
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is StoreUiState.Loading -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= dimensions.tabletBreakpoint) {
                            GridCells.Adaptive(dimensions.gridMinWidth)
                        } else {
                            GridCells.Fixed(2)
                        }
                        LazyVerticalGrid(
                            columns = columns,
                            contentPadding = PaddingValues(
                                start = spacing.lg,
                                end = spacing.lg,
                                top = spacing.sm,
                                bottom = spacing.lg
                            ),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(6) {
                                AppCardSkeleton()
                            }
                        }
                    }
                }

                is StoreUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.xxxl),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(dimensions.iconBrand)
                        )
                        Spacer(modifier = Modifier.height(spacing.lg))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(spacing.xl))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("retry_button")
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(dimensions.iconButton)
                            )
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                is StoreUiState.Success -> {
                    val filteredApps = remember(state.apps, searchQuery) {
                        if (searchQuery.isBlank()) {
                            state.apps
                        } else {
                            state.apps.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.packageName.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    val updatesCount = remember(state.apps, installStates) {
                        state.apps.count { installStates[it.packageName] is InstallState.UpdateAvailable }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = if (maxWidth >= dimensions.tabletBreakpoint) {
                            GridCells.Adaptive(dimensions.gridMinWidth)
                        } else {
                            GridCells.Fixed(2)
                        }

                        if (state.apps.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.no_apps_yet),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceMuted
                                )
                            }
                        } else if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceMuted,
                                        modifier = Modifier.size(dimensions.iconHero)
                                    )
                                    Spacer(modifier = Modifier.height(spacing.md))
                                    Text(
                                        text = stringResource(R.string.no_apps_found, searchQuery),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = columns,
                                contentPadding = PaddingValues(
                                    start = spacing.lg,
                                    end = spacing.lg,
                                    top = spacing.sm,
                                    bottom = spacing.lg
                                ),
                                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                                verticalArrangement = Arrangement.spacedBy(spacing.md),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (state.isOffline) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        OfflineBanner()
                                    }
                                }

                                if (updatesCount > 0 && searchQuery.isBlank()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        UpdatesAvailableBanner(
                                            count = updatesCount,
                                            onClick = onNavigateToInstalled
                                        )
                                    }
                                }

                                items(filteredApps, key = { it.packageName }) { app ->
                                    val installState = installStates[app.packageName] ?: InstallState.NotInstalled
                                    AppCard(
                                        app = app,
                                        installState = installState,
                                        onClick = { onAppClick(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    unreadCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onNotificationsClick: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    TopAppBar(
        title = {
            if (isSearchActive) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.searchBarHeight)
                        .clip(AppSearchPillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("search_text_input"),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = { onToggleSearch(false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.search_close),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(
                    onClick = { onToggleSearch(true) },
                    modifier = Modifier.testTag("search_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_hint),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Box {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("notifications_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = stringResource(R.string.notifications_title),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = spacing.sm, end = spacing.sm)
                                .size(dimensions.badgeDot)
                                .clip(AppDotShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = spacing.lg, vertical = spacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(dimensions.iconMedium)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpdatesAvailableBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppCardShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = spacing.lg, vertical = spacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(dimensions.iconButton)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = stringResource(R.string.updates_available_banner, count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = stringResource(R.string.btn_view),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
