package com.hktnv.iptvbox.ui.media
import androidx.compose.material3.MaterialTheme
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
import com.hktnv.iptvbox.core.designsystem.mediaCardRadius
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.LocalPerformanceMode
import com.hktnv.iptvbox.ui.catalog.badgeContainerColor
import com.hktnv.iptvbox.ui.catalog.badgeContentColor
import com.hktnv.iptvbox.ui.catalog.badgeLabel

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
        modifier = modifier.background(MaterialTheme.colorScheme.background),
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
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(22.dp),
        )
        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.30f)))
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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)))
}

@Composable
private fun PlaceholderArtwork(title: String, kind: ContentKind) {
    Surface(
        color = kind.badgeContainerColor(),
        shape = RoundedCornerShape(mediaCardRadius),
    ) {
        Text(
            text = title.initials(),
            color = MaterialTheme.colorScheme.onSurface,
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
        color = kind.badgeContainerColor(),
        shape = RoundedCornerShape(5.dp),
    ) {
        Text(
            text = kind.badgeLabel(),
            color = kind.badgeContentColor(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}
