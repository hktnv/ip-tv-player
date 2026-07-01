package com.hktnv.iptvbox.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.core.designsystem.surfaceBorder
import com.hktnv.iptvbox.state.StartupBehavior

@Composable
internal fun StartupBehaviorDialog(
    selectedBehavior: StartupBehavior,
    onSelect: (StartupBehavior) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedFocusRequester = remember(selectedBehavior) { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_startup_title),
                fontSize = SettingsHeadingSp,
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StartupBehavior.entries.forEach { behavior ->
                    StartupBehaviorRow(
                        behavior = behavior,
                        selected = behavior == selectedBehavior,
                        focusRequester = if (behavior == selectedBehavior) selectedFocusRequester else null,
                        onSelect = {
                            onSelect(behavior)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {},
    )
    LaunchedEffect(selectedBehavior) {
        withFrameNanos { }
        runCatching { selectedFocusRequester.requestFocus() }
    }
}

@Composable
internal fun SettingsInfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = SettingsHeadingSp,
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = SettingsBodySp,
                lineHeight = SettingsBodySp * 1.35f,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun StartupBehaviorRow(
    behavior: StartupBehavior,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable()
            .clickable(onClick = onSelect),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = behavior.titleText(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = SettingsBodySp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = behavior.bodyText(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = SettingsCaptionSp,
                    lineHeight = SettingsCaptionSp * 1.3f,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun StartupBehavior.titleText(): String {
    return when (this) {
        StartupBehavior.Home -> stringResource(R.string.settings_startup_option_home)
        StartupBehavior.LastScreen -> stringResource(R.string.settings_startup_option_last_screen)
        StartupBehavior.LastStream -> stringResource(R.string.settings_startup_option_last_stream)
    }
}

@Composable
private fun StartupBehavior.bodyText(): String {
    return when (this) {
        StartupBehavior.Home -> stringResource(R.string.settings_startup_option_home_body)
        StartupBehavior.LastScreen -> stringResource(R.string.settings_startup_option_last_screen_body)
        StartupBehavior.LastStream -> stringResource(R.string.settings_startup_option_last_stream_body)
    }
}
