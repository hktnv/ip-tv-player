package com.evomrdm.iptvbox

internal data class NavigationDrawerModel(
    val state: NavigationDrawerState = NavigationDrawerState.Collapsed,
    val focusExpansion: NavigationDrawerFocusExpansion = NavigationDrawerFocusExpansion.Enabled,
)

internal enum class NavigationDrawerState {
    Collapsed,
    ExpandedByUserNavigation;

    val expanded: Boolean
        get() = this == ExpandedByUserNavigation

}

internal enum class NavigationDrawerFocusExpansion {
    Enabled,
    BlockedAfterNavigation,
}

internal enum class NavigationDrawerEvent {
    OpenByUserNavigation,
    DrawerFocused,
    CollapseForNavigation,
    CollapseForContentFocus,
    ContentFocusRestored,
}

internal data class NavigationDrawerTransition(
    val model: NavigationDrawerModel,
    val widthChanged: Boolean,
)

internal fun NavigationDrawerModel.reduce(event: NavigationDrawerEvent): NavigationDrawerTransition {
    val next = when (event) {
        NavigationDrawerEvent.OpenByUserNavigation -> copy(
            state = NavigationDrawerState.ExpandedByUserNavigation,
            focusExpansion = NavigationDrawerFocusExpansion.Enabled,
        )
        NavigationDrawerEvent.DrawerFocused -> this
        NavigationDrawerEvent.CollapseForNavigation -> copy(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.BlockedAfterNavigation,
        )
        NavigationDrawerEvent.CollapseForContentFocus -> copy(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.BlockedAfterNavigation,
        )
        NavigationDrawerEvent.ContentFocusRestored -> copy(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.Enabled,
        )
    }
    return NavigationDrawerTransition(
        model = next,
        widthChanged = next.state != state,
    )
}

internal fun countDrawerWidthTransitions(
    initial: NavigationDrawerModel,
    events: List<NavigationDrawerEvent>,
): Int {
    var model = initial
    var count = 0
    events.forEach { event ->
        val transition = model.reduce(event)
        if (transition.widthChanged) count += 1
        model = transition.model
    }
    return count
}

internal fun shouldExpandCollapsedDrawerOnFocus(
    nowMs: Long,
    lastUserLeftIntentMs: Long,
    thresholdMs: Long = 450L,
): Boolean {
    return lastUserLeftIntentMs > 0L && nowMs - lastUserLeftIntentMs in 0L..thresholdMs
}
