package com.mangotv.app.data.download

import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.provider.ProviderRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Asks every installed addon for the sources of a movie or episode and keeps the downloadable ones ([downloadOptions]). An addon that fails
 * is skipped, so one slow or broken addon never empties the list.
 */
object DownloadFinder {
    suspend fun find(type: ContentType, id: String, season: Int?, episode: Int?): List<DownloadOption> = coroutineScope {
        val found = ProviderRegistry.activeProviders()
            .map { provider ->
                async {
                    try {
                        provider.getStreams(type, id, season, episode)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            .awaitAll()
            .flatten()
        downloadOptions(found)
    }
}
