package com.hktnv.iptvbox.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hktnv.iptvbox.core.designsystem.IptvColors
import com.hktnv.iptvbox.ui.common.TvFocusBorder
import com.hktnv.iptvbox.ui.common.TvFocusPanel
import com.hktnv.iptvbox.ui.common.TvRestingBorder
import com.hktnv.iptvbox.ui.common.tvClickable
import com.hktnv.iptvbox.ui.common.tvFocusElevation
import com.hktnv.iptvbox.ui.common.tvFocusLift

@Composable
internal fun SearchControls(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    compact: Boolean,
    television: Boolean,
    queryFocused: Boolean,
    keyboardRequested: Boolean,
    keyboardActivationReady: Boolean,
    hasResults: Boolean,
    inputFocusRequester: FocusRequester,
    searchButtonFocusRequester: FocusRequester,
    firstResultFocusRequester: FocusRequester,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    onQueryFocusChanged: (Boolean) -> Unit,
    onKeyboardRequestedChange: (Boolean) -> Unit,
    onRequestSideMenu: () -> Unit,
) {
    if (compact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchQueryField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                inputFocusRequester = inputFocusRequester,
                searchButtonFocusRequester = searchButtonFocusRequester,
                firstResultFocusRequester = firstResultFocusRequester,
                television = television,
                queryFocused = queryFocused,
                keyboardRequested = keyboardRequested,
                keyboardActivationReady = keyboardActivationReady,
                hasResults = hasResults,
                focusManager = focusManager,
                keyboardController = keyboardController,
                onQueryFocusChanged = onQueryFocusChanged,
                onKeyboardRequestedChange = onKeyboardRequestedChange,
                onRequestSideMenu = onRequestSideMenu,
            )
            SearchSubmitButton(
                enabled = query.isNotBlank(),
                focusRequester = searchButtonFocusRequester,
                onSearch = onSearch,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchQueryField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.weight(1f),
                inputFocusRequester = inputFocusRequester,
                searchButtonFocusRequester = searchButtonFocusRequester,
                firstResultFocusRequester = firstResultFocusRequester,
                television = television,
                queryFocused = queryFocused,
                keyboardRequested = keyboardRequested,
                keyboardActivationReady = keyboardActivationReady,
                hasResults = hasResults,
                focusManager = focusManager,
                keyboardController = keyboardController,
                onQueryFocusChanged = onQueryFocusChanged,
                onKeyboardRequestedChange = onKeyboardRequestedChange,
                onRequestSideMenu = onRequestSideMenu,
            )
            SearchSubmitButton(
                enabled = query.isNotBlank(),
                focusRequester = searchButtonFocusRequester,
                onSearch = onSearch,
            )
        }
    }
}

@Composable
private fun SearchQueryField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier,
    inputFocusRequester: FocusRequester,
    searchButtonFocusRequester: FocusRequester,
    firstResultFocusRequester: FocusRequester,
    television: Boolean,
    queryFocused: Boolean,
    keyboardRequested: Boolean,
    keyboardActivationReady: Boolean,
    hasResults: Boolean,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    onQueryFocusChanged: (Boolean) -> Unit,
    onKeyboardRequestedChange: (Boolean) -> Unit,
    onRequestSideMenu: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .focusRequester(inputFocusRequester)
            .focusProperties {
                right = searchButtonFocusRequester
                down = firstResultFocusRequester
            }
            .onFocusChanged { onQueryFocusChanged(it.hasFocus) }
            .onPreviewKeyEvent { event ->
                if (television && queryFocused && event.type == KeyEventType.KeyDown) {
                    handleSearchInputKey(
                        eventKey = event.key,
                        query = query,
                        keyboardActivationReady = keyboardActivationReady,
                        hasResults = hasResults,
                        searchButtonFocusRequester = searchButtonFocusRequester,
                        firstResultFocusRequester = firstResultFocusRequester,
                        focusManager = focusManager,
                        keyboardController = keyboardController,
                        onKeyboardRequestedChange = onKeyboardRequestedChange,
                        onRequestSideMenu = onRequestSideMenu,
                    )
                } else {
                    false
                }
            },
        label = { Text("Kanal, film, dizi veya kategori ara") },
        readOnly = television && !keyboardRequested,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(showKeyboardOnFocus = !television || keyboardRequested),
    )
}

private fun handleSearchInputKey(
    eventKey: Key,
    query: String,
    keyboardActivationReady: Boolean,
    hasResults: Boolean,
    searchButtonFocusRequester: FocusRequester,
    firstResultFocusRequester: FocusRequester,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    onKeyboardRequestedChange: (Boolean) -> Unit,
    onRequestSideMenu: () -> Unit,
): Boolean {
    return when (eventKey) {
        Key.DirectionRight -> {
            runCatching { searchButtonFocusRequester.requestFocus() }.getOrDefault(false)
        }
        Key.DirectionDown -> {
            onKeyboardRequestedChange(false)
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            if (hasResults) {
                runCatching { firstResultFocusRequester.requestFocus() }.getOrDefault(false)
            } else {
                false
            }
        }
        Key.DirectionLeft -> {
            if (query.isBlank()) {
                onKeyboardRequestedChange(false)
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
                onRequestSideMenu()
                true
            } else {
                false
            }
        }
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter -> {
            if (keyboardActivationReady) onKeyboardRequestedChange(true)
            true
        }
        else -> false
    }
}

@Composable
private fun SearchSubmitButton(
    enabled: Boolean,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val compact = LocalConfiguration.current.screenWidthDp < 420
    val panelColor = when {
        focused -> TvFocusPanel
        enabled -> IptvColors.Accent
        else -> Color(0xFF1E2731)
    }
    val borderColor = when {
        focused -> TvFocusBorder
        enabled -> IptvColors.Accent.copy(alpha = 0.62f)
        else -> TvRestingBorder
    }
    Surface(
        modifier = modifier
            .height(56.dp)
            .widthIn(min = if (compact) 76.dp else 104.dp)
            .focusRequester(focusRequester)
            .tvFocusLift(focused = focused, scale = 1.035f, liftPx = -4f)
            .onFocusChanged { focused = it.isFocused }
            .tvClickable(enabled = enabled, onClick = onSearch),
        color = panelColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
        shadowElevation = tvFocusElevation(focused = focused, resting = 1.dp, focusedElevation = 14.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Ara",
                color = if (enabled || focused) IptvColors.TextPrimary else IptvColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = if (compact) 14.sp else 15.sp,
            )
        }
    }
}
