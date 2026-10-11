package com.mangotv.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mangotv.app.ui.player.DevicePlayerPrefs
import com.mangotv.app.ui.player.SubtitleColor
import com.mangotv.app.ui.player.SubtitleSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.ui.components.TvFocusSurface
import com.mangotv.app.ui.theme.ArcAccent
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

/** One selectable entry in the Default Language list — [code] is an ISO 639-1 code, or null for "no preference" (System Default). */
data class SubtitleLanguageOption(val code: String?, val label: String)

/**
 * Fixed, hand-picked list rather than every Locale on the device: this is
 * a subtitle-track *preference* hint (see PlayerEngine.buildExoPlayer's
 * setPreferredTextLanguage), not an exhaustive locale picker — a short,
 * scannable list matters more here than completeness. "System Default"
 * (null) leaves ExoPlayer's own default track selection alone, i.e.
 * whatever it would already do with no preference set.
 */
val SubtitleLanguageOptions: List<SubtitleLanguageOption> = listOf(
    SubtitleLanguageOption(null, "System Default"),
    SubtitleLanguageOption("en", "English"),
    SubtitleLanguageOption("es", "Spanish"),
    SubtitleLanguageOption("fr", "French"),
    SubtitleLanguageOption("de", "German"),
    SubtitleLanguageOption("it", "Italian"),
    SubtitleLanguageOption("pt", "Portuguese"),
    SubtitleLanguageOption("nl", "Dutch"),
    SubtitleLanguageOption("sv", "Swedish"),
    SubtitleLanguageOption("pl", "Polish"),
    SubtitleLanguageOption("tr", "Turkish"),
    SubtitleLanguageOption("ar", "Arabic"),
    SubtitleLanguageOption("hi", "Hindi"),
    SubtitleLanguageOption("ja", "Japanese"),
    SubtitleLanguageOption("ko", "Korean"),
    SubtitleLanguageOption("zh", "Chinese")
)

/**
 * Settings > Subtitles: a default on/off for new playback sessions (off
 * means every video starts with subtitles off, regardless of what the
 * stream itself would otherwise auto-select) plus a preferred language
 * used when it's on. Both are just starting points for a session — the
 * in-player Subtitles menu can still override either one once a video is
 * actually playing (see PlayerEngine.buildExoPlayer's own kdoc).
 *
 * A ColumnScope extension hosted by SettingsScreen's detail pane -- see
 * AccountSettingsContent's kdoc for why this isn't its own screen anymore.
 *
 * The toggle row, header, and language options are all items of one
 * LazyColumn (see the comment on it below for why), with the toggle row
 * pinned first -- contentFocusRequester/navFocusRequester/sidebarFocusRequester
 * are wired onto it, not any language row, so this is safe the same way
 * HomeRowsSettingsContent's own first-item wiring is safe (see its kdoc):
 * only the selected category is ever composed at all, so switching into
 * this tab always recomposes fresh at scroll position 0, guaranteeing the
 * toggle row (item 0) is present the moment focus could land on it. The
 * language rows themselves still get no FocusRequester of their own (unlike
 * e.g. GenresScreen's genre list) -- pinning one to a row that can scroll
 * out of composition is what actually crashes (see GenresScreen/HomeRowsScreen's
 * own fix for that failure), and nothing here needs a guaranteed seam
 * beyond the one the toggle row already provides.
 */
