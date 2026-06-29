package com.hktnv.iptvbox.ui.media
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hktnv.iptvbox.core.designsystem.cardTitleSurface
import com.hktnv.iptvbox.core.designsystem.mediaCardRadius
import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder

private val MediaCardShape = RoundedCornerShape(mediaCardRadius)
internal val MediaCardCompactWidth = 110.dp
private const val MediaCardArtworkRatio = 0.78f
private val MediaCardRailInfoHeight = 64.dp
private val MediaCardGridInfoHeight = MediaCardRailInfoHeight

@Composable
internal fun CompactContentCard(
    item: CatalogItem,
    onClick: () -> Unit,
    favorite: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    fixedWidth: Dp? = null,
    fixedRatio: Float? = null,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val cardWidth = fixedWidth ?: MediaCardCompactWidth
    val artworkRatio = fixedRatio ?: MediaCardArtworkRatio
    val title = item.compactTitle()
    Surface(
        modifier = Modifier
            .width(cardWidth)
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.035f, liftPx = -5f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onLongClick = onLongClick, onClick = onClick),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = MediaCardShape,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column {
            FavoriteArtworkFrame(
                title = title,
                kind = item.kind,
                logoUrl = item.logoUrl,
                favorite = favorite,
                showBadge = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(artworkRatio),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MediaCardRailInfoHeight)
                    .background(MaterialTheme.colorScheme.cardTitleSurface)
                    .padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.metaLine(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun CatalogItem.compactTitle(): String {
    return displayTitle()
}

@Composable
internal fun SeriesGroupCard(
    group: SeriesGroup,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val title = group.title.readableContentTitle()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.03f, liftPx = -5f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onLongClick = onLongClick, onClick = onClick),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = MediaCardShape,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column {
            ContentArtwork(
                title = title,
                kind = ContentKind.SERIES,
                logoUrl = group.logoUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(MediaCardArtworkRatio),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(MediaCardRailInfoHeight)
                    .background(MaterialTheme.colorScheme.cardTitleSurface)
                    .padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${group.seasonCount} sezon · ${group.episodeCount} bölüm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
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
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val title = season.title.readableContentTitle()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = -4f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onLongClick = onLongClick, onClick = onClick),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = MediaCardShape,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 12.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${season.episodeCount} bölüm",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
internal fun ContentCard(
    item: CatalogItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val title = item.compactTitle()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (focused) 1f else 0f)
            .tvFocusLift(focused = focused, scale = 1.025f, liftPx = -5f)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .tvClickable(onLongClick = onLongClick, onClick = onOpen),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surface,
        shape = MediaCardShape,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Column {
            FavoriteArtworkFrame(
                title = title,
                kind = item.kind,
                logoUrl = item.logoUrl,
                favorite = favorite,
                showBadge = true,
                modifier = Modifier.fillMaxWidth().aspectRatio(MediaCardArtworkRatio),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(MediaCardGridInfoHeight)
                    .background(MaterialTheme.colorScheme.cardTitleSurface)
                    .padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.metaLine(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FavoriteArtworkFrame(
    title: String,
    kind: ContentKind,
    logoUrl: String?,
    favorite: Boolean,
    showBadge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        ContentArtwork(
            title = title,
            kind = kind,
            logoUrl = logoUrl,
            showBadge = showBadge,
            modifier = Modifier.fillMaxWidth().matchParentSize(),
        )
        if (favorite) {
            FavoriteIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
            )
        }
    }
}

@Composable
internal fun FavoriteIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.80f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)),
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(5.dp).size(13.dp),
        )
    }
}
