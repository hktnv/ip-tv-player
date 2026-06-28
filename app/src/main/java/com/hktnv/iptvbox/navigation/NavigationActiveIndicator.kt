package com.hktnv.iptvbox.navigation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.core.designsystem.IptvColors

@Composable
internal fun NavigationActiveIndicator(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .width(3.dp)
            .height(if (compact) 18.dp else 22.dp)
            .background(IptvColors.Accent, RoundedCornerShape(999.dp)),
    )
}
