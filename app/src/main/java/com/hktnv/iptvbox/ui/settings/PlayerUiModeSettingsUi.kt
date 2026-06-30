package com.hktnv.iptvbox.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.player.PlayerUiMode
import com.hktnv.iptvbox.ui.common.tvClickable

@Composable
internal fun PlayerUiModePanelContent(
    selectedMode: PlayerUiMode,
    onModeSelected: (PlayerUiMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Oynatıcı arayüzü",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Standart Media3 arayüzü ile modern IPTV kontrol arayüzü arasında geçiş yapın.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerUiModeChip(
                mode = PlayerUiMode.CustomOsd,
                selected = selectedMode == PlayerUiMode.CustomOsd,
                onSelected = { onModeSelected(PlayerUiMode.CustomOsd) },
                modifier = Modifier.weight(1f),
            )
            PlayerUiModeChip(
                mode = PlayerUiMode.StandardMedia3,
                selected = selectedMode == PlayerUiMode.StandardMedia3,
                onSelected = { onModeSelected(PlayerUiMode.StandardMedia3) },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = "Kumandada OK ile değiştirin veya dokunarak seçin.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun PlayerUiModeChip(
    mode: PlayerUiMode,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceBorder
    }
    Surface(
        modifier = modifier.tvClickable(onClick = onSelected),
        color = container,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = mode.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = mode.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
        }
    }
}
