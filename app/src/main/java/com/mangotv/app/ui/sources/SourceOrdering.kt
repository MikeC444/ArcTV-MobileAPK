package com.mangotv.app.ui.sources

import com.mangotv.app.data.model.Stream

/**
 * 0 = starts at once (cached, or not a debrid link at all); 1 = the debrid service still has to fetch the file first,
 * which can take minutes.
 */
fun cacheRank(stream: Stream): Int = if (stream.debrid != null && !stream.debrid.cached) 1 else 0

/** 0 = this phone's decoders can play the source; 1 = it probably cannot (e.g. 4K HEVC), so it goes after the ones that can. */
fun playRank(stream: Stream): Int = if (DeviceVideoSupport.canPlay(stream)) 0 else 1

/**
 * The "Quality" order: sources that start at once come before ones a debrid service still has to fetch, then ones this
 * device can play before ones it cannot, then the sharpest resolution, then the most seeders. A 720p that is ready beats
 * a 4K that makes you wait minutes, and a 1080p that plays beats a 4K the phone cannot decode.
 */
private val qualityOrder: Comparator<Stream> =
    compareBy<Stream> { cacheRank(it) }
        .thenBy { playRank(it) }
        .thenBy { it.resolutionTier.ordinal }
        .thenByDescending { it.seeders ?: -1 }

/** The source Select a Source marks "Recommended": the first of [streams] in the Quality order. */
fun recommendedStreamId(streams: List<Stream>): String? = streams.sortedWith(qualityOrder).firstOrNull()?.id

/** The order the Select a Source list uses for [sort]; ties keep the order the sources arrived in. */
fun sortSources(streams: List<Stream>, sort: SourceSort): List<Stream> = when (sort) {
    SourceSort.QUALITY -> streams.sortedWith(qualityOrder)
    SourceSort.SEEDERS -> streams.sortedByDescending { it.seeders ?: -1 }
    SourceSort.SIZE -> streams.sortedByDescending { it.sizeBytes ?: -1 }
}

/**
 * The rows of Select a Source: the recommended source first -- always, whatever is filtered out and however the rest
 * is sorted -- then the filtered sources in the chosen order. [all] is every source found; [filtered] is the subset
 * the current filter keeps.
 */
fun orderSources(all: List<Stream>, filtered: List<Stream>, recommendedId: String?, sort: SourceSort): List<Stream> {
    val recommended = recommendedId?.let { id -> all.firstOrNull { it.id == id } }
    val rest = sortSources(filtered, sort).filter { it.id != recommended?.id }
    return if (recommended != null) listOf(recommended) + rest else rest
}
