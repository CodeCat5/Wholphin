package com.github.damontecres.wholphin.ui.playback

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.cards.SeasonCard
import com.github.damontecres.wholphin.ui.ifElse

/**
 * A horizontal row of Continue Watching + Next Up items (across every library) shown below the
 * Next Up card, mirroring Plex's "On Deck" row on its post-episode screen.
 */
@Composable
fun OnDeckRow(
    items: List<BaseItem>,
    onClickItem: (BaseItem) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.continue_watching),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .focusRestorer(focusRequester)
                    .onFocusChanged { onFocusChanged(it.hasFocus) },
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused = interactionSource.collectIsFocusedAsState().value
                LaunchedEffect(isFocused) {
                    if (isFocused) onFocusChanged(true)
                }
                SeasonCard(
                    item = item,
                    onClick = { onClickItem(item) },
                    onLongClick = {},
                    imageHeight = 100.dp,
                    showImageOverlay = true,
                    interactionSource = interactionSource,
                    modifier =
                        Modifier.ifElse(
                            index == 0,
                            Modifier.focusRequester(focusRequester),
                        ),
                )
            }
        }
    }
}
