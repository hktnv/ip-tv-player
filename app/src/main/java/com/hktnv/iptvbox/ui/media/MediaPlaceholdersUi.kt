package com.hktnv.iptvbox.ui.media

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.designsystem.cardTitleSurface
import com.hktnv.iptvbox.core.designsystem.mediaCardRadius
import com.hktnv.iptvbox.core.designsystem.mediaCardSpacing
import com.hktnv.iptvbox.ui.common.TvRestingBorder

private val PlaceholderShape = RoundedCornerShape(mediaCardRadius)

@Composable
internal fun ContentCardPlaceholder(
    modifier: Modifier = Modifier,
    fixedWidth: Dp? = null,
    fixedRatio: Float? = null,
) {
    val cardWidth = fixedWidth ?: MediaCardCompactWidth
    val artworkRatio = fixedRatio ?: MediaCardArtworkRatio
    Surface(
        modifier = modifier.width(cardWidth),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        shape = PlaceholderShape,
        border = BorderStroke(1.dp, TvRestingBorder.copy(alpha = 0.42f)),
        shadowElevation = 0.dp,
    ) {
        Column {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(artworkRatio),
                shape = RoundedCornerShape(topStart = mediaCardRadius, topEnd = mediaCardRadius),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MediaCardRailInfoHeight)
                    .background(MaterialTheme.colorScheme.cardTitleSurface.copy(alpha = 0.86f))
                    .padding(start = 9.dp, end = 9.dp, top = 9.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(13.dp),
                    shape = RoundedCornerShape(5.dp),
                )
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .height(9.dp),
                    shape = RoundedCornerShape(5.dp),
                )
            }
        }
    }
}

@Composable
internal fun SearchResultPlaceholder(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 96.dp else 118.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        shape = PlaceholderShape,
        border = BorderStroke(1.dp, TvRestingBorder.copy(alpha = 0.42f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 7.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(mediaCardSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBlock(
                modifier = Modifier
                    .width(if (compact) 78.dp else 104.dp)
                    .height(if (compact) 72.dp else 94.dp),
                shape = RoundedCornerShape(mediaCardRadius),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            ) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(if (compact) 12.dp else 14.dp),
                    shape = RoundedCornerShape(5.dp),
                )
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.54f)
                        .height(if (compact) 10.dp else 11.dp),
                    shape = RoundedCornerShape(5.dp),
                )
            }
        }
    }
}

@Composable
internal fun ShimmerBlock(
    modifier: Modifier,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = modifier.background(brush = shimmerBrush(), shape = shape),
    )
}

@Composable
private fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)
    val transition = rememberInfiniteTransition(label = "media-placeholder-shimmer")
    val offset by transition.animateFloat(
        initialValue = -420f,
        targetValue = 840f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "media-placeholder-offset",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + 420f, 220f),
    )
}
