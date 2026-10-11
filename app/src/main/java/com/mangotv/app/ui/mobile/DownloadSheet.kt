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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Check
import coil.compose.AsyncImage
import com.mangotv.app.data.download.filterOptions
import com.mangotv.app.data.download.hdrLabel
import com.mangotv.app.data.download.isUnusuallySmall
import com.mangotv.app.data.download.qualityFilters
import com.mangotv.app.data.download.sourceLabel
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.sources.debridLabel
import com.mangotv.app.ui.theme.ArcWarn
import com.mangotv.app.ui.theme.MangoAzure
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.MangoTeal
import com.mangotv.app.ui.theme.WatchedGreen
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
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
 * first and, within a quality, the smallest first. A header with the poster and title stays put; below it a show's Season and Episode drop-downs,
 * filter chips (resolution, and "Cached" for files a debrid service already has) and the list. Tapping a file hands the link to Android's
 * download manager, which saves it into Downloads with a notification. [onFileStarted] reports the episode a file was started for.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadSheet(
    title: String,
    subtitle: String,
    backdropUrl: String?,
    logoUrl: String?,
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
    var tierFilter by remember { mutableStateOf<ResolutionTier?>(null) }
    var cachedOnly by remember { mutableStateOf(false) }
    val isShow = contentType == ContentType.TV_SHOW && seasons.isNotEmpty()
    val found by produceState<List<DownloadOption>?>(initialValue = null, season, episode) {
        value = null
        value = DownloadFinder.find(contentType, contentId, season, episode)
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MangoBackgroundElevated,
        dragHandle = null
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).navigationBarsPadding()) {
            // Header, like the web's: the title's backdrop picture across the top with its logo (or its name) over the bottom, "Download" and a close
            // button over the top, and a fade into the sheet. It stays put while the list below scrolls.
            Box(Modifier.fillMaxWidth().height(200.dp).background(MangoSurfaceHigh)) {
                if (backdropUrl != null) {
                    AsyncImage(
                        model = rememberOpaqueImageRequest(backdropUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent, MangoBackgroundElevated.copy(alpha = 0.55f), MangoBackgroundElevated))
                    )
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DOWNLOAD", color = ArcCyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(20.dp)) }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                    var logoFailed by remember(logoUrl) { mutableStateOf(false) }
                    if (logoUrl != null && !logoFailed) {
                        AsyncImage(
                            model = rememberOpaqueImageRequest(logoUrl),
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart,
                            onError = { logoFailed = true },
                            modifier = Modifier.heightIn(max = 70.dp).widthIn(max = 260.dp)
                        )
                    } else {
                        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (subtitle.isNotEmpty()) Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (isShow) {
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            val all = found
            when {
                all == null -> Row(Modifier.padding(horizontal = 20.dp, vertical = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = ArcCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Finding files…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                all.isEmpty() -> Text(
                    "No downloadable files found. Only plain video files can be downloaded here, not streams or torrents.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp)
                )
                else -> {
                    val tiers = qualityFilters(all)
                    val anyCached = all.any { it.stream.debrid?.cached == true }
                    // A filter that no longer matches anything (another episode) is dropped instead of leaving an empty list.
                    val activeTier = tierFilter?.takeIf { it in tiers }
                    val activeCached = cachedOnly && anyCached
                    val list = filterOptions(all, activeTier, activeCached)
                    if (tiers.size > 1 || anyCached) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 14.dp)
                        ) {
                            item { FilterChip("All", selected = activeTier == null && !activeCached) { tierFilter = null; cachedOnly = false } }
                            items(tiers) { tier -> FilterChip(tierName(tier), selected = activeTier == tier) { tierFilter = if (activeTier == tier) null else tier } }
                            if (anyCached) item { FilterChip("Cached", selected = activeCached) { cachedOnly = !activeCached } }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (list.size == 1) "1 source" else "${list.size} sources", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                        Text("Best quality first, then smallest", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp)
                    )
                }
            }
        }
    }
}

private fun tierName(tier: ResolutionTier): String = when (tier) {
    ResolutionTier.UHD_4K -> "4K"
    ResolutionTier.FHD_1080P -> "1080p"
    ResolutionTier.HD_720P -> "720p"
    ResolutionTier.OTHER -> "Other"
}

private fun tierColor(tier: ResolutionTier): Color = when (tier) {
    ResolutionTier.UHD_4K -> ArcCyan
    ResolutionTier.FHD_1080P -> MangoAzure
    ResolutionTier.HD_720P -> MangoTeal
    ResolutionTier.OTHER -> TextTertiary
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) TextPrimary else MangoSurface)
            .border(1.dp, if (selected) TextPrimary else DividerSubtle, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) MangoBackground else TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
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

/** One file: what kind of release it is (the headline), its file name small underneath, then chips for the size, codec, HDR and whether it is cached. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileRow(option: DownloadOption, startedAlready: Boolean, onClick: () -> Unit) {
    val stream = option.stream
    val color = tierColor(stream.resolutionTier)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MangoSurface)
            .border(1.dp, if (startedAlready) ArcCyan.copy(alpha = 0.5f) else DividerSubtle, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(56.dp).clip(RoundedCornerShape(10.dp)).background(MangoBackground).border(BorderStroke(1.dp, color.copy(alpha = 0.6f)), RoundedCornerShape(10.dp)).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text(stream.qualityBadge, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sourceLabel(stream), color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stream.releaseTitle, color = TextTertiary, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                stream.sizeLabel?.let { Chip(it, TextPrimary, strong = true) }
                stream.codec?.let { Chip(it, TextSecondary) }
                hdrLabel(stream)?.let { Chip(it, ArcCyan, outlined = true) }
                if (isUnusuallySmall(stream)) Chip("Unusually small", ArcWarn, outlined = true)
                stream.debrid?.let { Chip(debridLabel(it), if (it.cached) WatchedGreen else ArcWarn) }
            }
        }
        Spacer(Modifier.width(10.dp))
        // A round button like the web's: outlined until this file was started, then filled with a tick.
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (startedAlready) ArcCyan else ArcCyan.copy(alpha = 0.12f))
                .border(1.dp, ArcCyan.copy(alpha = if (startedAlready) 1f else 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (startedAlready) Icons.Rounded.Check else Icons.Rounded.Download,
                contentDescription = if (startedAlready) "Download started" else "Download",
                tint = if (startedAlready) MangoBackground else ArcCyan
            )
        }
    }
}

@Composable
private fun Chip(text: String, color: Color, strong: Boolean = false, outlined: Boolean = false) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (outlined) Color.Transparent else Color(0x1AFFFFFF))
            .then(if (outlined) Modifier.border(1.dp, color.copy(alpha = 0.55f), shape) else Modifier)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}
