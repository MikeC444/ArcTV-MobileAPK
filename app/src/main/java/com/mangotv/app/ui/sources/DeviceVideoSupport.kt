package com.mangotv.app.ui.sources

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream

/**
 * Whether this phone's own video decoders can play a source. A phone often cannot decode what a TV box can: a 4K HEVC
 * (H.265) remux fails with "Decoder failed ... NO_EXCEEDS_CAPABILITIES" on many of them (and on the emulator), so such a
 * source must not be the one the app recommends. Anything it cannot tell is assumed playable.
 */
object DeviceVideoSupport {
    /** Replaced in tests; given a video MIME type and a height, says whether the device decodes that. */
    internal var decodes: (mime: String, height: Int) -> Boolean = ::deviceDecodes

    /** Replaced in tests; says whether the device decodes 10-bit video of the given MIME type (what HDR / Dolby Vision / "10bit" releases need). */
    internal var decodesTenBit: (mime: String) -> Boolean = ::deviceDecodesTenBit

    fun canPlay(stream: Stream): Boolean {
        val mime = effectiveMime(stream) ?: return true
        val height = heightFor(stream.resolutionTier)
        if (height != null && !decodes(mime, height)) return false
        if (needsTenBit(stream) && !decodesTenBit(mime)) return false
        return true
    }

    /**
     * How likely a source is to play on a phone, 0 = the safest: plain H.264 (every phone decodes it), 1 = a newer codec
     * (HEVC, AV1, VP9) that most but not all phones decode, 2 = 10-bit / HDR / Dolby Vision, which is the first thing weaker
     * decoders trip over. A source the device is known not to decode is ranked separately (see playRank).
     */
    fun likelihoodTier(stream: Stream): Int {
        val mime = effectiveMime(stream)
        val tier = if (mime == null || mime == "video/avc") 0 else 1
        return if (needsTenBit(stream)) 2 else tier
    }

    private val tenBitWords = Regex("(10[ -]?bit|hi10|hdr|dolby[ .-]?vision|\\bdv\\b|\\bdovi\\b)", RegexOption.IGNORE_CASE)

    internal fun needsTenBit(stream: Stream): Boolean =
        tenBitWords.containsMatchIn(listOfNotNull(stream.releaseTitle, stream.sourceTag, stream.codec).joinToString(" "))

    /**
     * The video format of a source: the codec the addon's text names, else the one a release name gives away (x265, H.265,
     * x264 ...), else HEVC when the release is Dolby Vision / HDR / 10-bit or 4K, because nearly every such release is HEVC
     * even when it never says so. Null only when nothing points to a format (then the source is assumed playable).
     */
    internal fun effectiveMime(stream: Stream): String? {
        mimeFor(stream.codec)?.let { return it }
        mimeInTitle(stream.releaseTitle)?.let { return it }
        return if (needsTenBit(stream) || stream.resolutionTier == ResolutionTier.UHD_4K) "video/hevc" else null
    }

    private val titleCodecs = listOf(
        Regex("\\b(x|h)[ .]?265\\b|\\bhevc\\b", RegexOption.IGNORE_CASE) to "video/hevc",
        Regex("\\bav1\\b", RegexOption.IGNORE_CASE) to "video/av01",
        Regex("\\b(x|h)[ .]?264\\b|\\bavc\\b", RegexOption.IGNORE_CASE) to "video/avc"
    )

    private fun mimeInTitle(title: String): String? = titleCodecs.firstOrNull { (re, _) -> re.containsMatchIn(title) }?.second

    internal fun mimeFor(codec: String?): String? {
        val c = codec?.lowercase() ?: return null
        return when {
            "265" in c || "hevc" in c -> "video/hevc"
            "av1" in c -> "video/av01"
            "vp9" in c -> "video/x-vnd.on2.vp9"
            "264" in c || "avc" in c -> "video/avc"
            else -> null
        }
    }

    internal fun heightFor(tier: ResolutionTier): Int? = when (tier) {
        ResolutionTier.UHD_4K -> 2160
        ResolutionTier.FHD_1080P -> 1080
        ResolutionTier.HD_720P -> 720
        ResolutionTier.OTHER -> null
    }

    private val cache = HashMap<Pair<String, Int>, Boolean>()
    private val tenBitCache = HashMap<String, Boolean>()

    private fun deviceDecodesTenBit(mime: String): Boolean = synchronized(tenBitCache) {
        tenBitCache.getOrPut(mime) {
            runCatching {
                val tenBit = when (mime) {
                    "video/hevc" -> intArrayOf(MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10)
                    "video/av01" -> intArrayOf(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10)
                    "video/x-vnd.on2.vp9" -> intArrayOf(MediaCodecInfo.CodecProfileLevel.VP9Profile2)
                    else -> return@runCatching true // a 10-bit H.264 release is rare; leave it to the other checks
                }
                val infos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                infos.any { info ->
                    !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                        info.getCapabilitiesForType(mime).profileLevels.any { it.profile in tenBit }
                }
            }.getOrDefault(true)
        }
    }

    private fun deviceDecodes(mime: String, height: Int): Boolean = synchronized(cache) {
        cache.getOrPut(mime to height) {
            runCatching {
                val width = height * 16 / 9
                val infos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                infos.any { info ->
                    !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                        info.getCapabilitiesForType(mime).videoCapabilities.areSizeAndRateSupported(width, height, 24.0)
                }
            }.getOrDefault(true)
        }
    }
}
