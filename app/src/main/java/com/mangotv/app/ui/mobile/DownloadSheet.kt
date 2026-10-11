package com.mangotv.app.ui.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.download.DownloadFinder
import com.mangotv.app.data.download.DownloadOption
import com.mangotv.app.data.download.FileDownloader
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.DEBRID_NAMES
import com.mangotv.app.data.model.Season
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.DividerSubtle
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoBackgroundElevated
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

/**
 * Download: the files the person's addons offer for a movie or an episode (plain https video files only: not streams or torrents), best quality
 * first and, within a quality, the smallest first. Tapping one hands the link to Android's download manager, which saves it into Downloads
 * with a notification. For a show, Season and Episode drop-downs pick which episode's files to list, starting on [startSeason] / [startEpisode].
 * [onFileStarted] reports the episode a file was started for (the page marks that episode's Download button as used).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSheet(
    title: String,
    contentType: ContentType,
    contentId: String,
    seasons: List<Season>,
    startSeason: Int?,
    startEpisode: Int?,
    onFileStarted: (season: Int?, episode: Int?) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var season by remember { mutableStateOf(startSeason) }
    var episode by remember { mutableStateOf(startEpisode) }
    var started by remember { mutableStateOf(setOf<String>()) }
    val isShow = contentType == ContentType.TV_SHOW && seasons.isNotEmpty()
    val options by produceState<List<DownloadOption>?>(initialValue = null, season, episode) {
        value = null
        value = DownloadFinder.find(contentType, contentId, season, episode)
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MangoBackgroundElevated
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text("Download", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)

            if (isShow) {
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val currentSeason = seasons.find { it.seasonNumber == season } ?: seasons.first()
                    Picker(
                        modifier = Modifier.weight(1f),
                        label = "Season",
                        value = seasonLabel(currentSeason),
                        items = seasons.map { it.seasonNumber to seasonLabel(it) },
                        onPick = { number ->
                            seasons.find { it.seasonNumber == number }?.episodes?.firstOrNull()?.let { first ->
                                season = first.seasonNumber
                                episode = first.episodeNumber
                            }
                        }
                    )
                    val currentEpisode = currentSeason.episodes.find { it.episodeNumber == episode } ?: currentSeason.episodes.firstOrNull()
                    Picker(
                        modifier = Modifier.weight(1f),
                        label = "Episode",
                        value = currentEpisode?.let { "${it.episodeNumber}. ${it.title}" } ?: "",
                        items = currentSeason.episodes.map { it.episodeNumber to "${it.episodeNumber}. ${it.title}" },
                        onPick = { number -> episode = number }
                    )
                }
            }

            val list = options
            when {
                list == null -> Row(Modifier.padding(vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = ArcCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Finding files…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                list.isEmpty() -> Text(
                    "No downloadable files found. Only plain video files can be downloaded here, not streams or torrents.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                else -> {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (list.size == 1) "1 source" else "${list.size} sources", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        Text("Best quality first, then smallest", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                    LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(list, key = { it.stream.id }) { option ->
                            FileRow(option, startedAlready = option.stream.id in started) {
                                try {
                                    FileDownloader.enqueue(context, option.download)
                                    started = started + option.stream.id
                                    Toast.makeText(context, "Downloading ${option.download.filename}", Toast.LENGTH_SHORT).show()
                                    onFileStarted(season, episode)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Couldn't start that download", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    Text(
                        "Tap a source to save the file to your Downloads.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

private fun seasonLabel(season: Season): String {
    val count = season.episodes.size
    val name = if (season.seasonNumber == 0) "Specials" else "Season ${season.seasonNumber}"
    return "$name · ${if (count == 1) "1 episode" else "$count episodes"}"
}

@Composable
private fun Picker(modifier: Modifier, label: String, value: String, items: List<Pair<Int, String>>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, color = TextTertiary, style = MaterialTheme.typography.labelMedium)
        Box {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MangoSurface)
                    .border(1.dp, DividerSubtle, RoundedCornerShape(12.dp))
                    .clickable { open = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                items.forEach { (number, text) ->
                    DropdownMenuItem(text = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { open = false; onPick(number) })
                }
            }
        }
    }
}

@Composable
private fun FileRow(option: DownloadOption, startedAlready: Boolean, onClick: () -> Unit) {
    val stream = option.stream
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MangoSurface)
            .border(1.dp, DividerSubtle, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(58.dp).clip(RoundedCornerShape(10.dp)).background(MangoBackground).border(1.dp, DividerSubtle, RoundedCornerShape(10.dp)).padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) { Text(stream.qualityBadge, color = ArcCyan, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stream.releaseTitle, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val facts = listOfNotNull(
                stream.sizeLabel,
                stream.codec,
                stream.debrid?.let { if (it.cached) "Cached on ${DEBRID_NAMES[it.service] ?: it.service}" else "Not cached: may take a while to start" }
            ).joinToString("  ·  ")
            if (facts.isNotEmpty()) Text(facts, color = TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(10.dp))
        // Plain until this file was started, then the accent colour (and a tick).
        Icon(
            if (startedAlready) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
            contentDescription = if (startedAlready) "Download started" else "Download",
            tint = if (startedAlready) ArcCyan else TextPrimary
        )
    }
}
