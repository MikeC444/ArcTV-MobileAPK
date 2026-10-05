package com.mangotv.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

/**
 * Settings > Audio: the preferred audio language, picked automatically when a video has a matching track (the in-player Audio menu can still
 * change it for the video that is playing). Shared with the Fire TV and web apps through the account's settings. Uses the same language
 * list as Subtitles, with "Automatic" in place of "System Default".
 *
 * The first row carries the pane's focus requester (it is item 0, so always composed when the tab opens) -- see SubtitleSettingsContent's
 * kdoc for why the other rows get none.
 */
@Composable
fun ColumnScope.AudioSettingsContent(
    navFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    sidebarFocusRequester: FocusRequester,
    viewModel: AudioSettingsViewModel = viewModel()
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "audio_title") {
            Text(
                text = "Preferred Audio Language",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        item(key = "audio_description") {
            Text(
                text = "Picked automatically when a video has a matching audio track. Shared with your other devices.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(SubtitleLanguageOptions, key = { it.code ?: "automatic" }) { option ->
            val first = option.code == null
            LanguageOptionRow(
                label = if (first) "Automatic" else option.label,
                selected = option.code == preferences.defaultAudioLanguage,
                onClick = { viewModel.setDefaultAudioLanguage(option.code) },
                focusRequester = if (first) contentFocusRequester else null,
                focusUp = if (first) navFocusRequester else null,
                focusLeft = if (first) sidebarFocusRequester else null
            )
        }
    }
}
