package com.evomrdm.iptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.model.ContentKind

@Composable
internal fun ContentArtwork(
    title: String,
    kind: ContentKind,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
) {
    val performance = LocalPerformanceMode.current
    val logoLike = kind == ContentKind.LIVE_CHANNEL || kind == ContentKind.RADIO
    Box(
        modifier = modifier.background(Color(0xFF0A121A)),
        contentAlignment = Alignment.Center,
    ) {
        if (performance.loadImages && !logoUrl.isNullOrBlank()) {
            if (logoLike) LogoArtwork(logoUrl) else PosterArtwork(logoUrl)
        } else {
            PlaceholderArtwork(title, kind)
        }
        if (showBadge) ContentKindBadge(kind)
    }
}

@Composable
private fun LogoArtwork(logoUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111A23)),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(22.dp),
        )
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.30f)))
    }
}

@Composable
private fun PosterArtwork(logoUrl: String) {
    AsyncImage(
        model = logoUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
}

@Composable
private fun PlaceholderArtwork(title: String, kind: ContentKind) {
    Surface(
        color = kind.tint().copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = title.initials(),
            color = IptvColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun BoxScope.ContentKindBadge(kind: ContentKind) {
    Surface(
        modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
        color = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(5.dp),
    ) {
        Text(
            text = kind.badgeLabel(),
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}
