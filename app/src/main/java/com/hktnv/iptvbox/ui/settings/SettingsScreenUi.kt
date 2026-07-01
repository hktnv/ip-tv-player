package com.hktnv.iptvbox.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.BuildConfig
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.model.ScreenBottomPadding
import com.hktnv.iptvbox.state.StartupBehavior

@Composable
internal fun SettingsScreen(
    playlistCount: Int,
    startupBehavior: StartupBehavior,
    onStartupBehaviorChange: (StartupBehavior) -> Unit,
    onOpenPlaylistEntry: () -> Unit,
    contentPadding: Dp,
    initialFocusRequester: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
) {
    val layout = rememberSettingsLayout()
    val cardPadding = layout.cardPadding
    val gap = 12.dp
    val playlistsFocusRequester = initialFocusRequester ?: remember { FocusRequester() }
    val playerUiFocusRequester = remember { FocusRequester() }
    val startupFocusRequester = remember { FocusRequester() }
    val privacyFocusRequester = remember { FocusRequester() }
    val aboutFocusRequester = remember { FocusRequester() }
    var showStartupDialog by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<SettingsInfoDialogState?>(null) }

    LaunchedEffect(initialFocusRequester) {
        if (initialFocusRequester != null) {
            withFrameNanos { }
            runCatching { playlistsFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = contentPadding)
            .padding(top = 12.dp, bottom = ScreenBottomPadding),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        SettingsHeader()
        when (layout) {
            SettingsLayout.Expanded -> ExpandedSettingsGrid(
                playlistCount = playlistCount,
                startupBehavior = startupBehavior,
                cardPadding = cardPadding,
                gap = gap,
                playlistsFocusRequester = playlistsFocusRequester,
                playerUiFocusRequester = playerUiFocusRequester,
                startupFocusRequester = startupFocusRequester,
                privacyFocusRequester = privacyFocusRequester,
                aboutFocusRequester = aboutFocusRequester,
                onOpenPlaylistEntry = onOpenPlaylistEntry,
                onShowPlayerUi = { infoDialog = SettingsInfoDialogState.PlayerUi },
                onShowStartupDialog = { showStartupDialog = true },
                onShowPrivacy = { infoDialog = SettingsInfoDialogState.Privacy },
                onShowAbout = { infoDialog = SettingsInfoDialogState.About },
                onRequestSideMenu = onRequestSideMenu,
            )
            SettingsLayout.Medium -> MediumSettingsGrid(
                playlistCount = playlistCount,
                startupBehavior = startupBehavior,
                cardPadding = cardPadding,
                gap = gap,
                playlistsFocusRequester = playlistsFocusRequester,
                playerUiFocusRequester = playerUiFocusRequester,
                startupFocusRequester = startupFocusRequester,
                privacyFocusRequester = privacyFocusRequester,
                aboutFocusRequester = aboutFocusRequester,
                onOpenPlaylistEntry = onOpenPlaylistEntry,
                onShowPlayerUi = { infoDialog = SettingsInfoDialogState.PlayerUi },
                onShowStartupDialog = { showStartupDialog = true },
                onShowPrivacy = { infoDialog = SettingsInfoDialogState.Privacy },
                onShowAbout = { infoDialog = SettingsInfoDialogState.About },
                onRequestSideMenu = onRequestSideMenu,
            )
            SettingsLayout.Compact -> CompactSettingsGrid(
                playlistCount = playlistCount,
                startupBehavior = startupBehavior,
                cardPadding = cardPadding,
                gap = gap,
                playlistsFocusRequester = playlistsFocusRequester,
                playerUiFocusRequester = playerUiFocusRequester,
                startupFocusRequester = startupFocusRequester,
                privacyFocusRequester = privacyFocusRequester,
                aboutFocusRequester = aboutFocusRequester,
                onOpenPlaylistEntry = onOpenPlaylistEntry,
                onShowPlayerUi = { infoDialog = SettingsInfoDialogState.PlayerUi },
                onShowStartupDialog = { showStartupDialog = true },
                onShowPrivacy = { infoDialog = SettingsInfoDialogState.Privacy },
                onShowAbout = { infoDialog = SettingsInfoDialogState.About },
                onRequestSideMenu = onRequestSideMenu,
            )
        }
    }

    if (showStartupDialog) {
        StartupBehaviorDialog(
            selectedBehavior = startupBehavior,
            onSelect = onStartupBehaviorChange,
            onDismiss = { showStartupDialog = false },
        )
    }
    infoDialog?.let { dialog ->
        SettingsInfoDialog(
            title = stringResource(dialog.titleRes),
            body = stringResource(dialog.bodyRes, BuildConfig.VERSION_NAME),
            onDismiss = { infoDialog = null },
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.settings_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = SettingsHeadingSp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.settings_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = SettingsCaptionSp,
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun rememberSettingsLayout(): SettingsLayout {
    val activity = LocalContext.current as? Activity
    val widthClass = activity?.let { calculateWindowSizeClass(it).widthSizeClass }
        ?: WindowWidthSizeClass.Compact
    return when (widthClass) {
        WindowWidthSizeClass.Expanded -> SettingsLayout.Expanded
        WindowWidthSizeClass.Medium -> SettingsLayout.Medium
        else -> SettingsLayout.Compact
    }
}

private enum class SettingsInfoDialogState(
    val titleRes: Int,
    val bodyRes: Int,
) {
    Privacy(R.string.settings_privacy_title, R.string.settings_privacy_detail_body),
    PlayerUi(R.string.settings_player_ui_title, R.string.settings_player_ui_detail_body),
    About(R.string.settings_about_title, R.string.settings_about_detail_body),
}
