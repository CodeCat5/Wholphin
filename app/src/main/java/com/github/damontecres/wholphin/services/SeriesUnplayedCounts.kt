package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID

/**
 * Looks up how many episodes are unplayed for each series backing the given episode [items], by
 * batching a single `/Items?ids=...` request for their distinct series IDs. Returns empty unless
 * [viewOptions] opts into showing the count, or if [userId] is unknown, so callers can pass
 * straight through without checking either themselves.
 *
 * Used to show an unplayed-episode-count badge on episode cards (eg Continue Watching, Next Up,
 * Recently Added) instead of the specific episode number, since an episode's own [BaseItem] does
 * not carry its series' aggregate unplayed count.
 */
suspend fun fetchSeriesUnplayedCounts(
    api: ApiClient,
    viewOptions: HomeRowViewOptions,
    userId: UUID?,
    items: List<BaseItem?>,
): Map<UUID, Int> {
    if (!viewOptions.showUnplayedCount || userId == null) return emptyMap()
    val seriesIds =
        items
            .filterNotNull()
            .filter { it.type == BaseItemKind.EPISODE }
            .mapNotNull { it.data.seriesId }
            .distinct()
    if (seriesIds.isEmpty()) return emptyMap()
    return try {
        api.itemsApi
            .getItems(
                GetItemsRequest(
                    userId = userId,
                    ids = seriesIds,
                    enableUserData = true,
                ),
            ).content.items
            .associate { it.id to (it.userData?.unplayedItemCount ?: 0) }
    } catch (ex: Exception) {
        Timber.e(ex, "Failed fetching series unplayed counts")
        emptyMap()
    }
}
