package com.github.damontecres.wholphin.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowConfig
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.ui.abbreviateNumber
import com.github.damontecres.wholphin.ui.formatDate
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

val HomeRowLoadingState?.isWatchHistory: Boolean
    get() = (this as? HomeRowLoadingState.Success)?.rowType is HomeRowConfig.WatchHistory

/**
 * The top-right corner badge text for a home-row card: the unplayed-episode count when
 * [viewOptions] opts in, else the normal episode-number/unplayed fallback already computed on
 * [BaseItem.ui]. Episode cards look their series up in [seriesUnplayedCounts] (an episode doesn't
 * carry its series' aggregate count itself); Season/Series cards (eg a grouped "Recently Added"
 * entry) already carry their own count and need no lookup.
 */
fun unplayedCornerText(
    item: BaseItem?,
    viewOptions: HomeRowViewOptions,
    seriesUnplayedCounts: Map<UUID, Int>,
): String? {
    if (!viewOptions.showUnplayedCount) return item?.ui?.episodeUnplayedCornerText
    val unplayedCount =
        when (item?.type) {
            BaseItemKind.EPISODE -> item.data.seriesId?.let { seriesUnplayedCounts[it] }
            BaseItemKind.SEASON, BaseItemKind.SERIES -> item.data.userData?.unplayedItemCount
            else -> null
        }
    return if (unplayedCount != null) abbreviateNumber(unplayedCount) else item?.ui?.episodeUnplayedCornerText
}

/**
 * Home-row cards show the show's name, not a grouped Season item's own "Season N" name (Jellyfin's
 * grouped "Recently Added" can return a Season instead of an Episode for shows with many new
 * episodes).
 */
fun homeRowTitle(item: BaseItem?): String? =
    if (item?.type == BaseItemKind.SEASON) item.data.seriesName ?: item.title else item?.title

/** The per-card caption's second line: season/episode info, not the season's release year. */
fun homeRowSubtitle(item: BaseItem?): String? =
    if (item?.type == BaseItemKind.SEASON) item.name else item?.subtitle

/** The "Last played: <date>" text shown in the focused header, only for the Play History row. */
fun watchHistoryLastPlayedText(
    item: BaseItem?,
    isWatchHistoryRow: Boolean,
): String? = if (isWatchHistoryRow) item?.data?.userData?.lastPlayedDate?.let { formatDate(it) } else null

@Composable
fun LastPlayedLine(text: String?) {
    if (text != null) {
        Text(
            text = stringResource(R.string.last_played) + ": " + text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
