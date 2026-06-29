package com.hktnv.iptvbox.ui.catalog.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.model.SeasonGroup
import com.hktnv.iptvbox.model.SeriesGroup
import com.hktnv.iptvbox.ui.media.MediaCardGrid
import com.hktnv.iptvbox.ui.media.SeasonCard
import com.hktnv.iptvbox.ui.media.SeriesGroupCard

@Composable
internal fun SeriesGroupGrid(
    groups: List<SeriesGroup>,
    onOpen: (SeriesGroup) -> Unit,
    onLongClick: ((SeriesGroup) -> Unit)? = null,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    MediaCardGrid(
        items = groups,
        itemKey = { it.id },
        modifier = modifier,
        requestInitialFocus = requestInitialFocus,
        initialFocusRequester = initialFocusRequester,
        onRequestSideMenu = onRequestSideMenu,
    ) { group, itemModifier, onFocused ->
        SeriesGroupCard(
            group = group,
            onClick = { onOpen(group) },
            onLongClick = onLongClick?.let { { it(group) } },
            onFocused = onFocused,
            modifier = itemModifier,
        )
    }
}

@Composable
internal fun SeasonGroupGrid(
    seasons: List<SeasonGroup>,
    seriesTitle: String,
    onOpen: (SeasonGroup) -> Unit,
    onLongClick: ((SeasonGroup) -> Unit)? = null,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = false,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = seriesTitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MediaCardGrid(
            items = seasons,
            itemKey = { it.id },
            modifier = Modifier.weight(1f),
            requestInitialFocus = requestInitialFocus,
            initialFocusRequester = initialFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
        ) { season, itemModifier, onFocused ->
            SeasonCard(
                season = season,
                onClick = { onOpen(season) },
                onLongClick = onLongClick?.let { { it(season) } },
                onFocused = onFocused,
                modifier = itemModifier,
            )
        }
    }
}
