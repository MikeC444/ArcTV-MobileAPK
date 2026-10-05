package com.mangotv.app.data.audio

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Short, low-latency UI feedback: a "nav" tick on focus move, a "click"
 * tone on select, and a distinct "back" tone for leaving a screen (D-pad
 * BACK, or an on-screen Back button -- see TvFocusSurface.ClickSound) --
 * SoundPool rather than MediaPlayer since these fire constantly and can
 * overlap (e.g. a held D-pad scrolling fast through a row), which
 * MediaPlayer isn't built for. All three are loaded eagerly on
 * construction; SoundPool.play() on a sound that hasn't finished loading
 * yet is a silent no-op rather than a crash, so there's no need to gate
 * playback on the load callback for something this small.
 *
 * Volume tracks [SoundPreferencesRepository.preferences].navigationVolume
 * internally (a background collect, not something callers manage) so every
 * play*() call anywhere in the app always uses whatever the user last set
 * in Settings > Sounds, with no caller needing to know volume exists at
 * all.
 */
class UiSoundPlayer(@Suppress("UNUSED_PARAMETER") context: Context, @Suppress("UNUSED_PARAMETER") soundPreferencesRepository: SoundPreferencesRepository) {

    // The phone app has no interface sounds: moving around and tapping are silent. These stay as no-ops so the many call sites
    // (every button, card and menu goes through TvFocusSurface) don't each need changing.
    fun playNav() = Unit

    fun playClick() = Unit

    fun playBack() = Unit
}

/**
 * Lets TvFocusSurface -- the one shared building block behind every
 * focusable card/button/nav item -- reach the app-scoped UiSoundPlayer
 * without threading it through every composable's parameter list in
 * between. Defaults to null (silent) rather than throwing, so any
 * composable previewed or tested outside the real app tree (which provides
 * a real instance from AppContainer in MangoNavHost) still works, just
 * without sound.
 */
val LocalUiSoundPlayer = staticCompositionLocalOf<UiSoundPlayer?> { null }
