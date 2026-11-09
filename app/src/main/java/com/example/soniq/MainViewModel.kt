package com.example.soniq

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.soniq.audio.AudioEngineState
import com.example.soniq.audio.SoniqAudioEngine
import com.example.soniq.data.EqualizerPreset
import com.example.soniq.data.MediaItem
import com.example.soniq.data.MediaType
import com.example.soniq.data.SoniqTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.runtime.Immutable

@Immutable
data class MainUiState(
    val selectedTab: SoniqTab = SoniqTab.HOME,
    val isNowPlayingExpanded: Boolean = false,
    val activeVideoItem: MediaItem? = null,
    val isQueueSheetOpen: Boolean = false,
    val isLyricsSheetOpen: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val audioEngine = SoniqAudioEngine(viewModelScope, application)
    val engineState: StateFlow<AudioEngineState> = audioEngine.state
    val positionMs: StateFlow<Long> = audioEngine.positionMs

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun selectTab(tab: SoniqTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            isNowPlayingExpanded = false,
            activeVideoItem = null,
            isQueueSheetOpen = false,
            isLyricsSheetOpen = false
        )
    }

    fun playTrack(track: MediaItem) {
        if (track.mediaType == MediaType.VIDEO) {
            audioEngine.pause()
            _uiState.value = _uiState.value.copy(
                activeVideoItem = track,
                isNowPlayingExpanded = false
            )
        } else {
            _uiState.value = _uiState.value.copy(activeVideoItem = null)
            audioEngine.playTrack(track)
        }
    }

    fun expandNowPlaying() {
        _uiState.value = _uiState.value.copy(
            isNowPlayingExpanded = true,
            activeVideoItem = null
        )
    }

    fun collapseNowPlaying() {
        _uiState.value = _uiState.value.copy(isNowPlayingExpanded = false)
    }

    fun closeVideoPlayer() {
        _uiState.value = _uiState.value.copy(activeVideoItem = null)
    }

    fun openQueueSheet() {
        _uiState.value = _uiState.value.copy(isQueueSheetOpen = true)
    }

    fun closeQueueSheet() {
        _uiState.value = _uiState.value.copy(isQueueSheetOpen = false)
    }

    fun openLyricsSheet() {
        _uiState.value = _uiState.value.copy(isLyricsSheetOpen = true)
    }

    fun closeLyricsSheet() {
        _uiState.value = _uiState.value.copy(isLyricsSheetOpen = false)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
