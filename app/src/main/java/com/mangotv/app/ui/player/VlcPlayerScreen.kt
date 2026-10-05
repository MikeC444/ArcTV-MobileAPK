package com.mangotv.app.ui.player

import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.Episode
import com.mangotv.app.data.model.PlayerPreferences
import com.mangotv.app.ui.components.MangoButton
import com.mangotv.app.ui.components.MangoButtonStyle
import com.mangotv.app.ui.player.overlay.EpisodePanel
import com.mangotv.app.ui.player.overlay.PlayerChoiceCard
import com.mangotv.app.ui.player.overlay.PlayerChoiceOption
import com.mangotv.app.ui.theme.ArcAccent
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale

private const val SEEK_STEP_MS = 10_000L
private const val FIRST_REPORT_MS = 4_000L
private const val REPORT_INTERVAL_MS = 15_000L
private const val CONTROLS_HIDE_MS = 4_000L
private val SPEEDS = floatArrayOf(0.75f, 1f, 1.25f, 1.5f, 2f)

private enum class VlcMenu { AUDIO, SUBTITLES, SPEED, PLAYER, EPISODES }

private data class VlcTrack(val id: Int, val name: String)

/**
 * Plays [url] with VLC's own engine (LibVLC): it ships FFmpeg's software decoders for nearly every codec, so it plays formats the phone's chips
 * have no decoder for (at the cost of more CPU on big video). It is the phone app's default player, as on Fire TV.
 *
 * Touch controls: tap the picture to show or hide the controls, double-tap the left or right half to jump 10 s back or forward, drag the
 * progress bar to seek. It keeps Continue Watching working through [onReportProgress], and starts at [startPositionMs] so resuming (or
 * switching over from the built-in player) picks up where the person was.
 */
