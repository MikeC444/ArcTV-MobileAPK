package com.mangotv.app.data.torrent.platform

import android.content.Context
import android.net.ConnectivityManager

/**
 * Whether the phone is on a connection that is paid for by the amount: mobile data, or Wi-Fi marked as metered (a hotspot, a capped plan).
 * A torrent downloads (and uploads) far more than an ordinary stream, so the player asks before starting one there. False when it can't tell.
 */
fun isOnMeteredNetwork(context: Context): Boolean = try {
    val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    manager.isActiveNetworkMetered
} catch (e: Exception) {
    false
}
