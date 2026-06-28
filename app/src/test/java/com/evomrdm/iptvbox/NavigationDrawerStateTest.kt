package com.evomrdm.iptvbox

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationDrawerStateTest {
    @Test
    fun menuSelectionCollapsesOnceAndFocusCannotReopenDuringNavigation() {
        val initial = NavigationDrawerModel(
            state = NavigationDrawerState.ExpandedByUserNavigation,
        )
        val events = listOf(
            NavigationDrawerEvent.CollapseForNavigation,
            NavigationDrawerEvent.DrawerFocused,
            NavigationDrawerEvent.DrawerFocused,
        )

        assertEquals(1, countDrawerWidthTransitions(initial, events))
        assertEquals(
            NavigationDrawerState.Collapsed,
            events.fold(initial) { model, event -> model.reduce(event).model }.state,
        )
    }

    @Test
    fun drawerFocusOpensOnlyWhenFocusExpansionIsEnabled() {
        val blocked = NavigationDrawerModel(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.BlockedAfterNavigation,
        )
        val enabled = NavigationDrawerModel(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.Enabled,
        )

        assertEquals(0, countDrawerWidthTransitions(blocked, listOf(NavigationDrawerEvent.DrawerFocused)))
        assertEquals(0, countDrawerWidthTransitions(
            enabled,
            listOf(NavigationDrawerEvent.CollapseForContentFocus, NavigationDrawerEvent.DrawerFocused),
        ))
        assertEquals(1, countDrawerWidthTransitions(enabled, listOf(NavigationDrawerEvent.DrawerFocused)))
    }

    @Test
    fun contentFocusRestoredAllowsTheNextUserDrawerEntry() {
        val initial = NavigationDrawerModel(
            state = NavigationDrawerState.ExpandedByUserNavigation,
        )
        val events = listOf(
            NavigationDrawerEvent.CollapseForNavigation,
            NavigationDrawerEvent.DrawerFocused,
            NavigationDrawerEvent.ContentFocusRestored,
            NavigationDrawerEvent.DrawerFocused,
        )

        assertEquals(2, countDrawerWidthTransitions(initial, events))
        assertEquals(
            NavigationDrawerState.ExpandedByUserNavigation,
            events.fold(initial) { model, event -> model.reduce(event).model }.state,
        )
    }

    @Test
    fun explicitUserDrawerEntryExpandsEvenAfterNavigationGuard() {
        val blocked = NavigationDrawerModel(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.BlockedAfterNavigation,
        )

        assertEquals(1, countDrawerWidthTransitions(blocked, listOf(NavigationDrawerEvent.OpenByUserNavigation)))
        assertEquals(
            NavigationDrawerState.ExpandedByUserNavigation,
            blocked.reduce(NavigationDrawerEvent.OpenByUserNavigation).model.state,
        )
    }
}
