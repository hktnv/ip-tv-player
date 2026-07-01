package com.hktnv.iptvbox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hktnv.iptvbox.BuildConfig
import com.hktnv.iptvbox.R
import com.hktnv.iptvbox.state.StartupBehavior

@Composable
internal fun ExpandedSettingsGrid(
    playlistCount: Int,
    startupBehavior: StartupBehavior,
    cardPadding: Dp,
    gap: Dp,
    playlistsFocusRequester: FocusRequester,
    startupFocusRequester: FocusRequester,
    privacyFocusRequester: FocusRequester,
    aboutFocusRequester: FocusRequester,
    onOpenPlaylistEntry: () -> Unit,
    onShowStartupDialog: () -> Unit,
    onShowPrivacy: () -> Unit,
    onShowAbout: () -> Unit,
    onRequestSideMenu: (() -> Unit)?,
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(gap)) {
        PlaylistsCard(
            count = playlistCount,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = playlistsFocusRequester,
            right = startupFocusRequester,
            down = privacyFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
            onClick = onOpenPlaylistEntry,
        )
        PlayerUiCard(Modifier.weight(1f).fillMaxHeight(), cardPadding)
        StartupCard(
            behavior = startupBehavior,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = startupFocusRequester,
            left = playlistsFocusRequester,
            down = aboutFocusRequester,
            onClick = onShowStartupDialog,
        )
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(gap)) {
        PrivacyCard(
            modifier = Modifier.weight(2f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = privacyFocusRequester,
            up = playlistsFocusRequester,
            right = aboutFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
            onShowPrivacy = onShowPrivacy,
        )
        AboutCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = aboutFocusRequester,
            left = privacyFocusRequester,
            up = startupFocusRequester,
            onClick = onShowAbout,
        )
    }
}

@Composable
internal fun MediumSettingsGrid(
    playlistCount: Int,
    startupBehavior: StartupBehavior,
    cardPadding: Dp,
    gap: Dp,
    playlistsFocusRequester: FocusRequester,
    startupFocusRequester: FocusRequester,
    privacyFocusRequester: FocusRequester,
    aboutFocusRequester: FocusRequester,
    onOpenPlaylistEntry: () -> Unit,
    onShowStartupDialog: () -> Unit,
    onShowPrivacy: () -> Unit,
    onShowAbout: () -> Unit,
    onRequestSideMenu: (() -> Unit)?,
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(gap)) {
        PlaylistsCard(
            count = playlistCount,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = playlistsFocusRequester,
            down = startupFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
            onClick = onOpenPlaylistEntry,
        )
        PlayerUiCard(Modifier.weight(1f).fillMaxHeight(), cardPadding)
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(gap)) {
        StartupCard(
            behavior = startupBehavior,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = startupFocusRequester,
            right = privacyFocusRequester,
            up = playlistsFocusRequester,
            down = aboutFocusRequester,
            onRequestSideMenu = onRequestSideMenu,
            onClick = onShowStartupDialog,
        )
        PrivacyCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            padding = cardPadding,
            focusRequester = privacyFocusRequester,
            left = startupFocusRequester,
            down = aboutFocusRequester,
            onShowPrivacy = onShowPrivacy,
        )
    }
    AboutCard(
        modifier = Modifier.fillMaxWidth(),
        padding = cardPadding,
        focusRequester = aboutFocusRequester,
        up = startupFocusRequester,
        onRequestSideMenu = onRequestSideMenu,
        onClick = onShowAbout,
    )
}

@Composable
internal fun CompactSettingsGrid(
    playlistCount: Int,
    startupBehavior: StartupBehavior,
    cardPadding: Dp,
    gap: Dp,
    playlistsFocusRequester: FocusRequester,
    startupFocusRequester: FocusRequester,
    privacyFocusRequester: FocusRequester,
    aboutFocusRequester: FocusRequester,
    onOpenPlaylistEntry: () -> Unit,
    onShowStartupDialog: () -> Unit,
    onShowPrivacy: () -> Unit,
    onShowAbout: () -> Unit,
    onRequestSideMenu: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        PlaylistsCard(playlistCount, Modifier.fillMaxWidth(), cardPadding, playlistsFocusRequester, down = startupFocusRequester, onRequestSideMenu = onRequestSideMenu, onClick = onOpenPlaylistEntry)
        PlayerUiCard(Modifier.fillMaxWidth(), cardPadding)
        StartupCard(startupBehavior, Modifier.fillMaxWidth(), cardPadding, startupFocusRequester, up = playlistsFocusRequester, down = privacyFocusRequester, onRequestSideMenu = onRequestSideMenu, onClick = onShowStartupDialog)
        PrivacyCard(Modifier.fillMaxWidth(), cardPadding, privacyFocusRequester, up = startupFocusRequester, down = aboutFocusRequester, onRequestSideMenu = onRequestSideMenu, onShowPrivacy = onShowPrivacy)
        AboutCard(Modifier.fillMaxWidth(), cardPadding, aboutFocusRequester, up = privacyFocusRequester, onRequestSideMenu = onRequestSideMenu, onClick = onShowAbout)
    }
}

