package com.mangotv.app.data.download

import com.mangotv.app.data.model.DebridState
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadSourcesTest {
    private fun stream(
        id: String,
        url: String? = "https://cdn.example.com/d/$id/Movie.2024.mkv",
        tier: ResolutionTier = ResolutionTier.FHD_1080P,
        size: Long? = 2_000_000_000L,
        title: String = "Movie.2024.1080p.WEB-DL",
        tag: String? = null,
        debrid: DebridState? = null,
        ytId: String? = null,
        infoHash: String? = null
    ) = Stream(
        id = id, providerId = "p", providerLabel = "Addon", resolutionTier = tier, qualityBadge = "x", releaseTitle = title,
        sourceTag = tag, sizeBytes = size, url = url, debrid = debrid, ytId = ytId, infoHash = infoHash
    )

    @Test
    fun `a plain https video link can be downloaded, named after the release with the link's extension`() {
        val info = downloadInfo(stream("a"))
        assertNotNull(info)
        assertEquals("https://cdn.example.com/d/a/Movie.2024.mkv", info!!.url)
        assertEquals("Movie.2024.1080p.WEB-DL.mkv", info.filename)
    }

    @Test
    fun `streams, torrents, YouTube and plain http links are not offered`() {
        assertNull(downloadInfo(stream("m", url = "https://cdn.example.com/x/master.m3u8")))
        assertNull(downloadInfo(stream("d", url = "https://cdn.example.com/x/manifest.mpd")))
        assertNull(downloadInfo(stream("t", url = null, infoHash = "abc")))
        assertNull(downloadInfo(stream("y", url = "https://www.youtube.com/watch?v=1", ytId = "1")))
        assertNull(downloadInfo(stream("h", url = "http://cdn.example.com/x/movie.mkv")))
        assertNull(downloadInfo(stream("n", url = "not a url")))
    }

    @Test
    fun `file names are safe and keep a video extension`() {
        assertEquals("a b c.mp4", suggestedFilename("a/b:c", "/x/file.mp4"))
        assertEquals("Show S01E01.mkv", suggestedFilename("Show S01E01.mkv", "/x/other.mp4"))
        assertEquals("video.mkv", suggestedFilename("", "/x/f.mkv"))
        assertEquals("name", suggestedFilename("name", "/x/noextension"))
        assertEquals("hidden", suggestedFilename("...hidden", "/x/noextension"))
    }

    @Test
    fun `the best quality comes first and, within a quality, the smallest file`() {
        val options = downloadOptions(
            listOf(
                stream("hd-big", tier = ResolutionTier.FHD_1080P, size = 5_000_000_000L),
                stream("uhd", tier = ResolutionTier.UHD_4K, size = 9_000_000_000L),
                stream("hd-small", tier = ResolutionTier.FHD_1080P, size = 1_000_000_000L),
                stream("hd-unknown", tier = ResolutionTier.FHD_1080P, size = null)
            )
        )
        assertEquals(listOf("uhd", "hd-small", "hd-big", "hd-unknown"), options.map { it.stream.id })
    }

    @Test
    fun `stored sources come before ones a debrid service still has to fetch, and recordings go last`() {
        val options = downloadOptions(
            listOf(
                stream("uncached-4k", tier = ResolutionTier.UHD_4K, debrid = DebridState("RD", cached = false)),
                stream("cam-4k", tier = ResolutionTier.UHD_4K, tag = "CAM"),
                stream("cached-720", tier = ResolutionTier.HD_720P, debrid = DebridState("RD", cached = true))
            )
        )
        assertEquals(listOf("cached-720", "cam-4k", "uncached-4k"), options.map { it.stream.id })
    }

    @Test
    fun `the same link offered twice is listed once`() {
        val options = downloadOptions(listOf(stream("a", url = "https://cdn.example.com/same.mkv"), stream("b", url = "https://cdn.example.com/same.mkv")))
        assertEquals(listOf("a"), options.map { it.stream.id })
    }

    @Test
    fun `the saved file gets a matching video type`() {
        assertEquals("video/x-matroska", FileDownloader.mimeTypeFor("a.mkv"))
        assertEquals("video/mp4", FileDownloader.mimeTypeFor("a.mp4"))
        assertEquals("video/mp4", FileDownloader.mimeTypeFor("noextension"))
    }
}
