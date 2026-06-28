package com.hktnv.iptvbox

import org.junit.Assert.assertEquals
import org.junit.Test
import com.hktnv.iptvbox.navigation.countDrawerWidthTransitions
import com.hktnv.iptvbox.navigation.NavigationDrawerEvent
import com.hktnv.iptvbox.navigation.NavigationDrawerFocusExpansion
import com.hktnv.iptvbox.navigation.NavigationDrawerModel
import com.hktnv.iptvbox.navigation.NavigationDrawerState
import com.hktnv.iptvbox.navigation.reduce
import com.hktnv.iptvbox.navigation.shouldExpandCollapsedDrawerOnFocus

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
    fun drawerFocusNeverChangesWidthWithoutExplicitUserNavigation() {
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
        assertEquals(0, countDrawerWidthTransitions(enabled, listOf(NavigationDrawerEvent.DrawerFocused)))
        assertEquals(
            NavigationDrawerState.Collapsed,
            enabled.reduce(NavigationDrawerEvent.DrawerFocused).model.state,
        )
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
            NavigationDrawerEvent.OpenByUserNavigation,
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

    @Test
    fun categoryFocusLeakCannotOpenCollapsedDrawer() {
        val initial = NavigationDrawerModel(
            state = NavigationDrawerState.Collapsed,
            focusExpansion = NavigationDrawerFocusExpansion.Enabled,
        )
        val events = listOf(
            NavigationDrawerEvent.ContentFocusRestored,
            NavigationDrawerEvent.DrawerFocused,
            NavigationDrawerEvent.DrawerFocused,
        )

        assertEquals(0, countDrawerWidthTransitions(initial, events))
        assertEquals(
            NavigationDrawerState.Collapsed,
            events.fold(initial) { model, event -> model.reduce(event).model }.state,
        )
    }

    @Test
    fun collapsedDrawerFocusExpandsOnlyAfterFreshLeftIntent() {
        assertEquals(true, shouldExpandCollapsedDrawerOnFocus(nowMs = 1_200L, lastUserLeftIntentMs = 1_000L))
        assertEquals(true, shouldExpandCollapsedDrawerOnFocus(nowMs = 2_300L, lastUserLeftIntentMs = 1_000L))
        assertEquals(false, shouldExpandCollapsedDrawerOnFocus(nowMs = 2_500L, lastUserLeftIntentMs = 1_000L))
        assertEquals(false, shouldExpandCollapsedDrawerOnFocus(nowMs = 1_000L, lastUserLeftIntentMs = 0L))
    }

    @Test
    fun firstLeftAfterReturningFromExpandedDrawerStillOpensMenu() {
        assertEquals(true, shouldExpandCollapsedDrawerOnFocus(nowMs = 5_950L, lastUserLeftIntentMs = 4_700L))
    }
}
