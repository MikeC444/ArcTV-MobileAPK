package com.mangotv.app.ui.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration

/** How much room the window has: a phone held upright is Compact, a phone on its side or a small tablet Medium, a big tablet Expanded. */
enum class WindowClass {
    Compact, Medium, Expanded;

    companion object {
        fun forWidthDp(widthDp: Int): WindowClass = when {
            widthDp < 600 -> Compact
            widthDp < 840 -> Medium
            else -> Expanded
        }
    }
}

/**
 * The current window width, kept in snapshot state so plain (non-composable) getters such as `MangoDimens.PosterWidth` re-read it and every
 * screen re-lays-out when the phone rotates or the window is resized, without each screen having to pass a size down.
 */
object MobileMetrics {
    private var widthDpState by mutableIntStateOf(400)

    val widthDp: Int get() = widthDpState
    val windowClass: WindowClass get() = WindowClass.forWidthDp(widthDpState)
    val isCompact: Boolean get() = windowClass == WindowClass.Compact

    internal fun update(widthDp: Int) {
        if (widthDpState != widthDp) widthDpState = widthDp
    }
}

/** Reads the window width from the configuration and publishes it to [MobileMetrics]. Call once, above the app's content. */
@Composable
fun ProvideMobileMetrics(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    // Set before the children compose so their first frame already uses the right sizes.
    MobileMetrics.update(widthDp)
    content()
}