@Composable
fun VlcPlaybackContent(
    content: Content,
    episode: Episode?,
    url: String,
    startPositionMs: Long,
    preferences: PlayerPreferences,
    onReportProgress: (positionMs: Long, durationMs: Long, completed: Boolean) -> Unit,
    // The Choose player card's other rows: back to the built-in player from the given position, or another app (true when one opened).
    onPlayWithBuiltIn: (positionMs: Long) -> Unit,
    onOpenExternal: () -> Boolean,
    // Called when the card's built-in or VLC row is played, so the title remembers it.
    onRememberPlayer: (PreferredPlayer) -> Unit,
    // The episode after this one (null for a movie or the last episode): offered in the last minute, and counted down to after the end when
    // Auto Play Next Episode is on, exactly as in the built-in player.
    next: NextEpisode?,
    onNextEpisode: (season: Int, episode: Int) -> Unit,
    onChangeSource: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    val libVlc = remember { LibVLC(context, arrayListOf("--http-reconnect", "--network-caching=2000", "--no-drop-late-frames", "--no-skip-frames")) }
    val mediaPlayer = remember { MediaPlayer(libVlc) }

    var viewReady by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<String?>(null) }
    var lengthMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf<VlcMenu?>(null) }
    var showChoice by remember { mutableStateOf(false) }
    // True once the first picture plays: until then the loading screen (backdrop and logo) covers the video.
    var started by remember { mutableStateOf(false) }
    var upNext by remember { mutableStateOf<NextEpisode?>(null) }
    var offerNext by remember { mutableStateOf(false) }
    val nextOfferFocus = remember { FocusRequester() }
    var audioTracks by remember { mutableStateOf<List<VlcTrack>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<VlcTrack>>(emptyList()) }
    var selectedAudio by remember { mutableIntStateOf(-1) }
    var selectedSubtitle by remember { mutableIntStateOf(-1) }
    var preferencesApplied by remember { mutableStateOf(false) }
    var speedIndex by remember { mutableIntStateOf(1) }
    var seekText by remember { mutableStateOf<String?>(null) }
    // While a finger drags the thumb the bar follows it; VLC is asked to jump once, when the finger lifts.
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    fun bump() { interactionTick++ }
    fun refreshTracks() {
        audioTracks = mediaPlayer.audioTracks?.map { VlcTrack(it.id, it.name) }.orEmpty().filter { it.id >= 0 }
        subtitleTracks = mediaPlayer.spuTracks?.map { VlcTrack(it.id, it.name) }.orEmpty()
        selectedAudio = mediaPlayer.audioTrack
        selectedSubtitle = mediaPlayer.spuTrack
    }
    fun togglePlay() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
        bump()
    }
    fun seekBy(deltaMs: Long) {
        val length = lengthMs.takeIf { it > 0 } ?: mediaPlayer.length
        var target = (mediaPlayer.time + deltaMs).coerceAtLeast(0)
        if (length > 1_000) target = target.coerceAtMost(length - 1_000)
        mediaPlayer.setTime(target)
        positionMs = target
        seekText = if (deltaMs < 0) "«« 10 seconds" else "10 seconds »»"
        bump()
    }
    fun seekToFraction(fraction: Float) {
        val length = lengthMs.takeIf { it > 0 } ?: mediaPlayer.length
        if (length <= 0) return
        val target = (length * fraction.coerceIn(0f, 1f)).toLong().coerceAtMost((length - 1_000).coerceAtLeast(0))
        mediaPlayer.setTime(target)
        positionMs = target
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.setEventListener(object : MediaPlayer.EventListener {
            override fun onEvent(event: MediaPlayer.Event) {
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        playing = true
                        started = true
                        buffering = false
                    }
                    MediaPlayer.Event.Paused -> {
                        playing = false
                        onReportProgress(mediaPlayer.time, mediaPlayer.length, false)
                    }
                    MediaPlayer.Event.Buffering -> buffering = event.buffering < 100f
                    MediaPlayer.Event.LengthChanged -> lengthMs = event.lengthChanged
                    MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESSelected -> refreshTracks()
                    MediaPlayer.Event.EndReached -> {
                        playing = false
                        if (next != null && preferences.autoplayNextEpisode) upNext = next
                        onReportProgress(mediaPlayer.length, mediaPlayer.length, true)
                    }
                    MediaPlayer.Event.EncounteredError -> failure = "VLC could not play this source either."
                }
            }
        })
        onDispose {
            mediaPlayer.setEventListener(null)
            if (mediaPlayer.length > 0) onReportProgress(mediaPlayer.time, mediaPlayer.length, false)
            mediaPlayer.detachViews()
            // Stopping and releasing can block, so not on the main thread.
            Thread {
                mediaPlayer.stop()
                mediaPlayer.release()
                libVlc.release()
            }.start()
        }
    }

    fun loadMedia() {
        val media = Media(libVlc, Uri.parse(url))
        // The phone's hardware decoder (VLC still falls back to its own software decoder if the hardware one can't take the video).
        media.setHWDecoderEnabled(true, false)
        // Open the file at the spot rather than jumping once it plays: a jump mid-stream can land between key frames and show a broken picture.
        if (startPositionMs > 5_000) media.addOption(":start-time=${startPositionMs / 1000.0}")
        mediaPlayer.setMedia(media)
        media.release()
        mediaPlayer.play()
        mediaPlayer.setRate(SPEEDS[speedIndex])
    }

    LaunchedEffect(viewReady) {
        if (viewReady) loadMedia()
    }

    // Language preferences: match a track's name against the language's English name, the way VLC labels them ("Track 1 - [English]").
    LaunchedEffect(audioTracks, subtitleTracks) {
        if (preferencesApplied || audioTracks.isEmpty()) return@LaunchedEffect
        preferencesApplied = true
        fun matching(tracks: List<VlcTrack>, code: String?): VlcTrack? {
            if (code == null) return null
            val language = Locale(code).getDisplayLanguage(Locale.ENGLISH)
            return tracks.firstOrNull { it.name.contains(language, ignoreCase = true) }
        }
        matching(audioTracks, preferences.defaultAudioLanguage)?.let { mediaPlayer.setAudioTrack(it.id) }
        if (!preferences.subtitlesEnabled) {
            mediaPlayer.setSpuTrack(-1)
        } else {
            matching(subtitleTracks, preferences.defaultSubtitleLanguage)?.let { mediaPlayer.setSpuTrack(it.id) }
        }
        refreshTracks()
    }

    LaunchedEffect(seekText) {
        if (seekText != null) {
            delay(900)
            seekText = null
        }
    }

    // The timeline, and Continue Watching while playing (first after a few seconds, then every 15 s).
    LaunchedEffect(playing) {
        var sinceReport = REPORT_INTERVAL_MS - FIRST_REPORT_MS
        while (true) {
            val reported = mediaPlayer.time.coerceAtLeast(0)
            // VLC reports 0 once playback has stopped at the end: keep the last real position then, so the Next episode button (which needs a
            // position inside the last minute) stays up until the episode is over.
            val stoppedAtEnd = reported == 0L && positionMs > 0 && lengthMs > 0 && positionMs >= lengthMs - 120_000
            if (dragFraction == null) positionMs = if (stoppedAtEnd) positionMs else reported
            if (lengthMs <= 0) lengthMs = mediaPlayer.length.coerceAtLeast(0)
            offerNext = offerNextEpisode(positionMs, lengthMs, next != null)
            delay(500)
            if (playing) {
                sinceReport += 500
                if (sinceReport >= REPORT_INTERVAL_MS) {
                    sinceReport = 0
                    onReportProgress(mediaPlayer.time, mediaPlayer.length, false)
                }
            }
        }
    }

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // The controls hide a few seconds after the last touch, unless a menu is open or playback is paused.
    LaunchedEffect(controlsVisible, interactionTick, menu, playing) {
        if (controlsVisible && menu == null && playing) {
            delay(CONTROLS_HIDE_MS)
            controlsVisible = false
        }
    }

    BackHandler(enabled = showChoice) { showChoice = false }
    BackHandler(enabled = menu != null && !showChoice) { menu = null }
    BackHandler(enabled = menu == null && !showChoice) { onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (showChoice) Unit else if (menu != null) menu = null else controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        if (menu == null && failure == null) seekBy(if (offset.x < size.width / 2) -SEEK_STEP_MS else SEEK_STEP_MS)
                    }
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).also { layout ->
                    layout.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    mediaPlayer.attachViews(layout, null, true, false)
                    mediaPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT)
                    viewReady = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!started && failure == null) {
            PlayerLoadingScreen(content = content, episode = episode, busy = true)
        }

        if (buffering && started && failure == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(48.dp), color = Color.White)
        }

        seekText?.let { text -> SeekIndicatorPill(text = text, modifier = Modifier.align(Alignment.Center)) }

        if (controlsVisible && started && failure == null && menu == null && upNext == null) {
            VlcTouchControls(
                title = listOfNotNull(content.title, episode?.let { "S${it.seasonNumber} E${it.episodeNumber}" }).joinToString("  "),
                playing = playing,
                positionMs = positionMs,
                lengthMs = lengthMs,
                dragFraction = dragFraction,
                speedLabel = speedLabel(SPEEDS[speedIndex]),
                hasAudioChoice = audioTracks.size > 1,
                hasSubtitles = subtitleTracks.isNotEmpty(),
                hasEpisodes = episode != null && content.seasons.any { it.episodes.isNotEmpty() },
                onBack = onBack,
                onTogglePlay = ::togglePlay,
                onSkip = { seekBy(it) },
                onDragStart = { fraction -> dragFraction = fraction; bump() },
                onDrag = { fraction -> dragFraction = fraction; bump() },
                onDragEnd = {
                    dragFraction?.let { seekToFraction(it) }
                    dragFraction = null
                    bump()
                },
                onTapBar = { fraction -> seekToFraction(fraction); bump() },
                onOpenMenu = { if (it == VlcMenu.PLAYER) showChoice = true else menu = it }
            )
        }

        if (offerNext && next != null && upNext == null && menu == null && failure == null) {
            NextEpisodeOffer(
                next = next,
                onGo = { onNextEpisode(next.season, next.episode) },
                focusRequester = nextOfferFocus,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        upNext?.let { target ->
            UpNextCard(
                next = target,
                onGo = { onNextEpisode(target.season, target.episode) },
                onCancel = { upNext = null },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        when (menu) {
            VlcMenu.PLAYER -> Unit
            VlcMenu.EPISODES -> EpisodePanel(
                seasons = content.seasons,
                currentSeason = episode?.seasonNumber,
                currentEpisode = episode?.episodeNumber,
                onPick = { pickedSeason, pickedEpisode ->
                    menu = null
                    onNextEpisode(pickedSeason, pickedEpisode)
                },
                onClose = { menu = null }
            )
            VlcMenu.AUDIO -> VlcTrackMenu(
                title = "Audio",
                tracks = audioTracks,
                selectedId = selectedAudio,
                offLabel = null,
                onSelect = { id -> mediaPlayer.setAudioTrack(id); refreshTracks(); menu = null },
                onClose = { menu = null }
            )
            VlcMenu.SUBTITLES -> VlcTrackMenu(
                title = "Subtitles",
                tracks = subtitleTracks.filter { it.id >= 0 },
                selectedId = selectedSubtitle,
                offLabel = "Off",
                onSelect = { id -> mediaPlayer.setSpuTrack(id); refreshTracks(); menu = null },
                onClose = { menu = null }
            )
            VlcMenu.SPEED -> VlcOptionMenu(
                title = "Playback speed",
                labels = SPEEDS.map { speedLabel(it) },
                selectedIndex = speedIndex,
                onSelect = { index ->
                    speedIndex = index
                    mediaPlayer.setRate(SPEEDS[index])
                    menu = null
                },
                onClose = { menu = null }
            )
            null -> Unit
        }

        failure?.let { message ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                    Text("Unable to play this source", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MangoButton(text = "Other Players", icon = Icons.Filled.OpenInNew, onClick = { showChoice = true }, style = MangoButtonStyle.GLASS, borderColor = Color.White)
                        MangoButton(text = "Change Source", icon = Icons.Filled.SwapHoriz, onClick = onChangeSource, style = MangoButtonStyle.GLASS, borderColor = Color.White)
                        MangoButton(text = "Back", icon = Icons.Filled.ArrowBack, onClick = onBack, style = MangoButtonStyle.GLASS, borderColor = Color.White)
                    }
                }
            }
        }

        if (showChoice) {
            PlayerChoiceCard(
                externalAvailable = hasExternalPlayer(context, url),
                initial = PlayerChoiceOption.VLC,
                onPlay = { option ->
                    when (option) {
                        PlayerChoiceOption.BUILT_IN -> {
                            onRememberPlayer(PreferredPlayer.BUILT_IN)
                            showChoice = false
                            onPlayWithBuiltIn(mediaPlayer.time.coerceAtLeast(0))
                        }
                        PlayerChoiceOption.VLC -> {
                            onRememberPlayer(PreferredPlayer.VLC)
                            showChoice = false
                        }
                        PlayerChoiceOption.EXTERNAL -> {
                            mediaPlayer.pause()
                            if (onOpenExternal()) showChoice = false
                        }
                    }
                },
                onCancel = { showChoice = false }
            )
        }
    }
}

