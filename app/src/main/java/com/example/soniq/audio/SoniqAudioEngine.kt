package com.example.soniq.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.soniq.data.EqualizerPreset
import com.example.soniq.data.MediaItem
import com.example.soniq.data.RepeatMode
import com.example.soniq.data.SampleMediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.sin

@Immutable
data class AudioEngineState(
    val currentTrack: MediaItem = SampleMediaRepository.defaultTracks[0],
    val isPlaying: Boolean = false,
    val durationMs: Long = 312000L,
    val queue: List<MediaItem> = SampleMediaRepository.defaultTracks,
    val queueIndex: Int = 0,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 0.85f,
    val isDspEnabled: Boolean = true,
    val currentPreset: EqualizerPreset = SampleMediaRepository.defaultPresets[0],
    val bandGains: List<Float> = SampleMediaRepository.defaultPresets[0].bandGainsDb,
    val bassBoost: Int = SampleMediaRepository.defaultPresets[0].bassBoostPercent,
    val virtualizer: Int = SampleMediaRepository.defaultPresets[0].virtualizerPercent,
    val crystalizer: Int = SampleMediaRepository.defaultPresets[0].crystalizerPercent,
    val directDacActive: Boolean = true,
    val listeningXp: Int = 0,
    val dailyStreakDays: Int = 0,
    val sleepTimerMinutesLeft: Int? = null,
    val isDarkTheme: Boolean = true
)

