package com.mangotv.app.ui.torrent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mangotv.app.MangoTvApplication
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.ContentType
import com.mangotv.app.data.provider.ProviderRegistry
import com.mangotv.app.data.torrent.platform.IncomingTorrentInbox
import com.mangotv.app.navigation.MangoRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where "Play this torrent" is: looking for the title, choosing a title's episode, or loading its episodes. */
sealed interface TorrentOpenStep {
    data object PickTitle : TorrentOpenStep
    data class LoadingEpisodes(val show: Content) : TorrentOpenStep
    data class PickEpisode(val show: Content) : TorrentOpenStep
}

/**
 * A magnet link or .torrent file opened from another app has no title. This view model attaches it to the movie, or the episode of a
 * show, the person picks (the same "My torrent" source Select a Source's + Torrent adds) and hands back the player's route.
 */
class TorrentOpenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MangoTvApplication).container.customTorrentRepository

    private val _step = MutableStateFlow<TorrentOpenStep>(TorrentOpenStep.PickTitle)
    val step: StateFlow<TorrentOpenStep> = _step.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** The player's route once the torrent is attached; the screen navigates to it. */
    private val _playRoute = MutableStateFlow<String?>(null)
    val playRoute: StateFlow<String?> = _playRoute.asStateFlow()

    fun choose(content: Content) {
        _error.value = null
        if (content.type == ContentType.MOVIE) {
            attach(content, null, null)
            return
        }
        _step.value = TorrentOpenStep.LoadingEpisodes(content)
        viewModelScope.launch {
            val provider = ProviderRegistry.activeProviders().find { it.id == content.providerId }
            val detail = provider?.let { runCatching { it.getDetails(content.type, content.id) }.getOrNull() }
            if (detail == null || detail.seasons.none { it.episodes.isNotEmpty() }) {
                _error.value = "Couldn't load that show's episodes."
                _step.value = TorrentOpenStep.PickTitle
            } else {
                _step.value = TorrentOpenStep.PickEpisode(detail.copy(providerId = content.providerId))
            }
        }
    }

    fun back() {
        _error.value = null
        _step.value = TorrentOpenStep.PickTitle
    }

    fun attach(content: Content, season: Int?, episode: Int?) {
        val incoming = IncomingTorrentInbox.pending.value ?: return
        val providerId = content.providerId ?: run { _error.value = "This title can't be played."; return }
        val stream = if (incoming.text != null) {
            repository.addText(providerId, content.id, content.type, season, episode, incoming.text) { _error.value = it }
        } else {
            repository.addFile(providerId, content.id, content.type, season, episode, incoming.fileUri ?: return) { _error.value = it }
        } ?: return
        IncomingTorrentInbox.clear()
        _playRoute.value = MangoRoutes.player(providerId, content.type, content.id, season, episode, stream.id)
    }
}
