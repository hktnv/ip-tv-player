package com.hktnv.iptvbox.player

import com.hktnv.iptvbox.core.common.SearchNormalizer
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
    val totalItemCount: Int = items.size,
) {
    val hasContent: Boolean = items.isNotEmpty() || options.size > 1
    val hasMoreItems: Boolean = items.size < totalItemCount
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
    val visibleItems = items.take(itemLimit)
    return PlayerRelatedContentModel(
        options = options.markSelected(optionId),
        items = visibleItems,
        totalItemCount = items.size,
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
    val categoriesByKey = linkedMapOf<String, String>()
    compatibleItems.forEach { item ->
        val category = item.categoryLabel()
        categoriesByKey.putIfAbsent(category.categoryKey(), category)
    }
    val categories = categoriesByKey.values.toList()
    if (categories.isEmpty()) return PlayerRelatedContentModel(options = emptyList(), items = emptyList())

    val currentCategory = currentItem.categoryLabel()
    val selectedCategory = selectedOptionId?.removePrefix(CategoryOptionPrefix)
        ?.let(categoriesByKey::get)
        ?: categoriesByKey[currentCategory.categoryKey()]
        ?: categories.first()
    val selectedCategoryKey = selectedCategory.categoryKey()
    val optionId = categoryOptionId(selectedCategory)
    val options = categories.map { category ->
        PlayerRelatedContentOption(
            id = categoryOptionId(category),
            label = category,
            selected = category == selectedCategory,
        )
    }
    val items = compatibleItems
        .filter { it.categoryLabel().categoryKey() == selectedCategoryKey && it.id != currentItem.id }
        .sortedWith(compareBy<CatalogItem> { it.providerOrder }.thenBy { it.title })
    val visibleItems = items.take(itemLimit)
    return PlayerRelatedContentModel(
        options = options.markSelected(optionId),
        items = visibleItems,
        totalItemCount = items.size,
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
    return category?.trim()?.takeIf { it.isNotBlank() } ?: DefaultCategoryLabel
}

private fun String.categoryKey(): String {
    return SearchNormalizer.normalize(this).ifBlank { trim().lowercase() }
}

private fun seasonOptionId(season: Int): String = "$SeasonOptionPrefix$season"

private fun categoryOptionId(category: String): String = "$CategoryOptionPrefix${category.categoryKey()}"

private const val RelatedContentItemLimit = 16
private const val SeasonOptionPrefix = "season:"
private const val CategoryOptionPrefix = "category:"
private const val DefaultCategoryLabel = "Genel"
