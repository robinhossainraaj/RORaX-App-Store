package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component sizes that are not spacing steps. Kept in the theme so screens
 * contain no raw dimensions.
 */
data class Dimensions(
    val hairline: Dp = 1.dp,
    val strokeThin: Dp = 2.dp,
    val strokeMedium: Dp = 3.dp,
    val dividerThickness: Dp = 1.dp,
    val iconTiny: Dp = 12.dp,
    val iconSmall: Dp = 14.dp,
    val iconMedium: Dp = 16.dp,
    val iconButton: Dp = 18.dp,
    val iconAction: Dp = 20.dp,
    val iconLarge: Dp = 24.dp,
    val iconHero: Dp = 40.dp,
    val iconBrand: Dp = 48.dp,
    val avatarCard: Dp = 72.dp,
    val avatarList: Dp = 56.dp,
    val avatarDetail: Dp = 96.dp,
    val splashLogo: Dp = 112.dp,
    val gridMinWidth: Dp = 160.dp,
    val screenshotWidth: Dp = 160.dp,
    val screenshotHeight: Dp = 280.dp,
    val tabletBreakpoint: Dp = 600.dp,
    val badgeDot: Dp = 8.dp,
    val progressHeight: Dp = 8.dp,
    val buttonHeight: Dp = 48.dp,
    val searchBarHeight: Dp = 48.dp,
    val actionColumnWidth: Dp = 112.dp,
    val emptyIconContainer: Dp = 80.dp,
    val emptyDescriptionWidth: Dp = 280.dp,
    val navSlideDistance: Dp = 16.dp
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }

val MaterialTheme.dimensions: Dimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current