@Composable
private fun PlaylistsCard(
    count: Int,
    modifier: Modifier,
    padding: Dp,
    focusRequester: FocusRequester,
    right: FocusRequester? = null,
    down: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)?,
    onClick: () -> Unit,
) = SettingsActionCard(
    title = stringResource(R.string.settings_playlists_title),
    body = stringResource(R.string.settings_playlists_subtitle),
    caption = stringResource(R.string.settings_playlists_meta, count),
    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
    modifier = modifier,
    focusRequester = focusRequester,
    padding = padding,
    rightFocusRequester = right,
    downFocusRequester = down,
    onRequestSideMenu = onRequestSideMenu,
    onClick = onClick,
)

@Composable
private fun PlayerUiCard(modifier: Modifier, padding: Dp) = SettingsStaticCard(
    title = stringResource(R.string.settings_player_ui_title),
    body = stringResource(R.string.settings_player_ui_value),
    caption = stringResource(R.string.settings_player_ui_caption),
    icon = Icons.Outlined.Tv,
    modifier = modifier,
    padding = padding,
)

@Composable
private fun StartupCard(
    behavior: StartupBehavior,
    modifier: Modifier,
    padding: Dp,
    focusRequester: FocusRequester,
    left: FocusRequester? = null,
    right: FocusRequester? = null,
    up: FocusRequester? = null,
    down: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    onClick: () -> Unit,
) = SettingsActionCard(
    title = stringResource(R.string.settings_startup_title),
    body = stringResource(R.string.settings_startup_subtitle),
    trailingText = behavior.titleText(),
    icon = Icons.Outlined.RestartAlt,
    modifier = modifier,
    focusRequester = focusRequester,
    padding = padding,
    leftFocusRequester = left,
    rightFocusRequester = right,
    upFocusRequester = up,
    downFocusRequester = down,
    onRequestSideMenu = onRequestSideMenu,
    onClick = onClick,
)

@Composable
private fun PrivacyCard(
    modifier: Modifier,
    padding: Dp,
    focusRequester: FocusRequester,
    left: FocusRequester? = null,
    right: FocusRequester? = null,
    up: FocusRequester? = null,
    down: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    onShowPrivacy: () -> Unit,
) = SettingsActionCard(
    title = stringResource(R.string.settings_privacy_title),
    body = stringResource(R.string.settings_privacy_body),
    icon = Icons.Outlined.Shield,
    modifier = modifier,
    focusRequester = focusRequester,
    padding = padding,
    leftFocusRequester = left,
    rightFocusRequester = right,
    upFocusRequester = up,
    downFocusRequester = down,
    onRequestSideMenu = onRequestSideMenu,
    onClick = onShowPrivacy,
)

@Composable
private fun AboutCard(
    modifier: Modifier,
    padding: Dp,
    focusRequester: FocusRequester,
    left: FocusRequester? = null,
    up: FocusRequester? = null,
    onRequestSideMenu: (() -> Unit)? = null,
    onClick: () -> Unit,
) = SettingsActionCard(
    title = stringResource(R.string.settings_about_title),
    body = stringResource(R.string.settings_about_subtitle, BuildConfig.VERSION_NAME),
    icon = Icons.Outlined.Info,
    modifier = modifier,
    focusRequester = focusRequester,
    padding = padding,
    leftFocusRequester = left,
    upFocusRequester = up,
    onRequestSideMenu = onRequestSideMenu,
    onClick = onClick,
)

internal enum class SettingsLayout(val cardPadding: Dp) {
    Expanded(16.dp),
    Medium(14.dp),
    Compact(12.dp),
}