class SoniqAudioEngine(
    private val scope: CoroutineScope,
    private val context: android.content.Context? = null
) {
    private val _state = MutableStateFlow(AudioEngineState())
    val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private var playbackTickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        startTicker()
        setupServiceListener()
    }

    private fun setupServiceListener() {
        com.example.soniq.playback.SoniqPlaybackService.actionListener = object : com.example.soniq.playback.SoniqPlaybackService.PlaybackActionListener {
            override fun onPlayRequested() { play() }
            override fun onPauseRequested() { pause() }
            override fun onTogglePlayRequested() { togglePlayPause() }
            override fun onNextRequested() { playNext() }
            override fun onPreviousRequested() { playPrevious() }
            override fun onSeekRequested(positionMs: Long) { seekTo(positionMs) }
            override fun onStopRequested() { pause() }
            override fun onDuckVolumeRequested(duckLevel: Float) {
                audioTrack?.setVolume((_state.value.volume * duckLevel).coerceIn(0f, 1f))
            }
            override fun onRestoreVolumeRequested() {
                audioTrack?.setVolume(_state.value.volume)
            }
        }
    }

    private fun updateServiceState() {
        context?.let { ctx ->
            com.example.soniq.playback.SoniqPlaybackService.syncState(
                ctx,
                _state.value.currentTrack,
                _state.value.isPlaying,
                _positionMs.value
            )
        }
    }

    private fun startTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(200)
                if (_state.value.isPlaying) {
                    val current = _positionMs.value
                    val duration = _state.value.durationMs
                    val speed = _state.value.playbackSpeed
                    val nextPos = current + (200 * speed).toLong()

                    if (duration > 0 && nextPos >= duration) {
                        when (_state.value.repeatMode) {
                            RepeatMode.ONE -> {
                                _positionMs.value = 0L
                            }
                            RepeatMode.ALL -> {
                                playNext(autoAdvance = true)
                            }
                            RepeatMode.OFF -> {
                                if (_state.value.queueIndex < _state.value.queue.size - 1) {
                                    playNext(autoAdvance = true)
                                } else {
                                    pause()
                                    _positionMs.value = duration
                                }
                            }
                        }
                    } else {
                        // Increment XP subtly when listening
                        if (nextPos % 5000 < 200) {
                            _state.update { curr ->
                                curr.copy(listeningXp = (curr.listeningXp + 2).coerceAtMost(3000))
                            }
                        }
                        _positionMs.value = nextPos
                    }
                }
            }
        }
    }

    fun playTrack(track: MediaItem) {
        _positionMs.value = 0L
        _state.update { curr ->
            val q = curr.queue
            val index = q.indexOfFirst { it.id == track.id }
            val (newQueue, newIndex) = if (index >= 0) {
                Pair(q, index)
            } else {
                Pair(q + track, q.size)
            }
            curr.copy(
                currentTrack = track,
                durationMs = track.durationMs,
                queue = newQueue,
                queueIndex = newIndex,
                isPlaying = true
            )
        }
        startAudioSynthesis()
        updateServiceState()
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        _state.update { it.copy(isPlaying = true) }
        startAudioSynthesis()
        updateServiceState()
    }

    fun pause() {
        _state.update { it.copy(isPlaying = false) }
        stopAudioSynthesis()
        updateServiceState()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs)
        _positionMs.value = clamped
        updateServiceState()
    }

    private var shuffledIndices: List<Int> = emptyList()
    private var shufflePointer: Int = 0

    fun playNext(autoAdvance: Boolean = false) {
        val q = _state.value.queue
        if (q.isEmpty()) return
        val nextIndex = if (_state.value.isShuffle) {
            if (shuffledIndices.size != q.size) {
                shuffledIndices = q.indices.shuffled()
                shufflePointer = (shuffledIndices.indexOf(_state.value.queueIndex) + 1) % shuffledIndices.size
            } else {
                shufflePointer = (shufflePointer + 1) % shuffledIndices.size
            }
            shuffledIndices[shufflePointer]
        } else {
            (_state.value.queueIndex + 1) % q.size
        }
        val nextTrack = q[nextIndex]
        _positionMs.value = 0L
        _state.update { curr ->
            curr.copy(
                currentTrack = nextTrack,
                queueIndex = nextIndex,
                durationMs = nextTrack.durationMs,
                isPlaying = true
            )
        }
        startAudioSynthesis()
        updateServiceState()
    }

    fun playPrevious() {
        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }
        val q = _state.value.queue
        if (q.isEmpty()) return
        val prevIndex = if (_state.value.isShuffle) {
            if (shuffledIndices.size != q.size) {
                shuffledIndices = q.indices.shuffled()
                shufflePointer = (shuffledIndices.indexOf(_state.value.queueIndex) - 1 + shuffledIndices.size) % shuffledIndices.size
            } else {
                shufflePointer = (shufflePointer - 1 + shuffledIndices.size) % shuffledIndices.size
            }
            shuffledIndices[shufflePointer]
        } else {
            if (_state.value.queueIndex - 1 < 0) q.size - 1 else _state.value.queueIndex - 1
        }
        val prevTrack = q[prevIndex]
        _positionMs.value = 0L
        _state.update { curr ->
            curr.copy(
                currentTrack = prevTrack,
                queueIndex = prevIndex,
                durationMs = prevTrack.durationMs,
                isPlaying = true
            )
        }
        startAudioSynthesis()
        updateServiceState()
    }

    fun toggleShuffle() {
        _state.update { curr ->
            val newShuffle = !curr.isShuffle
            if (newShuffle) {
                shuffledIndices = curr.queue.indices.shuffled()
                shufflePointer = shuffledIndices.indexOf(curr.queueIndex).coerceAtLeast(0)
            }
            curr.copy(isShuffle = newShuffle)
        }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.update { it.copy(repeatMode = nextMode) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _state.update { it.copy(volume = clamped) }
        audioTrack?.setVolume(clamped)
    }

    fun toggleTheme() {
        _state.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun toggleFavorite(trackId: String) {
        _state.update { curr ->
            val updatedQueue = curr.queue.map {
                if (it.id == trackId) it.copy(isFavorite = !it.isFavorite) else it
            }
            val updatedCurrent = if (curr.currentTrack.id == trackId) {
                curr.currentTrack.copy(isFavorite = !curr.currentTrack.isFavorite)
            } else {
                curr.currentTrack
            }
            curr.copy(
                queue = updatedQueue,
                currentTrack = updatedCurrent
            )
        }
    }

    fun addToQueue(track: MediaItem) {
        _state.update { curr ->
            curr.copy(queue = curr.queue + track)
        }
    }

    fun removeFromQueue(index: Int) {
        _state.update { curr ->
            if (index in curr.queue.indices) {
                val newQueue = curr.queue.toMutableList().apply { removeAt(index) }
                val newIndex = if (curr.queueIndex >= newQueue.size) {
                    (newQueue.size - 1).coerceAtLeast(0)
                } else {
                    curr.queueIndex
                }
                curr.copy(queue = newQueue, queueIndex = newIndex)
            } else {
                curr
            }
        }
    }

    fun clearQueue() {
        _state.update { curr ->
            curr.copy(queue = listOf(curr.currentTrack), queueIndex = 0)
        }
    }

    // DSP and Equalizer Controls
    fun toggleDsp() {
        _state.update { it.copy(isDspEnabled = !it.isDspEnabled) }
    }

    fun selectPreset(preset: EqualizerPreset) {
        _state.update { curr ->
            curr.copy(
                currentPreset = preset,
                bandGains = preset.bandGainsDb,
                bassBoost = preset.bassBoostPercent,
                virtualizer = preset.virtualizerPercent,
                crystalizer = preset.crystalizerPercent
            )
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        _state.update { curr ->
            if (bandIndex in curr.bandGains.indices) {
                val updated = curr.bandGains.toMutableList()
                updated[bandIndex] = gainDb.coerceIn(-12.0f, 12.0f)
                curr.copy(
                    bandGains = updated,
                    currentPreset = curr.currentPreset.copy(name = "Custom Tuning")
                )
            } else {
                curr
            }
        }
    }

    fun resetEqToUnity() {
        val unity = List(10) { 0.0f }
        _state.update { curr ->
            curr.copy(
                bandGains = unity,
                bassBoost = 0,
                virtualizer = 0,
                crystalizer = 0,
                currentPreset = SampleMediaRepository.defaultPresets.find { it.id == "flat" }
                    ?: curr.currentPreset
            )
        }
    }

    fun setBassBoost(percent: Int) {
        _state.update { it.copy(bassBoost = percent.coerceIn(0, 100)) }
    }

    fun setVirtualizer(percent: Int) {
        _state.update { it.copy(virtualizer = percent.coerceIn(0, 100)) }
    }

    fun setCrystalizer(percent: Int) {
        _state.update { it.copy(crystalizer = percent.coerceIn(0, 100)) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _state.update { it.copy(sleepTimerMinutesLeft = minutes) }
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch(Dispatchers.Default) {
                var remaining = minutes
                while (remaining > 0 && isActive) {
                    delay(60000L)
                    remaining -= 1
                    _state.update { it.copy(sleepTimerMinutesLeft = remaining) }
                }
                // Fade out volume and pause without permanently wiping the user's saved volume setting
                val originalVolume = _state.value.volume
                for (v in 10 downTo 0) {
                    audioTrack?.setVolume((v / 10f * originalVolume).coerceIn(0f, 1f))
                    delay(150)
                }
                pause()
                audioTrack?.setVolume(originalVolume)
                _state.update { it.copy(sleepTimerMinutesLeft = null) }
            }
        }
    }

    // Audiophile Sound Synthesizer via AudioTrack
    private fun startAudioSynthesis() {
        stopAudioSynthesis()
        synthesisJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = minBufferSize * 2

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.setVolume(_state.value.volume)
                audioTrack?.play()

                val buffer = ShortArray(bufferSize / 2)
                var phaseL = 0.0
                var phaseR = 0.0
                var subPhase = 0.0

                // Chord notes in Hz depending on current track
                val rootFreq = when (_state.value.currentTrack.id) {
                    "track_1" -> 146.83 // D3
                    "track_2" -> 174.61 // F3
                    "track_3" -> 130.81 // C3
                    "track_4" -> 164.81 // E3
                    else -> 220.00 // A3
                }

                while (isActive && _state.value.isPlaying) {
                    val bassFactor = 1.0 + (_state.value.bassBoost / 100.0) * 0.8
                    val crystalFactor = 1.0 + (_state.value.crystalizer / 100.0) * 0.5

                    val dsp = _state.value.isDspEnabled
                    val lowGain = if (dsp) (1.0 + (_state.value.bandGains.getOrElse(0) { 0f } + _state.value.bandGains.getOrElse(1) { 0f }) / 24.0).coerceIn(0.1, 2.5) else 1.0
                    val midGain = if (dsp) (1.0 + (_state.value.bandGains.getOrElse(4) { 0f } + _state.value.bandGains.getOrElse(5) { 0f }) / 24.0).coerceIn(0.1, 2.5) else 1.0
                    val highGain = if (dsp) (1.0 + (_state.value.bandGains.getOrElse(8) { 0f } + _state.value.bandGains.getOrElse(9) { 0f }) / 24.0).coerceIn(0.1, 2.5) else 1.0

                    for (i in 0 until buffer.size step 2) {
                        val subWave = sin(subPhase) * 0.25 * bassFactor * lowGain
                        val rootWave = sin(phaseL) * 0.35 * midGain
                        val fifthWave = sin(phaseL * 1.5) * 0.20 * crystalFactor * highGain
                        val octaveWave = sin(phaseL * 2.0) * 0.12 * crystalFactor * highGain

                        val sampleL = (subWave + rootWave + fifthWave + octaveWave).coerceIn(-1.0, 1.0)
                        val sampleR = (subWave + sin(phaseR) * 0.35 * midGain + sin(phaseR * 1.5) * 0.20 * crystalFactor * highGain + sin(phaseR * 2.0) * 0.12 * crystalFactor * highGain).coerceIn(-1.0, 1.0)

                        buffer[i] = (sampleL * 32767 * 0.65).toInt().toShort()
                        buffer[i + 1] = (sampleR * 32767 * 0.65).toInt().toShort()

                        val step = (2.0 * PI * rootFreq) / sampleRate
                        phaseL += step
                        phaseR += step * 1.002 // Subtle stereo widening
                        subPhase += (2.0 * PI * (rootFreq * 0.5)) / sampleRate

                        if (phaseL > 2.0 * PI) phaseL -= 2.0 * PI
                        if (phaseR > 2.0 * PI) phaseR -= 2.0 * PI
                        if (subPhase > 2.0 * PI) subPhase -= 2.0 * PI
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
                // Audio synthesis fallback
            }
        }
    }

    private fun stopAudioSynthesis() {
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopAudioSynthesis()
        playbackTickerJob?.cancel()
        sleepTimerJob?.cancel()
    }
}
