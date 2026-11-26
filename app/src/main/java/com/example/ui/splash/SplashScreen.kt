package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.example.R
import com.example.ui.theme.AppMotion
import com.example.ui.theme.WordmarkTracking
import com.example.ui.theme.dimensions
import com.example.ui.theme.spacing
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions
    val initialTitleOffsetPx = with(LocalDensity.current) { spacing.lg.toPx() }

    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember(initialTitleOffsetPx) { Animatable(initialTitleOffsetPx) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(AppMotion.Long2))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = AppMotion.Long2,
                    easing = AppMotion.OvershootEasing
                )
            )
        }

        delay(AppMotion.Medium4.toLong())
        launch {
            titleAlpha.animateTo(1f, animationSpec = tween(AppMotion.Long2))
        }
        launch {
            titleOffset.animateTo(0f, animationSpec = tween(AppMotion.Long2))
        }

        delay(AppMotion.Medium2.toLong())
        launch {
            subtitleAlpha.animateTo(1f, animationSpec = tween(AppMotion.Medium4))
        }

        delay(AppMotion.Long2.toLong())
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(dimensions.splashLogo)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(spacing.xl))

            Text(
                text = stringResource(R.string.wordmark),
                style = MaterialTheme.typography.displayLarge.copy(
                    letterSpacing = WordmarkTracking
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .offset { IntOffset(0, titleOffset.value.roundToInt()) }
                    .alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = stringResource(R.string.app_store_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
