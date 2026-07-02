package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.model.CatalogItem
import com.hktnv.iptvbox.core.model.ContentKind

internal data class PlayerRelatedContentOption(
    val id: String,
    val label: String,
    val seasonNumber: Int? = null,
    val selected: Boolean,
)

internal data class PlayerRelatedContentModel(
    val options: List<PlayerRelatedContentOption>,
    val items: List<CatalogItem>,
) {
    val hasContent: Boolean = items.isNotEmpty()
}

internal fun buildPlayerRelatedContentModel(
    currentItem: CatalogItem,
    contextItems: List<CatalogItem>,
    selectedOptionId: String?,
    itemLimit: Int = RelatedContentItemLimit,
): PlayerRelatedContentModel {
    return when (currentItem.kind) {
        ContentKind.EPISODE -> buildEpisodeRelatedContent(
            currentItem = currentItem,
            contextItems = contextItems,
            selectedOptionId = selectedOptionId,
            itemLimit = itemLimit,
        )
        ContentKind.LIVE_CHANNEL,
        ContentKind.RADIO,
        ContentKind.MOVIE -> buildCategoryRelatedContent(
            currentItem = currentItem,
            contextItems = contextItems,
            selectedOptionId = selectedOptionId,
            itemLimit = itemLimit,
        )
        ContentKind.SERIES,
        ContentKind.SEASON -> PlayerRelatedContentModel(options = emptyList(), items = emptyList())
    }
}

private fun buildEpisodeRelatedContent(
    currentItem: CatalogItem,
    contextItems: List<CatalogItem>,
    selectedOptionId: String?,
    itemLimit: Int,
): PlayerRelatedContentModel {
    val seriesKey = currentItem.seriesContextKey()
    val episodes = uniqueContextItems(contextItems, currentItem)
        .filter { it.kind == ContentKind.EPISODE && it.seriesContextKey() == seriesKey }
        .sortedWith(compareBy<CatalogItem> { it.seasonNumber ?: 1 }
            .thenBy { it.episodeNumber ?: Int.MAX_VALUE }
            .thenBy { it.providerOrder }
            .thenBy { it.title })
    val seasons = episodes.map { it.seasonNumber ?: 1 }.distinct()
    if (seasons.isEmpty()) return PlayerRelatedContentModel(options = emptyList(), items = emptyList())

    val currentSeason = currentItem.seasonNumber ?: seasons.first()
    val selectedSeason = selectedOptionId?.removePrefix(SeasonOptionPrefix)?.toIntOrNull()
        ?.takeIf { it in seasons }
        ?: currentSeason.takeIf { it in seasons }
        ?: seasons.first()
    val optionId = seasonOptionId(selectedSeason)
    val options = seasons.map { season ->
        PlayerRelatedContentOption(
            id = seasonOptionId(season),
            label = season.toString(),
            seasonNumber = season,
            selected = season == selectedSeason,
        )
    }
    val items = episodes
        .filter { (it.seasonNumber ?: 1) == selectedSeason && it.id != currentItem.id }
        .take(itemLimit)
    return PlayerRelatedContentModel(
        options = options.markSelected(optionId),
        items = items,
    )
}

private fun buildCategoryRelatedContent(
    currentItem: CatalogItem,
    contextItems: List<CatalogItem>,
    selectedOptionId: String?,
    itemLimit: Int,
): PlayerRelatedContentModel {
    val compatibleItems = uniqueContextItems(contextItems, currentItem)
        .filter { it.kind == currentItem.kind && it.streamUrl.isNotBlank() }
    val categories = compatibleItems
        .map { it.categoryLabel() }
        .distinct()
    if (categories.isEmpty()) return PlayerRelatedContentModel(options = emptyList(), items = emptyList())

    val currentCategory = currentItem.categoryLabel()
    val selectedCategory = selectedOptionId?.removePrefix(CategoryOptionPrefix)
        ?.let { selectedKey -> categories.firstOrNull { it.categoryKey() == selectedKey } }
        ?: currentCategory.takeIf { it in categories }
        ?: categories.first()
    val optionId = categoryOptionId(selectedCategory)
    val options = categories.map { category ->
        PlayerRelatedContentOption(
            id = categoryOptionId(category),
            label = category,
            selected = category == selectedCategory,
        )
    }
    val items = compatibleItems
        .filter { it.categoryLabel() == selectedCategory && it.id != currentItem.id }
        .sortedWith(compareBy<CatalogItem> { it.providerOrder }.thenBy { it.title })
        .take(itemLimit)
    return PlayerRelatedContentModel(
        options = options.markSelected(optionId),
        items = items,
    )
}

private fun uniqueContextItems(
    contextItems: List<CatalogItem>,
    currentItem: CatalogItem,
): List<CatalogItem> {
    val uniqueItems = LinkedHashMap<String, CatalogItem>()
    contextItems.forEach { uniqueItems[it.id] = it }
    uniqueItems[currentItem.id] = currentItem
    return uniqueItems.values.toList()
}

private fun List<PlayerRelatedContentOption>.markSelected(
    selectedId: String,
): List<PlayerRelatedContentOption> {
    return map { option -> option.copy(selected = option.id == selectedId) }
}

private fun CatalogItem.seriesContextKey(): String {
    return seriesTitle.orEmpty()
        .ifBlank { title.substringBefore(" S", title).substringBefore(" Sezon", title) }
        .trim()
        .lowercase()
}

private fun CatalogItem.categoryLabel(): String {
    return category?.trim().orEmpty()
}

private fun String.categoryKey(): String = trim().lowercase()

private fun seasonOptionId(season: Int): String = "$SeasonOptionPrefix$season"

private fun categoryOptionId(category: String): String = "$CategoryOptionPrefix${category.categoryKey()}"

private const val RelatedContentItemLimit = 16
private const val SeasonOptionPrefix = "season:"
private const val CategoryOptionPrefix = "category:"
