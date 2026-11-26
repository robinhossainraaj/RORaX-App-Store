package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.AppDotShape
import com.example.ui.theme.AppScreenshotShape
import com.example.ui.theme.dimensions
import com.example.ui.theme.spacing

@Composable
fun ScreenshotRow(
    screenshots: List<String>,
    modifier: Modifier = Modifier
) {
    if (screenshots.isEmpty()) return

    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val dimensions = MaterialTheme.dimensions
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.screenshots_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = spacing.lg)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(screenshots) { index, url ->
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.screenshot_item, index + 1),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .width(dimensions.screenshotWidth)
                                .height(dimensions.screenshotHeight)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    },
                    modifier = Modifier
                        .height(dimensions.screenshotHeight)
                        .width(dimensions.screenshotWidth)
                        .clip(AppScreenshotShape)
                        .clickable { selectedIndex = index }
                        .testTag("screenshot_item_$index")
                )
            }
        }
    }

    selectedIndex?.let { initialIndex ->
        Dialog(
            onDismissRequest = { selectedIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { screenshots.size }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(screenshots[page])
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.screenshot_item, page + 1),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                }
                        )
                    }
                }

                IconButton(
                    onClick = { selectedIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.xxl)
                        .clip(AppDotShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = spacing.xxxl)
                        .clip(AppDotShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                        .padding(horizontal = spacing.lg, vertical = spacing.sm)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}
