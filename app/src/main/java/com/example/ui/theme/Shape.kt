package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppCardShape = RoundedCornerShape(16.dp)
val AppButtonShape = RoundedCornerShape(12.dp)
val AppIconShape = RoundedCornerShape(18.dp)
val AppDetailIconShape = RoundedCornerShape(22.dp)
val AppScreenshotShape = RoundedCornerShape(12.dp)
val AppSearchPillShape = CircleShape

val AppShapes = Shapes(
    small = AppButtonShape,
    medium = AppScreenshotShape,
    large = AppCardShape,
    extraLarge = AppIconShape
)
