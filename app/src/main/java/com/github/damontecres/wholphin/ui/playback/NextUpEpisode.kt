package com.github.damontecres.wholphin.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.surfaceColorAtElevation
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.PreviewTvSpec
import com.github.damontecres.wholphin.ui.cards.SeasonCard
import com.github.damontecres.wholphin.ui.dot
import com.github.damontecres.wholphin.ui.formatDuration
import com.github.damontecres.wholphin.ui.formatTime
import com.github.damontecres.wholphin.ui.theme.WholphinTheme
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.Clock
import com.github.damontecres.wholphin.ui.util.LocalClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Layout for showing the next up episode during playback
 */
@Composable
fun NextUpEpisode(
    title: String?,
    subtitle: String?,
    description: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    timeLeft: Duration?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = AspectRatios.WIDE,
    runtime: Duration? = null,
    justPlayed: BaseItem? = null,
    onClickJustPlayed: () -> Unit = {},
    onDeck: List<BaseItem> = emptyList(),
    onClickOnDeckItem: (BaseItem) -> Unit = {},
    onUpNextFocusChanged: (Boolean) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isUpNextFocused = interactionSource.collectIsFocusedAsState().value
    LaunchedEffect(isUpNextFocused) { onUpNextFocusChanged(isUpNextFocused) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }
    // Navigating down from the Up Next (or Just Played) card should always land on the first
    // Continue Watching item, rather than Compose's default geometric nearest-neighbor guess.
    val onDeckFocusRequester = remember { FocusRequester() }
    Box(
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .focusGroup()
                    .focusRestorer(focusRequester),
        ) {
            Text(
                text = stringResource(R.string.next_up) + "...",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(8.dp),
            ) {
                if (justPlayed != null) {
                    SeasonCard(
                        item = justPlayed,
                        onClick = onClickJustPlayed,
                        onLongClick = {},
                        imageHeight = 90.dp,
                        showImageOverlay = true,
                        modifier =
                            Modifier.focusProperties {
                                down = onDeckFocusRequester
                                left = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                            },
                    )
                }
                val nextUpAspectRatio = aspectRatio.coerceAtLeast(AspectRatios.MIN)
                val nextUpCardHeight = 110.dp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.width(nextUpCardHeight * nextUpAspectRatio),
                ) {
                    NextUpCard(
                        imageUrl = imageUrl,
                        onClick = onClick,
                        timeLeft = timeLeft,
                        interactionSource = interactionSource,
                        modifier =
                            Modifier
                                .height(nextUpCardHeight)
                                .aspectRatio(nextUpAspectRatio)
                                .focusRequester(focusRequester)
                                .focusProperties {
                                    down = onDeckFocusRequester
                                    up = FocusRequester.Cancel
                                    if (justPlayed == null) left = FocusRequester.Cancel
                                },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = subtitle ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier,
                    )
                    runtime?.let {
                        val context = LocalContext.current
                        val resources = LocalResources.current
                        val now by LocalClock.current.now
                        val text =
                            remember(now, runtime) {
                                buildAnnotatedString {
                                    append(resources.formatDuration(runtime))
                                    dot()
                                    val endsAt = now.plus(runtime.toJavaDuration())
                                    val endTimeStr = formatTime(context, endsAt)
                                    append(resources.getString(R.string.ends_at, endTimeStr))
                                }
                            }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier
                                    .padding(top = 4.dp)
                                    .align(Alignment.Start),
                        )
                    }
                }
            }
            if (onDeck.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                OnDeckRow(
                    items = onDeck,
                    onClickItem = onClickOnDeckItem,
                    focusRequester = onDeckFocusRequester,
                )
            }
        }
    }
}

@Composable
fun NextUpCard(
    imageUrl: String?,
    onClick: () -> Unit,
    timeLeft: Duration?,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .background(
                            AppColors.TransparentBlack50,
                            shape = CircleShape,
                        ),
            ) {
                if (timeLeft != null && timeLeft > Duration.ZERO) {
                    Text(
                        text = timeLeft.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .size(60.dp)
                                .align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@PreviewTvSpec
@Composable
private fun NextUpEpisodePreview() {
    WholphinTheme(true) {
        val theme = MaterialTheme.colorScheme
        val previewHandler =
            AsyncImagePreviewHandler {
                ColorImage(theme.surfaceVariant.toArgb())
            }
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
        ) {
            CompositionLocalProvider(
                LocalAsyncImagePreviewHandler provides previewHandler,
                LocalClock provides Clock(),
            ) {
                NextUpEpisode(
                    title = "Series Title",
                    subtitle = "S1E2 - Episode Title",
                    description =
                        "This is the description of the episode. It might be long. " +
                            "It might be very, very long and overflow the available space. " +
                            "But it just keeps going and going.",
                    imageUrl = "",
                    onClick = {},
                    aspectRatio = AspectRatios.WIDE,
                    timeLeft = 30.seconds,
                    runtime = 32.minutes + 20.seconds,
                    modifier =
                        Modifier
                            .padding(8.dp)
                            .fillMaxHeight(.4f)
                            .fillMaxWidth(.66f)
                            .align(Alignment.BottomCenter)
                            .background(
                                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                shape = RoundedCornerShape(8.dp),
                            ),
                )
            }
        }
    }
}
