package com.mangotv.app.data.download

import com.mangotv.app.data.model.Stream
import java.net.URI

/** What a Download button needs: the file's address and a name to save it under. */
data class DownloadInfo(val url: String, val filename: String)

/** A source that can be saved as a file, with the link and file name to save it under. */
data class DownloadOption(val stream: Stream, val download: DownloadInfo)

private val STREAMING_MANIFEST = Regex("\\.(m3u8|mpd)$", RegexOption.IGNORE_CASE)
private val VIDEO_EXTENSION = Regex("\\.(mp4|mkv|m4v|mov|avi|webm|ts)$", RegexOption.IGNORE_CASE)
// A camera or telesync recording: tiny and poor, so it would otherwise win "smallest file" within its resolution.
private val RECORDING_TAG = Regex("^(cam|hdcam|ts|hdts|telesync)$", RegexOption.IGNORE_CASE)

/**
 * The file behind a source, when it is simply downloadable: a plain https link to one video file. Not offered for streaming manifests (HLS and
 * DASH are many small pieces, not a file), for torrent-only or YouTube sources, or for plain http links. Same rules as the web app's Download.
 */
fun downloadInfo(stream: Stream): DownloadInfo? {
    val url = stream.url?.trim().orEmpty()
    if (url.isEmpty() || stream.ytId != null) return null
    val parsed = runCatching { URI(url) }.getOrNull() ?: return null
    if (!"https".equals(parsed.scheme, ignoreCase = true) || parsed.host.isNullOrEmpty()) return null
    val path = parsed.path.orEmpty()
    if (STREAMING_MANIFEST.containsMatchIn(path)) return null
    return DownloadInfo(url, suggestedFilename(stream.releaseTitle, path))
}

/** A safe file name from the release's title, ending in the link's own video extension when it has one. */
fun suggestedFilename(releaseTitle: String, path: String): String {
    val base = releaseTitle
        .map { if (it.code < 32) ' ' else it }
        .joinToString("")
        .replace(Regex("[\\\\/:*?\"<>|]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimStart('.')
        .take(120)
        .trim()
    val name = base.ifEmpty { "video" }
    if (VIDEO_EXTENSION.containsMatchIn(name)) return name
    val decoded = runCatching { java.net.URLDecoder.decode(path, "UTF-8") }.getOrDefault(path)
    val ext = VIDEO_EXTENSION.find(decoded)?.value.orEmpty()
    return name + ext
}

/**
 * The sources among [streams] that can be downloaded, in the order the list shows them, with no sort control: the highest quality first and,
 * within the same quality, the smallest file first (a file with no size listed comes last in its group). Two things sit above quality: ones a
 * debrid service has already stored come before ones it still has to fetch (those can take minutes to start), and camera recordings go to
 * the bottom. The same file offered twice (the same link) is listed once.
 */
fun downloadOptions(streams: List<Stream>): List<DownloadOption> {
    val seen = HashSet<String>()
    val options = ArrayList<DownloadOption>()
    for (stream in streams) {
        val download = downloadInfo(stream) ?: continue
        if (!seen.add(download.url)) continue
        options.add(DownloadOption(stream, download))
    }
    return options.sortedWith(
        compareBy<DownloadOption> { if (it.stream.debrid?.cached == false) 1 else 0 }
            .thenBy { if (RECORDING_TAG.matches(it.stream.sourceTag.orEmpty())) 1 else 0 }
            .thenBy { it.stream.resolutionTier.ordinal }
            .thenBy { it.stream.sizeBytes ?: Long.MAX_VALUE }
    )
}
