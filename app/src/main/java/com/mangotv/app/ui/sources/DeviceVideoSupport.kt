package com.mangotv.app.ui.sources

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

    fun canPlay(stream: Stream): Boolean {
        val mime = mimeFor(stream.codec) ?: return true
        val height = heightFor(stream.resolutionTier) ?: return true
        return decodes(mime, height)
    }

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
