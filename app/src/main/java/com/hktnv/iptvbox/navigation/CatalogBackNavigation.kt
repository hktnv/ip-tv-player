package com.hktnv.iptvbox.navigation

import com.hktnv.iptvbox.model.AppScreen
import com.hktnv.iptvbox.model.CatalogTab

internal fun shouldReturnToCatalogCategories(
    screen: AppScreen,
    showCategoryLanding: Boolean,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
): Boolean {
    return screen == AppScreen.CATALOG &&
        !showCategoryLanding &&
        selectedSeriesTitle == null &&
        selectedSeasonNumber == null
}

internal fun shouldHandleSeriesBack(
    screen: AppScreen,
    selectedTab: CatalogTab,
    showCategoryLanding: Boolean,
    selectedSeriesTitle: String?,
    selectedSeasonNumber: Int?,
): Boolean {
    return screen == AppScreen.CATALOG &&
        selectedTab == CatalogTab.SERIES &&
        !showCategoryLanding &&
        (selectedSeasonNumber != null || selectedSeriesTitle != null)
}
