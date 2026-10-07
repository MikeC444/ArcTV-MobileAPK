package com.mangotv.app.ui.torrent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.Episode
import com.mangotv.app.data.torrent.platform.IncomingTorrentInbox
import com.mangotv.app.ui.search.SearchUiState
import com.mangotv.app.ui.search.SearchViewModel
import com.mangotv.app.ui.theme.ArcAccent
import com.mangotv.app.ui.theme.ErrorCoral
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

/**
 * "Play this torrent": a magnet link or .torrent file opened or shared from another app does not say what it is. The person searches for the
 * movie or show it is, picks the episode for a show, and it plays at once (it is also kept on that title as a "My torrent" source).
 */
@Composable
fun TorrentOpenScreen(
    onClose: () -> Unit,
    onPlay: (route: String) -> Unit,
    viewModel: TorrentOpenViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    val incoming by IncomingTorrentInbox.pending.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val playRoute by viewModel.playRoute.collectAsStateWithLifecycle()

    LaunchedEffect(playRoute) { playRoute?.let(onPlay) }
    // Nothing waiting any more (it was attached, or cleared): leave.
    LaunchedEffect(incoming, playRoute) { if (incoming == null && playRoute == null) onClose() }
    BackHandler { if (step is TorrentOpenStep.PickTitle) onClose() else viewModel.back() }

    Column(modifier = Modifier.fillMaxSize().background(MangoBackground).statusBarsPadding().imePadding().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { if (step is TorrentOpenStep.PickTitle) onClose() else viewModel.back() }.padding(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Play this torrent", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        // What was received.
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MangoSurface).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = ArcAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (incoming?.fileUri != null) ".torrent file" else "Magnet link", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                Text(incoming?.label.orEmpty(), color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (error != null) {
            Text(error.orEmpty(), color = ErrorCoral, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        when (val s = step) {
            TorrentOpenStep.PickTitle -> PickTitle(searchViewModel, onChoose = viewModel::choose)
            is TorrentOpenStep.LoadingEpisodes -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ArcAccent)
                    Spacer(Modifier.height(10.dp))
                    Text("Loading ${s.show.title}…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is TorrentOpenStep.PickEpisode -> PickEpisode(s.show, onChoose = { ep -> viewModel.attach(s.show, ep.seasonNumber, ep.episodeNumber) })
        }
    }
}

@Composable
private fun PickTitle(searchViewModel: SearchViewModel, onChoose: (Content) -> Unit) {
    var query by remember { mutableStateOf("") }
    val state by searchViewModel.uiState.collectAsStateWithLifecycle()
    Text("Which movie or show is it?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    TextField(
        value = query,
        onValueChange = { query = it; searchViewModel.onQueryChanged(it) },
        placeholder = { Text("Search for the title") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MangoSurface,
            unfocusedContainerColor = MangoSurface,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = ArcAccent,
            focusedIndicatorColor = ArcAccent,
            unfocusedIndicatorColor = TextTertiary
        )
    )
    Spacer(Modifier.height(8.dp))
    when (val s = state) {
        SearchUiState.Idle -> Hint("Type at least two letters.")
        SearchUiState.Searching -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ArcAccent) }
        is SearchUiState.NoResults -> Hint("Nothing found for \"${s.query}\".")
        is SearchUiState.Error -> Hint(s.message)
        is SearchUiState.Results -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(s.movies + s.tvShows, key = { "${it.providerId}|${it.type}|${it.id}" }) { content -> TitleRow(content, onClick = { onChoose(content) }) }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = TextTertiary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun TitleRow(content: Content, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MangoSurface).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = content.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 48.dp, height = 72.dp).clip(RoundedCornerShape(8.dp)).background(MangoSurfaceHigh)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(content.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(if (content.type == com.mangotv.app.data.model.ContentType.MOVIE) "Movie" else "TV show", content.year?.toString()).joinToString(" · "),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PickEpisode(show: Content, onChoose: (Episode) -> Unit) {
    val seasons = remember(show) { show.seasons.filter { it.episodes.isNotEmpty() } }
    var seasonIndex by remember(show) { mutableIntStateOf(0) }
    val season = seasons.getOrNull(seasonIndex) ?: return
    Text(show.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("Which episode is it?", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(seasons.size) { index ->
            val picked = index == seasonIndex
            Text(
                text = seasons[index].name.ifBlank { "Season ${seasons[index].seasonNumber}" },
                color = if (picked) MangoBackground else TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (picked) ArcAccent else MangoSurface)
                    .clickable { seasonIndex = index }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
        items(season.episodes, key = { it.id }) { episode ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MangoSurface).clickable { onChoose(episode) }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("E${episode.episodeNumber}", color = ArcAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                Text(episode.title.ifBlank { "Episode ${episode.episodeNumber}" }, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
