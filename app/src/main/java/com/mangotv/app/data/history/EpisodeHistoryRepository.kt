package com.mangotv.app.data.history

import com.mangotv.app.BuildConfig
import com.mangotv.app.data.auth.AuthRepository
import com.mangotv.app.data.network.PlaybackProgressApiClient
import java.io.IOException

/** The key an episode is tracked under in [EpisodeHistoryRepository.watchedEpisodes]. */
fun episodeKey(season: Int, episode: Int): String = "$season:$episode"

/**
 * Which episodes of a show the signed-in profile has finished, from the account's watch history (GET /user/history, newest first -- one row
 * per episode, `completed` once it was finished or marked watched). Read once when a show's page opens; the page keeps its own changes on
 * top of that.
 */
class EpisodeHistoryRepository(private val authRepository: AuthRepository) {
    private val client = PlaybackProgressApiClient(BuildConfig.API_BASE_URL)

    /** The finished episodes of one show, as [episodeKey]s. Throws [IOException] (or an ApiException) when the history can't be read. */
    suspend fun watchedEpisodes(providerId: String, contentId: String): Set<String> {
        if (!authRepository.ensureFreshSession()) throw IOException("Not signed in")
        val token = authRepository.getCurrentSession()?.accessToken ?: throw IOException("Not signed in")
        val watched = HashSet<String>()
        var before: String? = null
        for (page in 0 until MAX_PAGES) {
            val result = client.getHistory(token, PAGE, before).items
            result.forEach { entry ->
                val season = entry.seasonNumber
                val episode = entry.episodeNumber
                if (entry.completed && entry.providerId == providerId && entry.contentId == contentId && season != null && episode != null) {
                    watched.add(episodeKey(season, episode))
                }
            }
            if (result.size < PAGE) break
            before = result.last().watchedAt
        }
        return watched
    }

    private companion object {
        const val PAGE = 200
        /** A very long history is read up to here (newest first) rather than without end. */
        const val MAX_PAGES = 25
    }
}
