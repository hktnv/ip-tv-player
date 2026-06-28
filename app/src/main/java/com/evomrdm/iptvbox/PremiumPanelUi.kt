package com.evomrdm.iptvbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evomrdm.iptvbox.core.designsystem.IptvColors

@Composable
internal fun PremiumPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFF263240),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = IptvColors.Panel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(Modifier.padding(18.dp)) {
            content()
        }
    }
}
