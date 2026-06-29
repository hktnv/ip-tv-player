package com.hktnv.iptvbox.ui.common
import androidx.compose.material3.MaterialTheme
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
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

@Composable
internal fun PremiumPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.surfaceBorder,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(Modifier.padding(18.dp)) {
            content()
        }
    }
}
