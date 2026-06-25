package com.evomrdm.iptvbox.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object IptvColors {
    val Night = Color(0xFF070A0E)
    val Panel = Color(0xFF0F1720)
    val PanelSoft = Color(0xFF151F2A)
    val Accent = Color(0xFF00D6A3)
    val Warning = Color(0xFFFFC857)
    val TextPrimary = Color(0xFFF4F7FA)
    val TextSecondary = Color(0xFFC4CDD8)
}

private val IptvColorScheme: ColorScheme = darkColorScheme(
    primary = IptvColors.Accent,
    onPrimary = IptvColors.Night,
    background = IptvColors.Night,
    onBackground = IptvColors.TextPrimary,
    surface = IptvColors.Panel,
    onSurface = IptvColors.TextPrimary,
    surfaceVariant = IptvColors.PanelSoft,
    onSurfaceVariant = IptvColors.TextSecondary,
)

@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IptvColorScheme,
        content = content,
    )
}

@Composable
fun FocusPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .scale(if (focused) 1.035f else 1f)
            .onFocusChanged { focused = it.isFocused }
            .focusable(),
        shape = shape,
        color = IptvColors.Panel,
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) IptvColors.Accent else Color(0xFF273340)),
        tonalElevation = if (focused) 6.dp else 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(IptvColors.Panel)
                .padding(contentPadding),
        ) {
            content()
        }
    }
}

@Composable
fun RailButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) IptvColors.Accent else Color.Transparent,
            contentColor = if (selected) IptvColors.Night else IptvColors.TextPrimary,
        ),
        border = BorderStroke(1.dp, if (selected) IptvColors.Accent else Color(0xFF2A3542)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