private fun speedLabel(speed: Float): String = if (speed == 1f) "Normal" else "${speed}x"

/** The touch controls: back and title on top, skip / play / skip in the middle, the progress bar and Audio / Subtitles / Speed along the bottom. */
@Composable
private fun VlcTouchControls(
    title: String,
    playing: Boolean,
    positionMs: Long,
    lengthMs: Long,
    dragFraction: Float?,
    speedLabel: String,
    hasAudioChoice: Boolean,
    hasSubtitles: Boolean,
    hasEpisodes: Boolean,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onSkip: (Long) -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onTapBar: (Float) -> Unit,
    onOpenMenu: (VlcMenu) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TouchIcon(Icons.Filled.ArrowBack, "Back", 40.dp, onBack)
            Spacer(Modifier.size(12.dp))
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TouchIcon(Icons.Filled.Replay10, "Back 10 seconds", 48.dp) { onSkip(-SEEK_STEP_MS) }
            TouchIcon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", 64.dp, onTogglePlay)
            TouchIcon(Icons.Filled.Forward10, "Forward 10 seconds", 48.dp) { onSkip(SEEK_STEP_MS) }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
            val fraction = dragFraction ?: if (lengthMs > 0) (positionMs.toFloat() / lengthMs).coerceIn(0f, 1f) else 0f
            VlcSeekBar(fraction = fraction, onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd, onTap = onTapBar)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val shownMs = dragFraction?.let { (it * lengthMs).toLong() } ?: positionMs
                Text(formatTimestamp(shownMs), color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                if (hasAudioChoice) MenuChip("Audio") { onOpenMenu(VlcMenu.AUDIO) }
                if (hasSubtitles) MenuChip("Subtitles") { onOpenMenu(VlcMenu.SUBTITLES) }
                if (hasEpisodes) MenuChip("Episodes") { onOpenMenu(VlcMenu.EPISODES) }
                MenuChip("Speed: $speedLabel") { onOpenMenu(VlcMenu.SPEED) }
                MenuChip("Player") { onOpenMenu(VlcMenu.PLAYER) }
                Spacer(Modifier.weight(1f))
                Text(formatTimestamp(lengthMs.coerceAtLeast(0)), color = TextPrimary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TouchIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size + 16.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = TextPrimary, modifier = Modifier.size(size))
    }
}

