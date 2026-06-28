package com.evomrdm.iptvbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.evomrdm.iptvbox.core.designsystem.IptvColors
import com.evomrdm.iptvbox.core.model.CatalogItem
import com.evomrdm.iptvbox.core.model.ContentKind

@Composable
internal fun CompactContentCard(
    item: CatalogItem,
    onClick: () -> Unit,
    fixedWidth: Dp? = null,
    fixedRatio: Float? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val logoLike = item.kind == ContentKind.LIVE_CHANNEL || item.kind == ContentKind.RADIO
    val cardWidth = fixedWidth ?: if (logoLike) 124.dp else 108.dp
    val artworkRatio = fixedRatio ?: if (logoLike) 0.78f else item.posterRatio()
    val title = item.compactTitle()
    Surface(
        modifier = Modifier
            .width(cardWidth)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.035f, liftPx = -5f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else IptvColors.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column {
            ContentArtwork(
                title = title,
                kind = item.kind,
                logoUrl = item.logoUrl,
                showBadge = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(artworkRatio),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C151E))
                    .padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = IptvColors.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.metaLine(),
                    color = IptvColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun CatalogItem.compactTitle(): String {
    val clean = displayTitle()
    return if (kind == ContentKind.MOVIE) clean.readableMovieTitle() else clean
}

@Composable
internal fun SeriesGroupCard(
    group: SeriesGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.03f, liftPx = -5f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else IptvColors.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column {
            ContentArtwork(
                title = group.title,
                kind = ContentKind.SERIES,
                logoUrl = group.logoUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = group.title,
                    color = IptvColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${group.seasonCount} sezon · ${group.episodeCount} bölüm",
                    color = IptvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun SeasonCard(
    season: SeasonGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = -4f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else IptvColors.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = season.title,
                color = IptvColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${season.episodeCount} bölüm",
                color = IptvColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

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
            if (logoLike) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(18.dp),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xCC071017)),
                )
                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(10.dp),
                    color = Color(0xE6131D26),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shadowElevation = 1.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.16f)),
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.12f)),
                )
            }
        } else {
            Surface(
                color = kind.tint().copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, kind.tint().copy(alpha = 0.30f)),
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
        if (showBadge) {
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
    }
}

@Composable
internal fun ContentCard(
    item: CatalogItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -5f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onClick = onOpen),
        color = if (focused) TvFocusPanel else IptvColors.Panel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column {
            ContentArtwork(
                title = item.displayTitle(),
                kind = item.kind,
                logoUrl = item.logoUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(item.posterRatio()),
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.displayTitle(),
                    color = IptvColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.metaLine(),
                    color = IptvColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = if (focused) Color(0xFF10221F) else Color(0xFF0E1720),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, if (focused) IptvColors.Accent else Color(0xFF263240)),
                    ) {
                        Text(
                            text = "OK Oynat",
                            color = if (focused) IptvColors.Accent else IptvColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f).height(30.dp),
                        shape = RoundedCornerShape(7.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text(if (favorite) "Favoride" else "Favori", fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
