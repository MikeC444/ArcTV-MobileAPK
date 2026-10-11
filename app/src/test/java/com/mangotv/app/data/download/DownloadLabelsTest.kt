package com.mangotv.app.data.download

import com.mangotv.app.data.model.DebridState
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLabelsTest {
    private fun stream(
        title: String,
        tier: ResolutionTier = ResolutionTier.FHD_1080P,
        size: Long? = 2_000_000_000L,
        tag: String? = null,
        badge: String = "1080p",
        debrid: DebridState? = null,
        id: String = title
    ) = Stream(
        id = id, providerId = "p", providerLabel = "A", resolutionTier = tier, qualityBadge = badge, releaseTitle = title,
        sourceTag = tag, sizeBytes = size, url = "https://cdn.example.com/$id.mkv", debrid = debrid
    )

    @Test
    fun `a release is named by its kind, with 4K spelled out`() {
        assertEquals("UHD Blu-ray REMUX", sourceLabel(stream("Movie.2160p.UHD.BluRay.REMUX", ResolutionTier.UHD_4K)))
        assertEquals("Blu-ray", sourceLabel(stream("Movie.1080p.BluRay.x264")))
        assertEquals("4K WEB-DL", sourceLabel(stream("Movie.2160p.WEB-DL.H265", ResolutionTier.UHD_4K)))
        assertEquals("WEB-DL", sourceLabel(stream("Movie.1080p.WEBRip")))
        assertEquals("HDTV", sourceLabel(stream("Movie 1080p HDTV")))
        assertEquals("Camera recording", sourceLabel(stream("Movie.2024.HDCAM")))
        assertEquals("720p", sourceLabel(stream("Movie", ResolutionTier.HD_720P, badge = "720p")))
    }

    @Test
    fun `Dolby Vision and HDR are read from the name`() {
        assertEquals("Dolby Vision", hdrLabel(stream("Movie.2160p.DV.HDR10")))
        assertEquals("Dolby Vision", hdrLabel(stream("Movie Dolby Vision")))
        assertEquals("HDR", hdrLabel(stream("Movie.2160p.HDR10.HEVC")))
        assertNull(hdrLabel(stream("Movie.1080p.x264")))
    }

    @Test
    fun `a tiny 1080p or 4K file is called unusually small, other files are not`() {
        assertTrue(isUnusuallySmall(stream("a", ResolutionTier.UHD_4K, size = 12_000_000L)))
        assertTrue(isUnusuallySmall(stream("b", ResolutionTier.FHD_1080P, size = 100_000_000L)))
        assertFalse(isUnusuallySmall(stream("c", ResolutionTier.FHD_1080P, size = 1_470_000_000L)))
        assertFalse(isUnusuallySmall(stream("d", ResolutionTier.HD_720P, size = 50_000_000L)))
        assertFalse(isUnusuallySmall(stream("e", ResolutionTier.UHD_4K, size = null)))
    }

    @Test
    fun `filters offer only the resolutions present, and narrow the list`() {
        val options = downloadOptions(
            listOf(
                stream("a", ResolutionTier.UHD_4K, debrid = DebridState("TB", cached = true)),
                stream("b", ResolutionTier.FHD_1080P, debrid = DebridState("TB", cached = false)),
                stream("c", ResolutionTier.FHD_1080P, debrid = DebridState("TB", cached = true))
            )
        )
        assertEquals(listOf(ResolutionTier.UHD_4K, ResolutionTier.FHD_1080P), qualityFilters(options))
        assertEquals(3, filterOptions(options, null, cachedOnly = false).size)
        assertEquals(listOf("c"), filterOptions(options, ResolutionTier.FHD_1080P, cachedOnly = true).map { it.stream.id })
        assertEquals(listOf("a"), filterOptions(options, ResolutionTier.UHD_4K, cachedOnly = false).map { it.stream.id })
    }
}