@Composable
private fun MenuChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = TextPrimary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

/** A draggable progress bar: tap to jump, or drag the thumb; the seek itself happens when the finger lifts. */
@Composable
private fun VlcSeekBar(
    fraction: Float,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset -> onTap((offset.x / size.width).coerceIn(0f, 1f)) })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onDragStart((offset.x / size.width).coerceIn(0f, 1f)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onDrag((change.position.x / size.width).coerceIn(0f, 1f))
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.3f)))
        Box(modifier = Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ArcAccent))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(fraction.coerceIn(0.0001f, 1f)))
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(ArcAccent))
            Spacer(Modifier.weight((1f - fraction).coerceIn(0.0001f, 1f)))
        }
    }
}

@Composable
private fun VlcTrackMenu(
    title: String,
    tracks: List<VlcTrack>,
    selectedId: Int,
    offLabel: String?,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    val labels = buildList {
        if (offLabel != null) add(offLabel)
        addAll(tracks.map { it.name })
    }
    val ids = buildList {
        if (offLabel != null) add(-1)
        addAll(tracks.map { it.id })
    }
    VlcOptionMenu(
        title = title,
        labels = labels,
        selectedIndex = ids.indexOf(selectedId),
        onSelect = { index -> onSelect(ids[index]) },
        onClose = onClose
    )
}

/** A tappable list over the video; tapping outside it closes it. */
@Composable
private fun VlcOptionMenu(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C22))
                .clickable(enabled = false) {}
                .padding(vertical = 12.dp)
        ) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                itemsIndexed(labels, key = { index, _ -> index }) { index, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(index) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (index == selectedIndex) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = ArcAccent)
                    }
                }
            }
        }
    }
}
