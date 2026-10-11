package com.mangotv.app.data.download

import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream

private val REMUX = Regex("remux", RegexOption.IGNORE_CASE)
private val BLURAY = Regex("blu-?ray|bdrip|brrip|\\bbd(?:25|50)?\\b", RegexOption.IGNORE_CASE)
private val WEB = Regex("web-?dl|web-?rip|\\bweb\\b", RegexOption.IGNORE_CASE)
private val HDTV = Regex("hdtv", RegexOption.IGNORE_CASE)
private val DVD = Regex("dvd", RegexOption.IGNORE_CASE)
private val CAMERA = Regex("\\b(?:cam|hdcam|ts|hdts|telesync)\\b", RegexOption.IGNORE_CASE)
private val DOLBY_VISION = Regex("dolby[ ._-]?vision|\\bdovi\\b|\\bDV\\b", RegexOption.IGNORE_CASE)
private val HDR = Regex("\\bhdr(?:10\\+?)?\\b", RegexOption.IGNORE_CASE)

/** What kind of release a download is, in plain words ("UHD Blu-ray REMUX", "4K WEB-DL", "Blu-ray"), worked out from the release's name. Same as the web app. */
fun sourceLabel(stream: Stream): String {
    val text = "${stream.releaseTitle} ${stream.sourceTag.orEmpty()}"
    val uhd = stream.resolutionTier == ResolutionTier.UHD_4K
    return when {
        REMUX.containsMatchIn(text) -> (if (uhd) "UHD " else "") + "Blu-ray REMUX"
        BLURAY.containsMatchIn(text) -> (if (uhd) "UHD " else "") + "Blu-ray"
        WEB.containsMatchIn(text) -> (if (uhd) "4K " else "") + "WEB-DL"
        HDTV.containsMatchIn(text) -> "HDTV"
        DVD.containsMatchIn(text) -> "DVD"
        CAMERA.containsMatchIn(text) -> "Camera recording"
        else -> stream.qualityBadge
    }
}

/** "Dolby Vision" or "HDR" when the release's name says so, else null. */
fun hdrLabel(stream: Stream): String? {
    val text = stream.releaseTitle
    return when {
        DOLBY_VISION.containsMatchIn(text) -> "Dolby Vision"
        HDR.containsMatchIn(text) -> "HDR"
        else -> null
    }
}

/** A 1080p or 4K file under this size is almost never a real copy (a stub, a sample or a mislabelled file), so the list says so. */
const val UNUSUALLY_SMALL_BYTES = 150_000_000L

fun isUnusuallySmall(stream: Stream): Boolean {
    val size = stream.sizeBytes ?: return false
    return (stream.resolutionTier == ResolutionTier.UHD_4K || stream.resolutionTier == ResolutionTier.FHD_1080P) && size < UNUSUALLY_SMALL_BYTES
}

/** The resolutions present in [options], best first, for the list's filter chips (a tier nobody offers gets no chip). */
fun qualityFilters(options: List<DownloadOption>): List<ResolutionTier> =
    ResolutionTier.entries.filter { tier -> options.any { it.stream.resolutionTier == tier } }

/** Applies the list's filters: one resolution (null for all) and, optionally, only files a debrid service already has stored. */
fun filterOptions(options: List<DownloadOption>, tier: ResolutionTier?, cachedOnly: Boolean): List<DownloadOption> =
    options.filter { (tier == null || it.stream.resolutionTier == tier) && (!cachedOnly || it.stream.debrid?.cached == true) }
