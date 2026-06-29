package com.hktnv.iptvbox

import org.junit.Assert.assertEquals
import org.junit.Test
import com.hktnv.iptvbox.navigation.countDrawerWidthTransitions
import com.hktnv.iptvbox.navigation.consumeUserLeftIntentAfterDrawerEvent
import com.hktnv.iptvbox.navigation.NavigationDrawerEvent
import com.hktnv.iptvbox.navigation.NavigationDrawerFocusExpansion
import com.hktnv.iptvbox.navigation.NavigationDrawerModel
import com.hktnv.iptvbox.navigation.NavigationDrawerState
import com.hktnv.iptvbox.navigation.reduce
import com.hktnv.iptvbox.navigation.shouldHandleSeriesBack
import com.hktnv.iptvbox.navigation.shouldExpandCollapsedDrawerOnFocus
import com.hktnv.iptvbox.navigation.shouldReturnToCatalogCategories
import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab

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
    fun menuNavigationConsumesStaleLeftIntentAndCannotFlickerOpen() {
        val initial = NavigationDrawerModel(
            state = NavigationDrawerState.ExpandedByUserNavigation,
        )
        var staleLeftIntentAt = 1_000L
        val events = mutableListOf(
            NavigationDrawerEvent.CollapseForNavigation,
            NavigationDrawerEvent.CollapseForContentFocus,
            NavigationDrawerEvent.ContentFocusRestored,
        )
        events.forEach { event ->
            staleLeftIntentAt = consumeUserLeftIntentAfterDrawerEvent(staleLeftIntentAt, event)
        }
        val modelAfterFocusRestore = events.fold(initial) { model, event -> model.reduce(event).model }
        val focusEvent = if (shouldExpandCollapsedDrawerOnFocus(
                nowMs = 1_300L,
                lastUserLeftIntentMs = staleLeftIntentAt,
                focusExpansion = modelAfterFocusRestore.focusExpansion,
            )
        ) {
            NavigationDrawerEvent.OpenByUserNavigation
        } else {
            NavigationDrawerEvent.DrawerFocused
        }
        val fullSequence = events + focusEvent

        assertEquals(0L, staleLeftIntentAt)
        assertEquals(1, countDrawerWidthTransitions(initial, fullSequence))
        assertEquals(
            NavigationDrawerState.Collapsed,
            fullSequence.fold(initial) { model, event -> model.reduce(event).model }.state,
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
        assertEquals(
            false,
            shouldExpandCollapsedDrawerOnFocus(
                nowMs = 1_200L,
                lastUserLeftIntentMs = 1_000L,
                focusExpansion = NavigationDrawerFocusExpansion.BlockedAfterNavigation,
            ),
        )
    }

    @Test
    fun firstLeftAfterReturningFromExpandedDrawerStillOpensMenu() {
        assertEquals(true, shouldExpandCollapsedDrawerOnFocus(nowMs = 5_950L, lastUserLeftIntentMs = 4_700L))
    }

    @Test
    fun catalogContentBackReturnsToCategoriesBeforeDrawer() {
        assertEquals(
            true,
            shouldReturnToCatalogCategories(
                screen = AppScreen.CATALOG,
                showCategoryLanding = false,
                selectedSeriesTitle = null,
                selectedSeasonNumber = null,
            ),
        )
        assertEquals(
            false,
            shouldReturnToCatalogCategories(
                screen = AppScreen.CATALOG,
                showCategoryLanding = true,
                selectedSeriesTitle = null,
                selectedSeasonNumber = null,
            ),
        )
    }

    @Test
    fun seriesDetailBackIsSeparateFromCategoryLandingBack() {
        assertEquals(
            true,
            shouldHandleSeriesBack(
                screen = AppScreen.CATALOG,
                selectedTab = CatalogTab.SERIES,
                showCategoryLanding = false,
                selectedSeriesTitle = "Prens",
                selectedSeasonNumber = null,
            ),
        )
        assertEquals(
            false,
            shouldHandleSeriesBack(
                screen = AppScreen.CATALOG,
                selectedTab = CatalogTab.SERIES,
                showCategoryLanding = true,
                selectedSeriesTitle = "Prens",
                selectedSeasonNumber = null,
            ),
        )
    }
}
