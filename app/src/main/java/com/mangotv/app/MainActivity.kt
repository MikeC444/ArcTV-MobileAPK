package com.mangotv.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mangotv.app.navigation.MangoNavHost
import com.mangotv.app.ui.theme.MangoTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full screen, like a video app: content runs behind the status / navigation bars (and a camera cut-out), which come back
        // for a moment when you swipe from the edge.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()
        setContent {
            MangoTvTheme {
                TvLayoutScale {
                    MangoNavHost()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Every screen's sizes were drawn for a 960 dp wide TV. A phone is narrower (about 800 dp in landscape) and a tablet wider, so the
 * density is scaled to make the screen's long side come out as 960 "TV dp": the layouts then look the way they were designed on any
 * device, instead of being cramped on a phone or tiny on a tablet. Text scales with it (the user's own font size is kept).
 */
@Composable
private fun TvLayoutScale(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val base = LocalDensity.current
    val longSideDp = maxOf(config.screenWidthDp, config.screenHeightDp)
    val scale = (longSideDp / REFERENCE_WIDTH_DP).coerceIn(MIN_SCALE, MAX_SCALE)
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
        content()
    }
}

private const val REFERENCE_WIDTH_DP = 960f
private const val MIN_SCALE = 0.7f
private const val MAX_SCALE = 1.4f
