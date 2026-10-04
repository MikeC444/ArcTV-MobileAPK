package com.mangotv.app.ui.mobile

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * While a title plays the phone is turned to landscape and the system bars are hidden (they return for a moment on an edge swipe);
 * everywhere else the app follows however the device is held, with the bars showing.
 */
@Composable
fun PlayerWindowEffect(playing: Boolean) {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(playing) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (playing) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}
