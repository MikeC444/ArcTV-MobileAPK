package com.mangotv.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mangotv.app.ui.mobile.MobileMetrics
import com.mangotv.app.ui.mobile.WindowClass

/**
 * Layout tokens for touch screens. They follow the window size (see [MobileMetrics]): a phone held upright gets tight margins and small
 * posters, a tablet gets wider margins and bigger posters. Every getter reads snapshot state, so screens re-lay-out on rotation.
 */
object MangoDimens {
    private fun <T> pick(compact: T, medium: T, expanded: T): T = when (MobileMetrics.windowClass) {
        WindowClass.Compact -> compact
        WindowClass.Medium -> medium
        WindowClass.Expanded -> expanded
    }

    val ScreenPaddingHorizontal: Dp get() = pick(16.dp, 24.dp, 32.dp)
    val ScreenPaddingVertical: Dp get() = pick(12.dp, 16.dp, 24.dp)

    /** The slim top bar (logo, profile, settings). */
    val NavBarHeight: Dp get() = pick(56.dp, 60.dp, 64.dp)

    val PosterWidth: Dp get() = pick(112.dp, 136.dp, 160.dp)
    val PosterHeight: Dp get() = pick(168.dp, 204.dp, 240.dp)

    val ContinueWatchingWidth: Dp get() = pick(224.dp, 272.dp, 320.dp)
    val ContinueWatchingHeight: Dp get() = pick(126.dp, 153.dp, 180.dp)

    val CardCornerRadius: Dp get() = 10.dp
    val ButtonCornerRadius: Dp get() = 10.dp

    val RowSpacing: Dp get() = pick(12.dp, 16.dp, 20.dp)
    val CardSpacing: Dp get() = pick(10.dp, 12.dp, 14.dp)

    /** The smallest a finger target may be. */
    val TouchTarget: Dp get() = 48.dp
}
