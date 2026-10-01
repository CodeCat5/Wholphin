package com.github.damontecres.wholphin.ui.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.ui.playback.overlay.BottomDialog
import com.github.damontecres.wholphin.ui.playback.overlay.BottomDialogItem

/**
 * Bitrate caps offered by the in-player quality picker.
 *
 * A `null` entry means "automatic", ie fall back to [AppPreference.MaxBitrate]. Choosing a cap
 * lower than the file's bitrate makes the server transcode, which is the point: it saves
 * bandwidth when streaming remotely.
 *
 * This is deliberately a shorter list than the one backing [AppPreference.MaxBitrate], since a
 * picker on the OSD needs to be navigable with a d-pad in a few presses.
 */
val playbackQualityOptions: List<Long?> =
    listOf(
        null,
        20 * AppPreference.MEGA_BIT,
        10 * AppPreference.MEGA_BIT,
        8 * AppPreference.MEGA_BIT,
        4 * AppPreference.MEGA_BIT,
        2 * AppPreference.MEGA_BIT,
        1 * AppPreference.MEGA_BIT,
        720 * 1024L,
    )

/**
 * Format a bitrate the same way [AppPreference.MaxBitrate] summarizes itself, so the OSD and the
 * settings screen agree.
 */
fun formatQualityBitrate(bitrate: Long): String =
    if (bitrate < AppPreference.MEGA_BIT) {
        "${bitrate / 1024} kbps"
    } else {
        "${bitrate / AppPreference.MEGA_BIT} Mbps"
    }

/**
 * Label for a [playbackQualityOptions] entry, where `null` is the automatic option.
 */
@Composable
fun qualityLabel(bitrate: Long?): String =
    if (bitrate == null) {
        stringResource(R.string.automatic)
    } else {
        formatQualityBitrate(bitrate)
    }

/**
 * Picker for the maximum streaming bitrate of the current playback session.
 *
 * @param currentChoice the active cap, or `null` for automatic
 * @param gravity the [android.view.Gravity] to align the dialog to left or right
 */
@Composable
fun QualityBottomDialog(
    onDismissRequest: () -> Unit,
    onSelectChoice: (Long?) -> Unit,
    gravity: Int,
    currentChoice: Long? = null,
) {
    val choices =
        playbackQualityOptions.map { bitrate ->
            BottomDialogItem(
                data = bitrate,
                headline = qualityLabel(bitrate),
                supporting = null,
            )
        }
    BottomDialog(
        choices = choices,
        currentChoice = choices.firstOrNull { it.data == currentChoice },
        onDismissRequest = onDismissRequest,
        onSelectChoice = { _, choice ->
            onSelectChoice(choice.data)
        },
        gravity = gravity,
    )
}
