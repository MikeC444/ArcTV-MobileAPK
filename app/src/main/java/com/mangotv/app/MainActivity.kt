package com.mangotv.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.mangotv.app.data.torrent.platform.IncomingResult
import com.mangotv.app.data.torrent.platform.IncomingTorrentInbox
import com.mangotv.app.data.torrent.platform.readIncomingTorrent
import com.mangotv.app.navigation.MangoNavHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.mangotv.app.ui.components.StartupSplash
import com.mangotv.app.ui.components.StartupSplashState
import com.mangotv.app.ui.mobile.ProvideMobileMetrics
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
        // Light status-bar icons on the dark app.
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            MangoTvTheme {
                ProvideMobileMetrics {
                    // The app loads behind the splash (a black screen with the logo) at a cold start, then it fades away.
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().onPreviewKeyEvent { StartupSplashState.active }) {
                        MangoNavHost()
                        StartupSplash()
                    }
                }
            }
        }
        if (savedInstanceState == null) handleIncoming(intent)
    }

    // Already running (singleTop): a magnet link or .torrent file opened or shared from another app arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    /** A magnet link or .torrent file from another app goes to the "Play this torrent" screen, which asks which title it is for. */
    private fun handleIncoming(intent: Intent?) {
        if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        val app = applicationContext
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { readIncomingTorrent(app, intent) }
            when (result) {
                is IncomingResult.Found -> IncomingTorrentInbox.offer(result.torrent)
                is IncomingResult.Refused -> Toast.makeText(app, result.message, Toast.LENGTH_LONG).show()
                IncomingResult.Ignore -> Unit
            }
        }
    }
}
