package com.hktnv.iptvbox.ui.playlist.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.model.PlaylistAutoUpdateHourOptions
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun AutoUpdateSelector(
    selectedHours: Int,
    updatedAtEpochMillis: Long,
    onSelect: (Int) -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    DetailPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailSectionTitle(stringResource(R.string.playlist_auto_refresh_interval))
            AutoUpdateInfoButton(
                selected = showInfo,
                onClick = { showInfo = !showInfo },
            )
        }
        Text(
            text = playlistLastUpdateText(updatedAtEpochMillis),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 15.sp,
        )
        AnimatedVisibility(visible = showInfo) {
            Text(
                text = stringResource(R.string.playlist_auto_refresh_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlaylistAutoUpdateHourOptions.forEach { hours ->
                AutoUpdateOption(
                    label = autoUpdateOptionLabel(hours),
                    selected = selectedHours == hours,
                    onClick = { onSelect(hours) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AutoUpdateInfoButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .size(36.dp)
            .tvFocusLift(focused = focused, scale = 1.02f, liftPx = 0f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(onClick = onClick),
        color = if (focused) TvFocusPanel else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) TvFocusBorder else TvRestingBorder),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = stringResource(R.string.playlist_auto_refresh_info_toggle),
            modifier = Modifier.padding(8.dp),
            tint = if (selected || focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AutoUpdateOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .tvFocusLift(focused = focused, scale = 1.012f, liftPx = 0f)
            .onFocusChanged { focused = it.isFocused }
            .defaultMinSize(minHeight = 46.dp)
            .tvClickable(onClick = onClick),
        color = when {
            focused -> TvFocusPanel
            selected -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(
            if (focused) 2.dp else 1.dp,
            when {
                focused -> TvFocusBorder
                selected -> MaterialTheme.colorScheme.surfaceBorder
                else -> TvRestingBorder
            },
        ),
        shadowElevation = tvFocusElevation(focused = focused, resting = 0.dp, focusedElevation = 0.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 11.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun autoUpdateOptionLabel(hours: Int): String {
    return when (hours) {
        6 -> stringResource(R.string.playlist_auto_refresh_6_hours)
        12 -> stringResource(R.string.playlist_auto_refresh_12_hours)
        24 -> stringResource(R.string.playlist_auto_refresh_daily)
        else -> stringResource(R.string.playlist_auto_refresh_off)
    }
}

@Composable
private fun playlistLastUpdateText(updatedAtEpochMillis: Long): String {
    if (updatedAtEpochMillis <= 0L) {
        return stringResource(R.string.playlist_last_update_never)
    }
    val now = System.currentTimeMillis()
    val diffMillis = (now - updatedAtEpochMillis).coerceAtLeast(0L)
    if (diffMillis < RecentUpdateThresholdMillis) {
        return stringResource(R.string.playlist_last_update_recent)
    }
    val trLocale = remember { Locale.forLanguageTag("tr-TR") }
    val timeText = remember(updatedAtEpochMillis, trLocale) {
        SimpleDateFormat("HH:mm", trLocale).format(Date(updatedAtEpochMillis))
    }
    if (isSameDay(updatedAtEpochMillis, now, trLocale)) {
        return stringResource(R.string.playlist_last_update_today, timeText)
    }
    val dateText = remember(updatedAtEpochMillis, trLocale) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", trLocale).format(Date(updatedAtEpochMillis))
    }
    return stringResource(R.string.playlist_last_update_date, dateText)
}

private fun isSameDay(firstEpochMillis: Long, secondEpochMillis: Long, locale: Locale): Boolean {
    val calendar = Calendar.getInstance(locale)
    calendar.timeInMillis = firstEpochMillis
    val firstYear = calendar.get(Calendar.YEAR)
    val firstDay = calendar.get(Calendar.DAY_OF_YEAR)
    calendar.timeInMillis = secondEpochMillis
    return firstYear == calendar.get(Calendar.YEAR) && firstDay == calendar.get(Calendar.DAY_OF_YEAR)
}

private const val RecentUpdateThresholdMillis = 10 * 60 * 1_000L
