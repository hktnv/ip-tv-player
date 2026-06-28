package com.evomrdm.iptvbox

internal enum class NavigationDrawerState {
    Collapsed,
    ExpandedByUserNavigation;

    val expanded: Boolean
        get() = this == ExpandedByUserNavigation

    companion object {
        fun fromExpanded(expanded: Boolean): NavigationDrawerState {
            return if (expanded) ExpandedByUserNavigation else Collapsed
        }
    }
}
