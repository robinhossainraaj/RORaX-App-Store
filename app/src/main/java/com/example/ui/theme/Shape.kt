package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val ExtraSmallShape = RoundedCornerShape(4.dp)
private val SmallShape = RoundedCornerShape(8.dp)
private val MediumShape = RoundedCornerShape(12.dp)
private val LargeShape = RoundedCornerShape(16.dp)
private val ExtraLargeShape = RoundedCornerShape(28.dp)
private val PillShape = RoundedCornerShape(50)

val AppShapes = Shapes(
    extraSmall = ExtraSmallShape,
    small = SmallShape,
    medium = MediumShape,
    large = LargeShape,
    extraLarge = ExtraLargeShape
)

val AppCardShape = LargeShape
val AppButtonShape = PillShape
val AppIconShape = LargeShape
val AppDetailIconShape = ExtraLargeShape
val AppScreenshotShape = MediumShape
val AppSearchPillShape = PillShape
val AppChipShape = PillShape
val AppBadgeShape = SmallShape
val AppDotShape = CircleShape
