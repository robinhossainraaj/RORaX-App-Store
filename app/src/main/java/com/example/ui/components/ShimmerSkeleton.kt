package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.theme.AppBadgeShape
import com.example.ui.theme.AppCardShape
import com.example.ui.theme.AppIconShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.spacing

@Composable
fun AppCardSkeleton(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions
    val placeholder = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = AppCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(dimensions.hairline, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
        ) {
            Box(
                modifier = Modifier
                    .size(dimensions.avatarCard)
                    .clip(AppIconShape)
                    .background(placeholder)
            )

            Spacer(Modifier.height(spacing.md))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(spacing.lg)
                    .clip(AppBadgeShape)
                    .background(placeholder)
            )

            Spacer(Modifier.height(spacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(spacing.md)
                    .clip(AppBadgeShape)
                    .background(placeholder)
            )

            Spacer(Modifier.height(spacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(spacing.xl)
                    .clip(AppBadgeShape)
                    .background(placeholder)
            )
        }
    }
}
