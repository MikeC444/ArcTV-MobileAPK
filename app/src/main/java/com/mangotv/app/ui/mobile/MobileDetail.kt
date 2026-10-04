package com.mangotv.app.ui.mobile

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.history.ContinueWatchingEntry
import com.mangotv.app.data.model.CastMember
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.Episode
import com.mangotv.app.data.recommend.Feedback
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.ProgressFill
import com.mangotv.app.ui.theme.ProgressTrack
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

/**
 * A title's page for touch: a picture header with a back button, then title, details, one big Play / Resume button with round
 * buttons beside it, the story, seasons and episodes as a tappable list, cast, and similar titles. On wide windows the header
 * shows the backdrop and the text column sits beside the poster.
 */
@Composable
fun MobileDetailContent(
    content: Content,
    similar: List<Content>,
    isInMyList: Boolean,
    onToggleMyList: () -> Unit,
    onToggleWatched: () -> Unit,
    feedback: Feedback?,
    hasPlus: Boolean,
    onFeedback: (Feedback) -> Unit,
    resumeEntry: ContinueWatchingEntry?,
    onPlay: (season: Int?, episode: Int?) -> Unit,
    onOpenSimilar: (Content) -> Unit,
    onTrailer: (() -> Unit)?,
    trailerReady: Boolean,
    releaseLabel: String?,
    modifier: Modifier = Modifier
) {
    val back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val pad = MangoDimens.ScreenPaddingHorizontal
    val compact = MobileMetrics.isCompact
    val isShow = content.type == ContentType.TV_SHOW

    // Which episode Play / Resume starts (same rule as the TV page: a Continue Watching hit first, else the first episode).
    val resumeEpisode = resumeEntry?.takeIf { it.seasonNumber != null && it.episodeNumber != null }?.let { entry ->
        content.seasons.find { it.seasonNumber == entry.seasonNumber }?.episodes?.find { it.episodeNumber == entry.episodeNumber }
    }
    val startEpisode = if (isShow) resumeEpisode ?: content.seasons.firstOrNull()?.episodes?.firstOrNull() else null
    val verb = if (resumeEntry != null) "Resume" else "Play"
    val playText = if (isShow && startEpisode != null) "$verb S${startEpisode.seasonNumber}E${startEpisode.episodeNumber}" else verb

    Box(modifier.fillMaxSize().background(MangoBackground)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth().aspectRatio(if (compact) 16f / 10f else 21f / 9f)) {
                    AsyncImage(
                        model = rememberOpaqueImageRequest(content.backdropUrl ?: content.posterUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().background(MangoSurfaceHigh)
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to MangoBackground)))
                }
            }
            item(key = "info") {
                Column(Modifier.padding(horizontal = pad).widthIn(max = 840.dp)) {
                    if (content.logoUrl != null) {
                        AsyncImage(
                            model = rememberOpaqueImageRequest(content.logoUrl),
                            contentDescription = content.title,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart,
                            modifier = Modifier.height(72.dp).widthIn(max = 300.dp)
                        )
                    } else {
                        Text(content.title, color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    val meta = listOfNotNull(
                        releaseLabel ?: content.year?.toString(),
                        content.runtimeMinutes?.let { "${it / 60}h ${it % 60}m" },
                        content.ageRating,
                        content.rating?.let { "★ ${"%.1f".format(it)}" }
                    ).joinToString("  ·  ")
                    if (meta.isNotEmpty()) {
                        Text(meta, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (content.genres.isNotEmpty()) {
                        Text(
                            content.genres.joinToString("  •  ") { it.name },
                            color = TextTertiary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // The one big action, with round buttons beside it.
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .height(52.dp)
                                .clip(RoundedCornerShape(50))
                                .background(TextPrimary)
                                .clickable { onPlay(startEpisode?.seasonNumber, startEpisode?.episodeNumber) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MangoBackground)
                            Spacer(Modifier.width(6.dp))
                            Text(playText, color = MangoBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        RoundAction(
                            icon = if (isInMyList) Icons.Rounded.Check else Icons.Rounded.Add,
                            label = if (isInMyList) "Remove from My List" else "Add to My List",
                            highlighted = isInMyList,
                            onClick = onToggleMyList
                        )
                        RoundAction(
                            icon = Icons.Rounded.Visibility,
                            label = if (content.watched) "Mark as not watched" else "Mark as watched",
                            highlighted = content.watched,
                            onClick = onToggleWatched
                        )
                        if (onTrailer != null) {
                            RoundAction(Icons.Rounded.SmartDisplay, "Trailer", highlighted = false, dimmed = !trailerReady, onClick = onTrailer)
                        }
                    }
                    if (hasPlus && content.type == ContentType.MOVIE) {
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RoundAction(Icons.Rounded.ThumbUp, "Like", highlighted = feedback == Feedback.LIKE) { onFeedback(Feedback.LIKE) }
                            RoundAction(Icons.Rounded.ThumbDown, "Not for me", highlighted = feedback == Feedback.DISLIKE) { onFeedback(Feedback.DISLIKE) }
                        }
                    }
                    resumeEntry?.let { entry ->
                        val fraction = if (entry.durationMs > 0) (entry.positionMs.toFloat() / entry.durationMs).coerceIn(0f, 1f) else 0f
                        if (fraction > 0f) {
                            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(ProgressTrack)) {
                                Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(ProgressFill))
                            }
                        }
                    }
                    if (content.description.isNotBlank()) {
                        Text(content.description, color = TextSecondary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
                    }
                    content.director?.takeIf { it.isNotBlank() }?.let {
                        Text("Director: $it", color = TextTertiary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            if (isShow && content.seasons.isNotEmpty()) {
                item(key = "seasons") { SeasonsList(content, onPlay = { ep -> onPlay(ep.seasonNumber, ep.episodeNumber) }) }
            }
            if (content.cast.isNotEmpty()) {
                item(key = "cast") {
                    SectionTitle("Cast")
                    LazyRow(contentPadding = PaddingValues(horizontal = pad), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(content.cast, key = { it.name + (it.role ?: "") }) { CastTile(it) }
                    }
                }
            }
            if (similar.isNotEmpty()) {
                item(key = "similar") {
                    SectionTitle("You may also like")
                    LazyRow(contentPadding = PaddingValues(horizontal = pad), horizontalArrangement = Arrangement.spacedBy(MangoDimens.CardSpacing)) {
                        items(similar, key = { "${it.providerId}:${it.id}" }) { MobilePoster(content = it, onClick = { onOpenSimilar(it) }) }
                    }
                }
            }
        }
        // Over the picture, clear of the status bar (the shell already pads for it).
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .size(MangoDimens.TouchTarget)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .clickable { back?.onBackPressed() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, highlighted: Boolean, dimmed: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .alpha(if (dimmed) 0.45f else 1f)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = label, tint = if (highlighted) ArcCyan else TextPrimary) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = TextPrimary,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = MangoDimens.ScreenPaddingHorizontal, end = MangoDimens.ScreenPaddingHorizontal, top = 24.dp, bottom = 10.dp)
    )
}

@Composable
private fun SeasonsList(content: Content, onPlay: (Episode) -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    val season = content.seasons.getOrNull(selected) ?: return
    val pad = MangoDimens.ScreenPaddingHorizontal
    Column {
        SectionTitle("Seasons")
        LazyRow(contentPadding = PaddingValues(horizontal = pad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(content.seasons.size) { i ->
                val on = i == selected
                Box(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) TextPrimary else Color(0x1FFFFFFF))
                        .clickable { selected = i }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Season ${content.seasons[i].seasonNumber}",
                        color = if (on) MangoBackground else TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        Text(
            "${season.name}  ·  ${season.episodes.size} episodes",
            color = TextTertiary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = pad, vertical = 12.dp)
        )
        season.episodes.forEach { ep ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(ep) }
                    .padding(horizontal = pad, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.width(128.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(MangoSurfaceHigh)) {
                    if (ep.thumbnailUrl != null) {
                        AsyncImage(
                            model = rememberOpaqueImageRequest(ep.thumbnailUrl),
                            contentDescription = ep.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color(0x8C000000), CircleShape).padding(4.dp)
                    )
                    ep.watchProgress?.let { p ->
                        if (p.fraction > 0f) {
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(ProgressTrack)) {
                                Box(Modifier.fillMaxWidth(p.fraction).height(3.dp).background(ProgressFill))
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(
                            "${ep.episodeNumber}. ${ep.title}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        ep.runtimeMinutes?.let { Text("  ${it}m", color = TextTertiary, style = MaterialTheme.typography.labelSmall) }
                    }
                    if (ep.description.isNotBlank()) {
                        Text(ep.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CastTile(member: CastMember) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(CircleShape).background(MangoSurface), contentAlignment = Alignment.Center) {
            if (member.photoUrl != null) {
                AsyncImage(
                    model = rememberOpaqueImageRequest(member.photoUrl),
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(32.dp))
            }
        }
        Text(
            member.name,
            color = TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
