package com.mangotv.app.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Refresh
import com.mangotv.app.ui.theme.ArcWarn
import com.mangotv.app.data.model.StreamLookup
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.ui.components.ClickSound
import com.mangotv.app.ui.components.FullScreenErrorState
import com.mangotv.app.ui.components.HeroIconButton
import com.mangotv.app.ui.components.MangoButton
import com.mangotv.app.ui.components.MangoButtonStyle
import com.mangotv.app.ui.components.ShimmerBox
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.theme.DividerSubtle
import com.mangotv.app.ui.theme.ArcAccent
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

@Composable
fun SourcesScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourcesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addTorrentError by viewModel.addTorrentError.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangoBackground)
    ) {
        when (val state = uiState) {
            is SourcesUiState.Loading -> SourcesLoadingSkeleton(onBack = onBack)
            is SourcesUiState.Error -> FullScreenErrorState(
                message = state.message,
                onRetry = viewModel::load
            )
            is SourcesUiState.Loaded -> {
                val autoSelectStream = state.autoSelectStream
                if (autoSelectStream != null) {
                    // Resuming a title whose last-used source is still
                    // available -- skip the picker entirely and continue
                    // straight into Player, exactly as if the user had
                    // re-selected it themselves. Reuses the loading
                    // skeleton as the transitional frame while this fires,
                    // rather than flashing the interactive source list the
                    // user doesn't need to see.
                    LaunchedEffect(autoSelectStream.id) {
                        state.content.providerId?.let { pid ->
                            onNavigate(
                                MangoRoutes.player(pid, state.content.type, state.content.id, state.season, state.episode, autoSelectStream.id)
                            )
                        }
                    }
                    SourcesLoadingSkeleton(onBack = onBack)
                } else {
                    SourcesContent(
                        state = state,
                        onBack = onBack,
                        // Addons is now a tab inside the unified Settings
                        // screen rather than its own route -- this lands on
                        // Settings' default tab, not Addons specifically.
                        onManageAddons = { onNavigate(MangoRoutes.SETTINGS) },
                        onRetry = viewModel::load,
                        addTorrentError = addTorrentError,
                        onAddTorrentText = viewModel::addTorrentText,
                        onAddTorrentFile = viewModel::addTorrentFile,
                        onClearAddTorrentError = viewModel::clearAddTorrentError,
                        onSelectSource = { stream ->
                            state.content.providerId?.let { pid ->
                                onNavigate(
                                    MangoRoutes.player(pid, state.content.type, state.content.id, state.season, state.episode, stream.id)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Shown the instant Play is tapped, before getDetails/getStreams resolve —
 * mirrors [SourcesContent]'s actual layout (info panel + header + source
 * list) with shimmer placeholders standing in for data, rather than the
 * generic Home-shaped skeleton this screen used to borrow. Everything that
 * doesn't depend on network data — the back button, the "Select a Source"
 * header, and the safety footer — renders for real immediately, so the
 * source selector reads as already there rather than as an unrelated
 * loading interstitial.
 */
@Composable
private fun SourcesLoadingSkeleton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val rowShape = RoundedCornerShape(MangoDimens.CardCornerRadius)
    // The same split as SourcesContent: an upright phone has no title panel beside the list, just a back arrow and the title over it.
    val compactWindow = com.mangotv.app.ui.mobile.MobileMetrics.isCompact

    Row(modifier = modifier.fillMaxSize()) {
        if (!compactWindow) {
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .padding(22.dp)
            ) {
                HeroIconButton(
                    icon = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    clickSound = ClickSound.BACK
                )
                Spacer(Modifier.height(16.dp))
                ShimmerBox(modifier = Modifier.size(width = 84.dp, height = 126.dp))
                Spacer(Modifier.height(14.dp))
                ShimmerBox(modifier = Modifier.width(90.dp).height(18.dp))
                Spacer(Modifier.height(10.dp))
                ShimmerBox(modifier = Modifier.width(200.dp).height(26.dp))
                Spacer(Modifier.height(10.dp))
                ShimmerBox(modifier = Modifier.width(160.dp).height(16.dp))
                Spacer(Modifier.height(12.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.width(140.dp).height(14.dp))
                Spacer(Modifier.weight(1f))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(62.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(if (compactWindow) 1f else 0.65f)
                .fillMaxSize()
                .padding(horizontal = if (compactWindow) 16.dp else 36.dp, vertical = if (compactWindow) 8.dp else 22.dp)
        ) {
            if (compactWindow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ShimmerBox(modifier = Modifier.width(140.dp).height(18.dp))
                }
            }
            Text(
                text = "Select a Source",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose the best quality and server for your stream.",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(16.dp))
            // The filter bar: a pill each for Audio, All Sources, 4K, 1080p, 720p and Other, and the sort pill at the end.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                listOf(78.dp, 84.dp, 34.dp, 46.dp, 46.dp, 46.dp).forEach { width ->
                    ShimmerBox(modifier = Modifier.width(width).height(28.dp), shape = RoundedCornerShape(percent = 50))
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(if (compactWindow) 7 else 5) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(76.dp), shape = rowShape)
                }
            }
            Spacer(Modifier.height(14.dp))
            SafetyBar()
        }
    }
}

@Composable
private fun SourcesContent(
    state: SourcesUiState.Loaded,
    onBack: () -> Unit,
    onManageAddons: () -> Unit,
    onRetry: () -> Unit,
    addTorrentError: String?,
    onAddTorrentText: (String) -> Boolean,
    onAddTorrentFile: (android.net.Uri) -> Boolean,
    onClearAddTorrentError: () -> Unit,
    onSelectSource: (Stream) -> Unit
) {
    // "Add a torrent": the person's own magnet link or .torrent file for this title.
    var addingTorrent by remember { mutableStateOf(false) }
    if (addingTorrent) {
        AddTorrentDialog(
            error = addTorrentError,
            onSubmitText = { text -> if (onAddTorrentText(text)) addingTorrent = false },
            onPickFile = { uri -> if (onAddTorrentFile(uri)) addingTorrent = false },
            onDismiss = {
                addingTorrent = false
                onClearAddTorrentError()
            }
        )
    }
    // On a phone the list opens on 1080p (it plays smoothly where 4K often struggles); until the person picks a filter, a title with no 1080p
    // source shows everything instead of an empty list.
    var chosenFilter by remember { mutableStateOf<SourceFilter?>(null) }
    val selectedFilter = chosenFilter ?: if (state.streams.any { it.resolutionTier == ResolutionTier.FHD_1080P }) SourceFilter.FHD_1080P else SourceFilter.ALL
    // Biggest file first, as on the web; "Recommended" still marks the best source and always sits on top.
    var selectedSort by remember { mutableStateOf(SourceSort.SIZE) }

    // The Audio drop-down: filters by a sound layout this title's sources have (hidden when no source names its audio). The recommended
    // source comes from whatever is listed.
    var pickedAudio by remember { mutableStateOf<AudioChoice?>(null) }
    val audioChoiceList = remember(state.streams) { audioChoices(state.streams) }
    val audioChoice = (pickedAudio ?: AudioChoice.All).takeIf { it in audioChoiceList } ?: AudioChoice.All
    val audioOptions = remember(audioChoiceList, state.streams) { audioChoiceList.map { audioChoiceLabel(it, state.streams) } }
    val listed = remember(state.streams, audioChoice, audioChoiceList) {
        if (audioChoiceList.isEmpty()) state.streams else applyAudioChoice(state.streams, audioChoice)
    }
    val filtered = remember(listed, selectedFilter) {
        val tier = selectedFilter.tier
        if (tier == null) listed else listed.filter { it.resolutionTier == tier }
    }
    // The recommended source comes from what the filters show (so on the 1080p filter it is the best 1080p source, not a 4K one the filter
    // hides), falling back to everything listed when the filter shows nothing. It is always the first row, whatever the sort -- see orderSources().
    val recommendedId = remember(filtered, listed) { recommendedStreamId(filtered.ifEmpty { listed }) }
    val sorted = remember(listed, filtered, recommendedId, selectedSort) {
        orderSources(listed, filtered, recommendedId, selectedSort)
    }

    // Land the D-pad cursor on the first (top/best) source as soon as the
    // screen opens, rather than the default heuristic focusing the back
    // button — same "request focus exactly once" pattern DetailScreen uses
    // for its Play button.
    val firstSourceFocusRequester = remember { FocusRequester() }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    LaunchedEffect(sorted) {
        if (!hasRequestedInitialFocus && sorted.isNotEmpty()) {
            hasRequestedInitialFocus = true
            runCatching { firstSourceFocusRequester.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The backdrop now spans the entire screen (both the info panel and
        // the source list) instead of being cropped separately inside just
        // the left panel, so it reads as one continuous, centered photo
        // rather than a narrow sliver — only lightly dimmed for contrast.
        AsyncImage(
            model = rememberOpaqueImageRequest(state.content.backdropUrl ?: state.content.posterUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MangoBackground.copy(alpha = 0.4f))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Weighted, not a fixed dp width — a fixed width can eat a hugely
            // disproportionate share of the screen on non-standard displays
            // (e.g. an ultrawide monitor with unusual density reporting),
            // starving the row content on the right of the space it needs.
            val compactWindow = com.mangotv.app.ui.mobile.MobileMetrics.isCompact
            // An upright phone has no room for the title panel beside the list: the list takes the whole width, under a back arrow.
            if (!compactWindow) {
                SourcesInfoPanel(
                    content = state.content,
                    onBack = onBack,
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                )
            }

            Column(
                modifier = Modifier
                    .weight(if (compactWindow) 1f else 0.65f)
                    .fillMaxSize()
                    // Deliberately smaller than MangoDimens.ScreenPaddingHorizontal/
                    // Vertical (this screen's own local values, not the shared
                    // tokens other screens use) — this page packs a header, filter
                    // bar, several rows and a bottom bar into one non-scrolling
                    // view, so it needs tighter margins than a normal content page.
                    .padding(horizontal = if (compactWindow) 16.dp else 36.dp, vertical = if (compactWindow) 8.dp else 22.dp)
            ) {
                if (compactWindow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Text(text = state.content.title, color = TextSecondary, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Text(
                    text = "Select a Source",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Choose the best quality and server for your stream.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.height(16.dp))

                SourceFilterBar(
                    selectedFilter = selectedFilter,
                    onFilterChange = { chosenFilter = it },
                    selectedSort = selectedSort,
                    onSortChange = { selectedSort = it },
                    audioLabel = if (audioChoiceList.isEmpty()) null else audioButtonLabel(audioChoice),
                    audioOptions = audioOptions,
                    audioSelectedIndex = audioChoiceList.indexOf(audioChoice).coerceAtLeast(0),
                    audioHighlighted = audioChoice != AudioChoice.All,
                    onAudioSelect = { index -> pickedAudio = audioChoiceList.getOrNull(index) },
                    onAddTorrent = { addingTorrent = true }
                )

                Spacer(Modifier.height(14.dp))

                when {
                    // Nothing yet, but other providers still haven't answered
                    // -- this is not the same as "no sources found," so don't
                    // show that empty state's "try installing more addons"
                    // prompt before they've had a chance to reply.
                    sorted.isEmpty() && state.isSearchingMore ->
                        SourcesSearchingState(modifier = Modifier.weight(1f))
                    sorted.isEmpty() ->
                        SourcesEmptyState(
                            addons = state.addons,
                            onManageAddons = onManageAddons,
                            onRetry = onRetry,
                            modifier = Modifier.weight(1f)
                        )
                    else -> Column(modifier = Modifier.weight(1f)) {
                        if (state.isSearchingMore) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ArcAccent, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Looking for more sources…",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            // Top padding so the "Recommended" badge — which floats
                            // above its row via a negative offset — has room to
                            // show fully instead of being clipped by the list's own
                            // top edge when that row is first/near the top.
                            contentPadding = PaddingValues(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(sorted, key = { _, stream -> stream.id }) { index, stream ->
                                SourceRow(
                                    stream = stream,
                                    isRecommended = stream.id == recommendedId,
                                    onClick = { onSelectSource(stream) },
                                    focusRequester = if (index == 0) firstSourceFocusRequester else null
                                )
                            }
                        }
                        // Some addons didn't answer, so say so even though other sources were found -- the list may
                        // be missing the best one.
                        if (!state.isSearchingMore && anyAddonFailed(state.addons)) {
                            Text(
                                text = "Some addons didn't answer, so this list may be incomplete: " +
                                    failedAddonLines(state.addons).joinToString("; "),
                                color = ArcWarn,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                SafetyBar()
            }
        }
    }
}

// More than this many addons and the rest are summarised, so the empty state still fits a TV screen.
private const val MAX_ADDON_LINES = 5

@Composable
private fun SourcesEmptyState(
    addons: List<AddonLookupRow>,
    onManageAddons: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.height(40.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No sources found",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = noSourcesHint(addons),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp)
        )
        // What each addon answered, so a missing source is never a mystery.
        if (addons.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            addons.take(MAX_ADDON_LINES).forEach { row ->
                Text(
                    text = "${row.name}  \u2014  ${lookupText(row.lookup)}",
                    color = if (row.lookup is StreamLookup.Failed) ArcWarn else TextTertiary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (addons.size > MAX_ADDON_LINES) {
                Text(
                    text = "and ${addons.size - MAX_ADDON_LINES} more",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (anyAddonFailed(addons)) {
                MangoButton(
                    text = "Try Again",
                    icon = Icons.Filled.Refresh,
                    onClick = onRetry,
                    style = MangoButtonStyle.GLASS
                )
            }
            MangoButton(
                text = "Manage Addons",
                icon = Icons.Filled.Extension,
                onClick = onManageAddons,
                style = MangoButtonStyle.GLASS
            )
        }
    }
}

/**
 * Shown in place of [SourcesEmptyState] while at least one active provider's
 * getStreams() call is still outstanding and nothing has come back yet --
 * distinct from "no sources found" (which reads as final and prompts
 * installing more addons) since providers still in flight may yet answer.
 */
@Composable
private fun SourcesSearchingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = ArcAccent, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Searching for sources…",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Checking your installed addons for this title.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SafetyBar() {
    // A hairline divider rather than a filled card — reads as part of the
    // screen, not a separate popped-out alert box.
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerSubtle)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.height(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safe & secure",
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "All sources are scanned for your safety",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.width(14.dp))
            MangoButton(
                text = "How it works",
                icon = Icons.Filled.Info,
                onClick = {},
                style = MangoButtonStyle.GLASS,
                compact = true
            )
        }
    }
}
