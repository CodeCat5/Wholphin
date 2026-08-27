package com.github.damontecres.wholphin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.UnavailabilityReason
import com.github.damontecres.wholphin.data.model.unavailabilityReason
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.Cards
import com.github.damontecres.wholphin.ui.OneTimeLaunchedEffect
import com.github.damontecres.wholphin.ui.SlimItemFields
import com.github.damontecres.wholphin.ui.cards.BannerCard
import com.github.damontecres.wholphin.ui.cards.ItemRow
import com.github.damontecres.wholphin.ui.cards.SeasonCard
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialog
import com.github.damontecres.wholphin.ui.data.ItemDetailsDialogInfo
import com.github.damontecres.wholphin.ui.formatDate
import com.github.damontecres.wholphin.ui.formatDateTime
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.DataLoadingState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.model.api.request.GetUpcomingEpisodesRequest
import timber.log.Timber
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@HiltViewModel(assistedFactory = UpcomingViewModel.Factory::class)
class UpcomingViewModel
    @AssistedInject
    constructor(
        private val api: ApiClient,
        @Assisted private val parentId: UUID,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(parentId: UUID): UpcomingViewModel
        }

        private val _state =
            MutableStateFlow<DataLoadingState<List<UpcomingDayGroup>>>(DataLoadingState.Pending)
        val state: StateFlow<DataLoadingState<List<UpcomingDayGroup>>> = _state

        fun init() {
            _state.update { DataLoadingState.Loading }
            viewModelScope.launchIO {
                try {
                    val items =
                        api.tvShowsApi
                            .getUpcomingEpisodes(
                                GetUpcomingEpisodesRequest(
                                    parentId = parentId,
                                    fields = SlimItemFields,
                                    enableUserData = true,
                                    limit = 300,
                                ),
                            ).content.items
                            .map { BaseItem(it, useSeriesForPrimary = true) }
                    val groups =
                        items
                            .filter { it.data.premiereDate != null }
                            .groupBy { it.data.premiereDate!!.toLocalDate() }
                            .toSortedMap()
                            .map { (date, groupItems) -> UpcomingDayGroup(date, groupItems) }
                    _state.update { DataLoadingState.Success(groups) }
                } catch (ex: Exception) {
                    Timber.e(ex, "Error fetching upcoming episodes for parent %s", parentId)
                    _state.update { DataLoadingState.Error(ex) }
                }
            }
        }
    }

data class UpcomingDayGroup(
    val date: LocalDate,
    val items: List<BaseItem>,
)

@Composable
private fun dayHeader(date: LocalDate): String {
    val today = remember { LocalDate.now() }
    return when (date) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " + formatDate(date)
    }
}

/**
 * Card for a single upcoming/unaired episode, shown as a row on a show's own details page.
 *
 * Kept here alongside the rest of the Upcoming feature so pages that use it (e.g. [SeriesDetailsContent])
 * only need one small, additive call site rather than duplicating this rendering logic inline.
 */
@Composable
fun UpcomingEpisodeCard(
    item: BaseItem?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unairedLabel = stringResource(R.string.unaired)
    val missingLabel = stringResource(R.string.missing)
    BannerCard(
        name = item?.name,
        item = item,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        cornerText =
            when (item?.unavailabilityReason) {
                UnavailabilityReason.MISSING -> missingLabel
                UnavailabilityReason.UNAIRED -> unairedLabel
                null -> item?.data?.premiereDate?.let(::formatDateTime) ?: item?.ui?.episodeCornerText
            },
        aspectRatio = item?.aspectRatio ?: AspectRatios.WIDE,
        cardHeight = Cards.heightEpisode,
        useSeriesForPrimary = false,
    )
}

/**
 * The "upcoming" tab of a TV show library: unaired episodes grouped by air date, similar to
 * Jellyfin web's Upcoming section
 */
@Composable
fun UpcomingTvShow(
    parentId: UUID,
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel =
        hiltViewModel<UpcomingViewModel, UpcomingViewModel.Factory>(
            creationCallback = { it.create(parentId) },
        ),
) {
    OneTimeLaunchedEffect { viewModel.init() }
    val state by viewModel.state.collectAsState()
    var overviewDialog by remember { mutableStateOf<ItemDetailsDialogInfo?>(null) }

    when (val st = state) {
        DataLoadingState.Pending,
        DataLoadingState.Loading,
        -> LoadingPage(modifier)

        is DataLoadingState.Error -> ErrorMessage(st, modifier)

        is DataLoadingState.Success -> {
            if (st.data.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = modifier.fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(R.string.no_results),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val firstRowFocusRequester = remember { FocusRequester() }
                LaunchedEffect(st.data) {
                    firstRowFocusRequester.tryRequestFocus()
                }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = modifier.fillMaxSize(),
                ) {
                    itemsIndexed(st.data, key = { _, group -> group.date.toString() }) { index, group ->
                        ItemRow(
                            title = dayHeader(group.date),
                            items = group.items,
                            onClickItem = { _, item ->
                                overviewDialog = ItemDetailsDialogInfo(item)
                            },
                            onLongClickItem = { _, _ -> },
                            cardContent = { _, item, mod, onClick, onLongClick ->
                                val unairedLabel = stringResource(R.string.unaired)
                                val missingLabel = stringResource(R.string.missing)
                                SeasonCard(
                                    item = item,
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                    modifier = mod,
                                    showImageOverlay = true,
                                    imageHeight = Cards.height2x3,
                                    imageWidth = Dp.Unspecified,
                                    badgeText =
                                        when (item?.unavailabilityReason) {
                                            UnavailabilityReason.MISSING -> missingLabel
                                            UnavailabilityReason.UNAIRED -> unairedLabel
                                            null -> null
                                        },
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .let { if (index == 0) it.focusRequester(firstRowFocusRequester) else it },
                        )
                    }
                }
            }
        }
    }
    overviewDialog?.let { info ->
        ItemDetailsDialog(
            info = info,
            showFilePath = false,
            onDismissRequest = { overviewDialog = null },
        )
    }
}