@Composable
fun ColumnScope.SubtitleSettingsContent(
    navFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    sidebarFocusRequester: FocusRequester,
    viewModel: SubtitleSettingsViewModel = viewModel()
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    // Size and colour belong to this device (see DevicePlayerPrefs) and take effect the next time a video starts.
    val context = LocalContext.current
    var size by remember { mutableStateOf(DevicePlayerPrefs.subtitleSize(context)) }
    var color by remember { mutableStateOf(DevicePlayerPrefs.subtitleColor(context)) }

    // The toggle row, "Default Language" header, and the language list are
    // all items in this one LazyColumn (rather than a static header above a
    // separately-scrolling list) so the whole tab scrolls as a unit -- the
    // header scrolls away with everything else instead of permanently
    // reserving space at the top, leaving more of the screen for language
    // rows once scrolled.
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        // LazyColumn clips to its own bounds, and each row below fills
        // its full width with no margin of its own -- so a row's focus
        // scale-up (TvFocusSurface) had nowhere to grow into and got
        // clipped flush against the list's left/right edges. Same fix
        // as TopNavBar/SourcesScreen/SoundSettingsScreen: reserve a
        // little headroom via contentPadding for the scale to grow into.
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item(key = "toggle") {
            SubtitlesToggleRow(
                enabled = preferences.subtitlesEnabled,
                onToggle = viewModel::setSubtitlesEnabled,
                focusRequester = contentFocusRequester,
                focusUp = navFocusRequester,
                focusLeft = sidebarFocusRequester
            )
        }
        item(key = "style_preview") {
            SubtitlePreview(size = size, color = color)
        }
        item(key = "size_title") {
            Text(
                text = "Size",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        items(SubtitleSize.entries, key = { "size_${it.wire}" }) { option ->
            LanguageOptionRow(
                label = option.label,
                selected = option == size,
                onClick = {
                    size = option
                    DevicePlayerPrefs.setSubtitleSize(context, option)
                }
            )
        }
        item(key = "color_title") {
            Text(
                text = "Colour",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        item(key = "color_description") {
            Text(
                text = "Applies from the next video you start, on this device.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(SubtitleColor.entries, key = { "color_${it.wire}" }) { option ->
            LanguageOptionRow(
                label = option.label,
                selected = option == color,
                onClick = {
                    color = option
                    DevicePlayerPrefs.setSubtitleColor(context, option)
                }
            )
        }
        item(key = "language_title") {
            Text(
                text = "Default Language",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        item(key = "language_description") {
            Text(
                text = "Used to automatically pick a matching subtitle track when Subtitles is on.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(SubtitleLanguageOptions, key = { it.code ?: "system_default" }) { option ->
            LanguageOptionRow(
                label = option.label,
                selected = option.code == preferences.defaultSubtitleLanguage,
                onClick = { viewModel.setDefaultSubtitleLanguage(option.code) }
            )
        }
    }
}

/** A line of sample subtitle text on a dark picture, at the chosen size and colour, so the choice can be judged before playing something. */
@Composable
private fun SubtitlePreview(size: SubtitleSize, color: SubtitleColor) {
    // A mock 16:9 video frame. The text is sized as a fraction of the frame's
    // height -- the same proportion the real player uses -- so what you see
    // here matches playback, with a scene behind it to judge it against.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(MangoDimens.CardCornerRadius))
            .background(Color(0xFF05060A))
    ) {
        val density = LocalDensity.current
        val textSize = with(density) { (constraints.maxHeight * size.fraction).toSp() }
        // An original deep-space scene (a black hole with a glowing ring and a small
        // ship), drawn in code, so there is a busy picture to judge subtitles against.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val c = Offset(w * 0.5f, h * 0.42f)
            val r = h * 0.2f
            // Stars.
            val stars = listOf(
                0.08f to 0.12f, 0.18f to 0.55f, 0.27f to 0.2f, 0.36f to 0.7f, 0.62f to 0.1f,
                0.72f to 0.62f, 0.82f to 0.25f, 0.91f to 0.5f, 0.12f to 0.85f, 0.88f to 0.82f,
                0.45f to 0.06f, 0.55f to 0.92f
            )
            stars.forEach { (x, y) -> drawCircle(Color(0xCCFFFFFF), 1.6f, Offset(w * x, h * y)) }
            // Soft glow around the hole.
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0x66FFB347), Color(0x00FFB347)),
                    center = c,
                    radius = r * 3.2f
                ),
                r * 3.2f,
                c
            )
            // Bright ring seen edge-on, passing behind and in front of the hole.
            drawOval(
                Brush.horizontalGradient(
                    listOf(Color(0x00FFC46B), Color(0xFFFFE2A8), Color(0x00FFC46B)),
                    startX = c.x - r * 3f,
                    endX = c.x + r * 3f
                ),
                topLeft = Offset(c.x - r * 3f, c.y - r * 0.28f),
                size = androidx.compose.ui.geometry.Size(r * 6f, r * 0.56f)
            )
            // The hole itself, with a thin lit rim.
            drawCircle(Color(0xFFFFD08A), r * 1.04f, c)
            drawCircle(Color.Black, r, c)
            // Front half of the ring crossing the hole.
            drawOval(
                Color(0xCCFFE2A8),
                topLeft = Offset(c.x - r * 1.9f, c.y + r * 0.02f),
                size = androidx.compose.ui.geometry.Size(r * 3.8f, r * 0.2f)
            )
            // A small ship drifting in the foreground.
            val sx = w * 0.2f
            val sy = h * 0.62f
            drawRoundRect(
                Color(0xFFCBD2DA),
                topLeft = Offset(sx, sy),
                size = androidx.compose.ui.geometry.Size(w * 0.09f, h * 0.035f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.017f)
            )
            drawRect(
                Color(0xFF8A939E),
                topLeft = Offset(sx + w * 0.03f, sy - h * 0.03f),
                size = androidx.compose.ui.geometry.Size(w * 0.03f, h * 0.03f)
            )
        }
        Text(
            text = "I told you we'd make it\nbefore the sun goes down.",
            color = Color(color.argb),
            fontSize = textSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f))
        )
    }
}

@Composable
private fun SubtitlesToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    focusRequester: FocusRequester? = null,
    focusUp: FocusRequester? = null,
    focusLeft: FocusRequester? = null
) {
    TvFocusSurface(
        onClick = { onToggle(!enabled) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MangoDimens.CardCornerRadius),
        // Same wide-element-safe scale as Home Rows'/Genres' full-width rows.
        focusedScale = 1.02f,
        backgroundColor = MangoSurface,
        borderColor = TextPrimary,
        focusRequester = focusRequester,
        focusUp = focusUp,
        focusLeft = focusLeft
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Subtitles",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = ArcAccent)
            )
        }
    }
}

@Composable
internal fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    focusUp: FocusRequester? = null,
    focusLeft: FocusRequester? = null
) {
    TvFocusSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MangoDimens.CardCornerRadius),
        focusedScale = 1.02f,
        backgroundColor = MangoSurface,
        borderColor = TextPrimary,
        focusRequester = focusRequester,
        focusUp = focusUp,
        focusLeft = focusLeft
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Was 14.dp -- with 16 languages to scroll through via
                // D-pad, a shorter row means more of the list is visible
                // at once without shrinking the tap target unreasonably.
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = ArcAccent)
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
