package com.mangotv.app.ui.sources

import com.mangotv.app.data.model.DebridState
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceOrderingTest {

    private fun stream(id: String, tier: ResolutionTier, seeders: Int? = null, size: Long? = null, debrid: DebridState? = null) = Stream(
        id = id, providerId = "p", providerLabel = "P", resolutionTier = tier, qualityBadge = "", releaseTitle = id,
        seeders = seeders, sizeBytes = size, debrid = debrid
    )

    private val hd = stream("hd", ResolutionTier.HD_720P, seeders = 900)
    private val fhd = stream("fhd", ResolutionTier.FHD_1080P, seeders = 10)
    private val uhd = stream("uhd", ResolutionTier.UHD_4K, seeders = 50)
    private val all = listOf(hd, fhd, uhd)

    private fun ids(streams: List<Stream>) = streams.map { it.id }

    @Test
    fun `quality sort puts the best resolution first then the most seeded`() {
        assertEquals(listOf("uhd", "fhd", "hd"), ids(sortSources(all, SourceSort.QUALITY)))
    }

    @Test
    fun `the recommended source is first whatever the sort`() {
        assertEquals(listOf("hd", "uhd", "fhd"), ids(orderSources(all, all, "hd", SourceSort.QUALITY)))
        assertEquals(listOf("fhd", "hd", "uhd"), ids(orderSources(all, all, "fhd", SourceSort.SEEDERS)))
    }

    @Test
    fun `the recommended source stays first even when the filter would hide it`() {
        val only720 = listOf(hd)
        assertEquals(listOf("uhd", "hd"), ids(orderSources(all, only720, "uhd", SourceSort.QUALITY)))
    }

    @Test
    fun `the recommended source is listed once`() {
        val out = orderSources(all, all, "uhd", SourceSort.SIZE)
        assertEquals(1, out.count { it.id == "uhd" })
    }

    @Test
    fun `with no recommendation the list is just the sorted filter`() {
        assertEquals(listOf("uhd", "fhd", "hd"), ids(orderSources(all, all, null, SourceSort.QUALITY)))
        assertEquals(listOf("uhd", "fhd", "hd"), ids(orderSources(all, all, "gone", SourceSort.QUALITY)))
    }

    private val cached720 = stream("cached720", ResolutionTier.HD_720P, seeders = 10, debrid = DebridState("RD", cached = true))
    private val uncached4k = stream("uncached4k", ResolutionTier.UHD_4K, seeders = 900, debrid = DebridState("RD", cached = false))
    private val plain1080 = stream("plain1080", ResolutionTier.FHD_1080P, seeders = 5)

    @Test
    fun `a source that starts at once beats a sharper one the debrid service still has to fetch`() {
        assertEquals(listOf("plain1080", "cached720", "uncached4k"), ids(sortSources(listOf(uncached4k, cached720, plain1080), SourceSort.QUALITY)))
        assertEquals("plain1080", recommendedStreamId(listOf(uncached4k, cached720, plain1080)))
    }

    @Test
    fun `uncached sources are only recommended when nothing starts at once`() {
        assertEquals("uncached4k", recommendedStreamId(listOf(uncached4k)))
    }

    @Test
    fun `explicit sorts are not changed by the debrid state`() {
        assertEquals(listOf("uncached4k", "cached720", "plain1080"), ids(sortSources(listOf(cached720, plain1080, uncached4k), SourceSort.SEEDERS)))
    }

    @Test
    fun `cache rank is 1 only for an uncached debrid link`() {
        assertEquals(1, cacheRank(uncached4k))
        assertEquals(0, cacheRank(cached720))
        assertEquals(0, cacheRank(plain1080))
    }
}

class DeviceVideoSupportTest {
    private fun stream(id: String, tier: ResolutionTier, codec: String?, title: String = id) = Stream(
        id = id, providerId = "p", providerLabel = "P", resolutionTier = tier, qualityBadge = "", releaseTitle = title, codec = codec
    )

    @org.junit.After
    fun restore() {
        DeviceVideoSupport.decodes = { _, _ -> true }
        DeviceVideoSupport.decodesTenBit = { _ -> true }
    }

    @Test
    fun `sources rank from most to least likely to play on a phone`() {
        DeviceVideoSupport.decodes = { _, _ -> true }
        val h264 = stream("h264", ResolutionTier.FHD_1080P, "H.264")
        val hevc = stream("hevc", ResolutionTier.FHD_1080P, "HEVC")
        val hdr = stream("hdr", ResolutionTier.FHD_1080P, "HEVC", title = "Movie.2160p.HDR.x265")
        val h264Low = stream("h264low", ResolutionTier.HD_720P, "H.264")
        assertEquals(listOf("h264", "h264low", "hevc", "hdr"), sortSources(listOf(hdr, hevc, h264Low, h264), SourceSort.QUALITY).map { it.id })
    }

    @Test
    fun `an HDR source is last when the device has no 10-bit decoder`() {
        DeviceVideoSupport.decodesTenBit = { _ -> false }
        val hdr = stream("hdr", ResolutionTier.FHD_1080P, "HEVC", title = "Movie.1080p.DV.HEVC")
        val plain = stream("plain", ResolutionTier.HD_720P, "HEVC")
        assertEquals(false, DeviceVideoSupport.canPlay(hdr))
        assertEquals("plain", recommendedStreamId(listOf(hdr, plain)))
    }

    @Test
    fun `ten-bit words are spotted`() {
        assertEquals(true, DeviceVideoSupport.needsTenBit(stream("a", ResolutionTier.FHD_1080P, null, "Film 2160p UHD BluRay x265 10bit HDR")))
        assertEquals(true, DeviceVideoSupport.needsTenBit(stream("b", ResolutionTier.FHD_1080P, null, "Film.DV.2160p")))
        assertEquals(false, DeviceVideoSupport.needsTenBit(stream("c", ResolutionTier.FHD_1080P, "H.264", "Film 1080p WEB-DL AAC")))
    }

    @Test
    fun `a 1080p source that plays is recommended over a 4K HEVC one the device cannot decode`() {
        DeviceVideoSupport.decodes = { mime, height -> !(mime == "video/hevc" && height >= 2160) }
        val uhd = stream("uhd", ResolutionTier.UHD_4K, "HEVC")
        val fhd = stream("fhd", ResolutionTier.FHD_1080P, "H.264")
        assertEquals("fhd", recommendedStreamId(listOf(uhd, fhd)))
        assertEquals(listOf("fhd", "uhd"), sortSources(listOf(uhd, fhd), SourceSort.QUALITY).map { it.id })
    }

    @Test
    fun `an unknown codec is assumed playable`() {
        DeviceVideoSupport.decodes = { _, _ -> false }
        assertEquals(true, DeviceVideoSupport.canPlay(stream("x", ResolutionTier.UHD_4K, null)))
        assertEquals(true, DeviceVideoSupport.canPlay(stream("y", ResolutionTier.OTHER, "HEVC")))
        assertEquals(false, DeviceVideoSupport.canPlay(stream("z", ResolutionTier.UHD_4K, "x265")))
    }

    @Test
    fun `codec names map to MIME types`() {
        assertEquals("video/hevc", DeviceVideoSupport.mimeFor("HEVC"))
        assertEquals("video/hevc", DeviceVideoSupport.mimeFor("x265 10bit"))
        assertEquals("video/avc", DeviceVideoSupport.mimeFor("H.264"))
        assertEquals("video/av01", DeviceVideoSupport.mimeFor("AV1"))
        assertEquals(null, DeviceVideoSupport.mimeFor("mystery"))
    }
}
