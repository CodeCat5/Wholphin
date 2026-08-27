package com.github.damontecres.wholphin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.SlimItemFields
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import java.time.LocalDateTime
import java.util.UUID

private fun getRecommendedRows(parentId: UUID) =
    listOf(
        RecommendedRow.combinedContinueWatchingNextUp(dedupeBySeries = true),
        RecommendedRow(
            title = R.string.recently_released,
            handler = GetItemsRequestHandler,
            request =
                GetItemsRequest(
                    parentId = parentId,
                    fields = SlimItemFields,
                    includeItemTypes = listOf(BaseItemKind.EPISODE),
                    recursive = true,
                    enableUserData = true,
                    sortBy =
                        listOf(
                            ItemSortBy.PREMIERE_DATE,
                            ItemSortBy.SERIES_SORT_NAME,
                            ItemSortBy.AIRED_EPISODE_ORDER,
                        ),
                    sortOrder =
                        listOf(
                            SortOrder.DESCENDING,
                            SortOrder.ASCENDING,
                            SortOrder.DESCENDING,
                        ),
                    enableTotalRecordCount = false,
                    maxPremiereDate = LocalDateTime.now(),
                    isUnaired = false,
                ),
            dedupeBySeries = true,
        ),
        RecommendedRow(
            title = R.string.recently_added,
            handler = GetItemsRequestHandler,
            request =
                GetItemsRequest(
                    parentId = parentId,
                    fields = SlimItemFields,
                    includeItemTypes = listOf(BaseItemKind.EPISODE),
                    recursive = true,
                    enableUserData = true,
                    sortBy = listOf(ItemSortBy.DATE_CREATED),
                    sortOrder = listOf(SortOrder.DESCENDING),
                    enableTotalRecordCount = false,
                ),
            dedupeBySeries = true,
        ),
        RecommendedRow(
            title = R.string.top_unwatched,
            handler = GetItemsRequestHandler,
            request =
                GetItemsRequest(
                    parentId = parentId,
                    fields = SlimItemFields,
                    includeItemTypes = listOf(BaseItemKind.SERIES),
                    recursive = true,
                    enableUserData = true,
                    isPlayed = false,
                    sortBy = listOf(ItemSortBy.COMMUNITY_RATING),
                    sortOrder = listOf(SortOrder.DESCENDING),
                    enableTotalRecordCount = false,
                ),
        ),
    )

private fun watchHistoryRow(parentId: UUID) =
    RecommendedRow(
        title = R.string.watch_history,
        handler = GetItemsRequestHandler,
        request =
            GetItemsRequest(
                parentId = parentId,
                fields = SlimItemFields,
                includeItemTypes = listOf(BaseItemKind.EPISODE),
                recursive = true,
                enableUserData = true,
                isPlayed = true,
                sortBy = listOf(ItemSortBy.DATE_PLAYED),
                sortOrder = listOf(SortOrder.DESCENDING),
                enableTotalRecordCount = false,
            ),
    )

/**
 * The "recommended" tab of a TV show library
 */
@Composable
fun RecommendedTvShow(
    preferences: UserPreferences,
    parentId: UUID,
    onFocusPosition: (RowColumn) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecommendedViewModel =
        hiltViewModel<RecommendedViewModel, RecommendedViewModel.Factory>(
            creationCallback = {
                it.create(
                    parentId = parentId,
                    suggestionsType = BaseItemKind.SERIES,
                    recommendedRows = getRecommendedRows(parentId),
                    viewOptions = HomeRowViewOptions(),
                    watchHistoryRow = watchHistoryRow(parentId),
                )
            },
        ),
) {
    RecommendedContent(
        preferences = preferences,
        viewModel = viewModel,
        onFocusPosition = onFocusPosition,
        modifier = modifier,
    )
}
