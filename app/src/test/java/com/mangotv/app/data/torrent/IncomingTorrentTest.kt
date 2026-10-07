package com.mangotv.app.data.torrent

import com.mangotv.app.data.torrent.platform.findTorrentInText
import com.mangotv.app.data.torrent.platform.incomingLabelFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingTorrentTest {
    private val hash = "0123456789abcdef0123456789abcdef01234567"

    @Test
    fun `finds a magnet link inside shared text`() {
        val magnet = "magnet:?xt=urn:btih:$hash&dn=Some+Film"
        assertEquals(magnet, findTorrentInText("Look at this: $magnet thanks"))
    }

    @Test
    fun `finds a link to a torrent file`() {
        assertEquals("https://example.org/a/film.torrent", findTorrentInText("Film https://example.org/a/film.torrent"))
    }

    @Test
    fun `ignores ordinary text, ordinary links and a bare hash`() {
        assertNull(findTorrentInText("hello world"))
        assertNull(findTorrentInText("see https://example.org/page"))
        assertNull(findTorrentInText("id $hash"))
        assertNull(findTorrentInText(null))
    }

    @Test
    fun `labels a magnet by its name and a link by its file name`() {
        assertEquals("Some Film", incomingLabelFor("magnet:?xt=urn:btih:$hash&dn=Some+Film"))
        assertEquals("film.torrent", incomingLabelFor("https://example.org/a/film.torrent?x=1"))
    }
}
