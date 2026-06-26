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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .width(if (item.kind == ContentKind.LIVE_CHANNEL || item.kind == ContentKind.RADIO) 124.dp else 108.dp)
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
                title = item.displayTitle(),
                kind = item.kind,
                logoUrl = item.logoUrl,
                showBadge = false,
                modifier = Modifier.fillMaxWidth().aspectRatio(item.posterRatio()),
            )
            Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.displayTitle(),
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

@Composable
internal fun SeriesGroupCard(
    group: SeriesGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.03f, liftPx = -5f)
            .onFocusChanged { focused = it.isFocused }
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
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = -4f)
            .onFocusChanged { focused = it.isFocused }
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
        modifier = modifier.background(Color(0xFF0B141C)),
        contentAlignment = Alignment.Center,
    ) {
        if (performance.loadImages && !logoUrl.isNullOrBlank()) {
            if (logoLike) {
                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp),
                    color = Color(0xFFF3F6F8),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
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
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -5f)
            .onFocusChanged { focused = it.isFocused }
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
