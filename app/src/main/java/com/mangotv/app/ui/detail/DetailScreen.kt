package com.mangotv.app.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import com.mangotv.app.ui.mobile.MobileDetailContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mangotv.app.data.history.ContinueWatchingEntry
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.recommend.Feedback
import com.mangotv.app.data.trailer.TrailerLauncher
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.navigation.routeForNavLabel
import com.mangotv.app.ui.components.ContentRow
import com.mangotv.app.ui.components.FullScreenErrorState
import com.mangotv.app.ui.components.HomeLoadingSkeleton
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangoBackground)
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> HomeLoadingSkeleton()
            is DetailUiState.Error -> FullScreenErrorState(
                message = state.message,
                onRetry = viewModel::load
            )
            is DetailUiState.Success -> {
                val isInMyList by viewModel.isInMyList.collectAsStateWithLifecycle()
                val feedback by viewModel.feedback.collectAsStateWithLifecycle()
                val hasPlus by viewModel.hasPlus.collectAsStateWithLifecycle()
                val resumeEntry by viewModel.resumeEntry.collectAsStateWithLifecycle()
                val trailerState by viewModel.trailerState.collectAsStateWithLifecycle()
                val foundTrailer = trailerState as? TrailerState.Found
                val releaseDateState by viewModel.releaseDateState.collectAsStateWithLifecycle()
                DetailContent(
                    content = state.content,
                    similar = state.similar,
                    onNavigate = onNavigate,
                    isInMyList = isInMyList,
                    onToggleMyList = viewModel::toggleMyList,
                    onToggleWatched = viewModel::toggleWatched,
                    feedback = feedback,
                    hasPlus = hasPlus,
                    onFeedback = viewModel::toggleFeedback,
                    resumeEntry = resumeEntry,
                    lastStreamIdFor = viewModel::lastStreamIdFor,
                    releaseDateState = releaseDateState,
                    // The button is always there, dimmed until a lookup has found a trailer (trailerReady).
                    // Hands the trailer off to whichever app the user picks rather than playing it in-app --
                    // see TrailerLauncher's own kdoc for why MangoTV stopped trying to play YouTube video itself.
                    // Pressed before one is found, it says why nothing opened.
                    onTrailer = {
                        when {
                            foundTrailer != null -> TrailerLauncher.launch(context, foundTrailer.youtubeVideoId)
                            trailerState == TrailerState.NotFound -> Toast.makeText(context, "No trailer found for this title", Toast.LENGTH_SHORT).show()
                            else -> Toast.makeText(context, "Looking for a trailer\u2026", Toast.LENGTH_SHORT).show()
                        }
                    },
                    trailerReady = foundTrailer != null
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    content: Content,
    similar: List<Content>,
    onNavigate: (String) -> Unit,
    isInMyList: Boolean,
    onToggleMyList: () -> Unit,
    onToggleWatched: () -> Unit,
    feedback: Feedback?,
    hasPlus: Boolean,
    onFeedback: (Feedback) -> Unit,
    resumeEntry: ContinueWatchingEntry?,
    lastStreamIdFor: (season: Int?, episode: Int?) -> String?,
    onTrailer: (() -> Unit)?,
    trailerReady: Boolean,
    releaseDateState: ReleaseDateState,
    modifier: Modifier = Modifier
) {
    fun navigateToContent(target: Content) {
        val providerId = target.providerId ?: return
        PendingDetailCache.stash(target)
        onNavigate(MangoRoutes.detail(providerId, target.type, target.id))
    }

    // Whenever a source is remembered for this exact title / season / episode, skip the Sources picker and go straight into the
    // player on it (what "Resume" means); otherwise the picker. Player's own error state offers a different source from there.
    fun navigateToPlayback(providerId: String, season: Int?, episode: Int?) {
        val streamId = lastStreamIdFor(season, episode)
        val route = if (streamId != null) {
            MangoRoutes.player(providerId, content.type, content.id, season, episode, streamId)
        } else {
            MangoRoutes.sources(providerId, content.type, content.id, season, episode)
        }
        onNavigate(route)
    }

    val releaseLabel = when (releaseDateState) {
        is ReleaseDateState.Found -> formatReleaseDate(releaseDateState.releaseDate) ?: content.year?.toString()
        ReleaseDateState.NotFound -> content.year?.toString()
        ReleaseDateState.Idle, ReleaseDateState.Loading -> null
    }

    Box(modifier = modifier.fillMaxSize()) {
        MobileDetailContent(
            content = content,
            similar = similar,
            isInMyList = isInMyList,
            onToggleMyList = onToggleMyList,
            onToggleWatched = onToggleWatched,
            feedback = feedback,
            hasPlus = hasPlus,
            onFeedback = onFeedback,
            resumeEntry = resumeEntry,
            onPlay = { season, episode -> content.providerId?.let { pid -> navigateToPlayback(pid, season, episode) } },
            onOpenSimilar = ::navigateToContent,
            onTrailer = onTrailer,
            trailerReady = trailerReady,
            releaseLabel = releaseLabel
        )
    }
}

@Composable
private fun DetailBackdrop(url: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = rememberOpaqueImageRequest(url),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )
}
