package com.mangotv.app.ui.sources

import com.mangotv.app.data.model.Stream

/** The audio types a source can be filtered by (Select a Source's Audio drop-down). */
enum class AudioKind(val label: String) {
    STEREO("Stereo"),
    SURROUND_5_1("5.1"),
    SURROUND_7_1("7.1"),
    ATMOS("Atmos")
}

/** A release's layout bucket: 2 (stereo or mono), 6 (up to 5.1), 8 (7.1 and up), or null when its name doesn't say. */
fun audioBucket(stream: Stream): Int? = stream.audioChannels?.let { if (it <= 2) 2 else if (it <= 6) 6 else 8 }

/** Whether [stream]'s name says it is this kind. A release can be two (an "Atmos" 5.1 release is both). */
fun matchesKind(stream: Stream, kind: AudioKind): Boolean = when (kind) {
    AudioKind.STEREO -> audioBucket(stream) == 2
    AudioKind.SURROUND_5_1 -> audioBucket(stream) == 6
    AudioKind.SURROUND_7_1 -> audioBucket(stream) == 8
    AudioKind.ATMOS -> stream.audioAtmos
}

/** True when the release's name says nothing about its audio. */
fun audioUnlisted(stream: Stream): Boolean = audioBucket(stream) == null && !stream.audioAtmos

/** What the Audio drop-down on Select a Source can be set to. */
sealed interface AudioChoice {
    /** Every source. */
    data object All : AudioChoice

    /** Only sources of this kind. */
    data class OfKind(val kind: AudioKind) : AudioChoice

    /** Only the sources whose name doesn't say. */
    data object Unlisted : AudioChoice
}

/**
 * The drop-down's entries for this title: "all", then each kind that some source of THIS title is (stereo, 5.1, 7.1, Atmos), then
 * "not listed" for the sources that don't say. Empty when no source names its audio (nothing to filter by).
 */
fun audioChoices(streams: List<Stream>): List<AudioChoice> {
    if (streams.all { audioUnlisted(it) }) return emptyList()
    return buildList {
        add(AudioChoice.All)
        for (kind in AudioKind.entries) if (streams.any { matchesKind(it, kind) }) add(AudioChoice.OfKind(kind))
        if (streams.any { audioUnlisted(it) }) add(AudioChoice.Unlisted)
    }
}

/** The sources a choice lists. */
fun applyAudioChoice(streams: List<Stream>, choice: AudioChoice): List<Stream> = when (choice) {
    AudioChoice.All -> streams
    is AudioChoice.OfKind -> streams.filter { matchesKind(it, choice.kind) }
    AudioChoice.Unlisted -> streams.filter { audioUnlisted(it) }
}

/** One row of the drop-down, with how many sources it lists. */
fun audioChoiceLabel(choice: AudioChoice, streams: List<Stream>): String {
    val count = applyAudioChoice(streams, choice).size
    val name = when (choice) {
        AudioChoice.All -> "All audio"
        is AudioChoice.OfKind -> choice.kind.label
        AudioChoice.Unlisted -> "Not listed"
    }
    return "$name ($count)"
}

/** The pill's text: "Audio: 5.1", "Audio: All". */
fun audioButtonLabel(choice: AudioChoice): String = when (choice) {
    AudioChoice.All -> "Audio: All"
    is AudioChoice.OfKind -> "Audio: ${choice.kind.label}"
    AudioChoice.Unlisted -> "Audio: Not listed"
}
