package com.mangotv.app.data.torrent.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.mangotv.app.data.torrent.MagnetLink
import com.mangotv.app.data.torrent.UserTorrentInput
import com.mangotv.app.data.torrent.parseUserTorrentInput
import com.mangotv.app.data.torrent.readTorrentFileBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.MessageDigest

/**
 * A magnet link or .torrent file another app handed to Arc TV (opened, or shared). It has no title yet: the "Play this torrent" screen
 * asks which movie or episode it is for. Exactly one of [text] (a magnet link or the address of a .torrent file) and [fileUri] (a copy of the
 * .torrent file in the app's own cache) is set; [label] is what the person is shown.
 */
data class IncomingTorrent(val text: String?, val fileUri: Uri?, val label: String)

/** Holds the torrent waiting for the person to say which title it is for. App-wide, so it survives the screen being recreated. */
object IncomingTorrentInbox {
    private val _pending = MutableStateFlow<IncomingTorrent?>(null)
    val pending: StateFlow<IncomingTorrent?> = _pending.asStateFlow()

    fun offer(torrent: IncomingTorrent) { _pending.value = torrent }
    fun clear() { _pending.value = null }
}

/** What reading an incoming intent found. */
sealed interface IncomingResult {
    data class Found(val torrent: IncomingTorrent) : IncomingResult
    /** It was meant as a torrent but isn't a usable one; [message] says why. */
    data class Refused(val message: String) : IncomingResult
    /** Nothing to do with torrents (the normal launch, for one). */
    data object Ignore : IncomingResult
}

/** The first magnet link, or link to a .torrent file, in some shared text (a browser shares "Title https://..." as often as the bare link). */
fun findTorrentInText(text: String?): String? {
    if (text.isNullOrBlank()) return null
    return text.split(Regex("\\s+")).map { it.trim('"', '\'', '<', '>', '(', ')') }.firstOrNull { token ->
        parseUserTorrentInput(token) !is UserTorrentInput.Rejected && MagnetLink.normalizeInfoHash(token) == null
    }
}

/** What to show for a received link: the magnet's own name, or the end of the address. */
fun incomingLabelFor(text: String): String =
    MagnetLink.parse(text)?.displayName?.takeIf { it.isNotBlank() }
        ?: text.substringBefore('?').substringAfterLast('/').takeIf { it.isNotBlank() }
        ?: "Magnet link"

/** Reads what an intent carries. The .torrent file itself is copied into the cache straight away, before the sender's permission to read it can lapse. */
fun readIncomingTorrent(context: Context, intent: Intent): IncomingResult {
    val notATorrent = "That isn't a magnet link or a .torrent file."
    return when (intent.action) {
        Intent.ACTION_VIEW -> {
            val data = intent.data ?: return IncomingResult.Ignore
            when (data.scheme?.lowercase()) {
                "magnet", "http", "https" -> fromText(data.toString())
                "content", "file" -> fromFile(context, data)
                else -> IncomingResult.Refused(notATorrent)
            }
        }
        Intent.ACTION_SEND -> {
            @Suppress("DEPRECATION")
            val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (stream != null) fromFile(context, stream)
            else findTorrentInText(intent.getStringExtra(Intent.EXTRA_TEXT))?.let { fromText(it) } ?: IncomingResult.Refused(notATorrent)
        }
        else -> IncomingResult.Ignore
    }
}

private fun fromText(text: String): IncomingResult = when (val input = parseUserTorrentInput(text)) {
    is UserTorrentInput.Rejected -> IncomingResult.Refused(input.message)
    else -> IncomingResult.Found(IncomingTorrent(text = text.trim(), fileUri = null, label = incomingLabelFor(text.trim())))
}

private fun fromFile(context: Context, uri: Uri): IncomingResult = try {
    val bytes = context.contentResolver.openInputStream(uri)?.use { readTorrentFileBytes(it) }
        ?: return IncomingResult.Refused("Couldn't open that file.")
    val digest = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
    val dir = File(context.cacheDir, "incoming").apply { mkdirs() }
    val copy = File(dir, "$digest.torrent")
    if (!copy.exists()) copy.writeBytes(bytes)
    IncomingResult.Found(IncomingTorrent(text = null, fileUri = Uri.fromFile(copy), label = displayNameOf(context, uri) ?: "Torrent file"))
} catch (e: com.mangotv.app.data.torrent.TorrentStreamException) {
    IncomingResult.Refused(e.message ?: "That isn't a valid .torrent file.")
} catch (e: Exception) {
    IncomingResult.Refused("Couldn't open that file.")
}

private fun displayNameOf(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
} catch (_: Exception) {
    null
} ?: uri.lastPathSegment
