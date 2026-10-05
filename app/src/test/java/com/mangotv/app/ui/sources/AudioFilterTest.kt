package com.mangotv.app.ui.sources

import com.mangotv.app.data.addon.detectAtmos
import com.mangotv.app.data.addon.detectAudioChannels
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFilterTest {

    private fun stream(id: String, channels: Int? = null, atmos: Boolean = false) = Stream(
        id = id, providerId = "p", providerLabel = "P", resolutionTier = ResolutionTier.FHD_1080P, qualityBadge = "1080p",
        releaseTitle = id, audioChannels = channels, audioAtmos = atmos
    )

    private val stereo = stream("stereo", 2)
    private val fiveOne = stream("five", 6)
    private val sevenOne = stream("seven", 8)
    private val atmos = stream("atmos", 8, atmos = true)
    private val unlisted = stream("unlisted")
    private val all = listOf(stereo, fiveOne, sevenOne, atmos, unlisted)

    private fun ids(streams: List<Stream>) = streams.map { it.id }

    @Test
    fun `release names say their layout`() {
        assertEquals(6, detectAudioChannels("Movie.2024.1080p.WEB-DL.DDP5.1.H264"))
        assertEquals(8, detectAudioChannels("Movie 2160p DTS-HD.MA.7.1"))
        assertEquals(2, detectAudioChannels("Movie 720p AAC2.0"))
        assertEquals(8, detectAudioChannels("Movie 2160p TrueHD Atmos"))
        assertNull(detectAudioChannels("Movie 1080p BluRay x264"))
        assertTrue(detectAtmos("DDP5.1.Atmos"))
        assertFalse(detectAtmos("DDP5.1"))
    }

    @Test
    fun `choices list only the kinds this title has`() {
        val choices = audioChoices(all)
        assertEquals(
            listOf(AudioChoice.All, AudioChoice.OfKind(AudioKind.STEREO), AudioChoice.OfKind(AudioKind.SURROUND_5_1), AudioChoice.OfKind(AudioKind.SURROUND_7_1), AudioChoice.OfKind(AudioKind.ATMOS), AudioChoice.Unlisted),
            choices
        )
    }

    @Test
    fun `no choices when no source names its audio`() {
        assertTrue(audioChoices(listOf(unlisted, stream("also"))).isEmpty())
    }

    @Test
    fun `a choice lists the sources of that kind`() {
        assertEquals(listOf("five"), ids(applyAudioChoice(all, AudioChoice.OfKind(AudioKind.SURROUND_5_1))))
        assertEquals(listOf("atmos"), ids(applyAudioChoice(all, AudioChoice.OfKind(AudioKind.ATMOS))))
        assertEquals(listOf("seven", "atmos"), ids(applyAudioChoice(all, AudioChoice.OfKind(AudioKind.SURROUND_7_1))))
        assertEquals(listOf("unlisted"), ids(applyAudioChoice(all, AudioChoice.Unlisted)))
        assertEquals(ids(all), ids(applyAudioChoice(all, AudioChoice.All)))
    }

    @Test
    fun `labels carry the count`() {
        assertEquals("5.1 (1)", audioChoiceLabel(AudioChoice.OfKind(AudioKind.SURROUND_5_1), all))
        assertEquals("Audio: Atmos", audioButtonLabel(AudioChoice.OfKind(AudioKind.ATMOS)))
        assertEquals("Audio: All", audioButtonLabel(AudioChoice.All))
    }
}
