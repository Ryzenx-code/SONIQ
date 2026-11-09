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
import kotlin.math.PI
import kotlin.math.sin

data class AudioEngineState(
    val currentTrack: MediaItem = SampleMediaRepository.defaultTracks[0],
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 68000L, // 1:08
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
    val listeningXp: Int = 2450,
    val dailyStreakDays: Int = 18,
    val sleepTimerMinutesLeft: Int? = null,
    val isDarkTheme: Boolean = true
)

class SoniqAudioEngine(
    private val scope: CoroutineScope,
    private val context: android.content.Context? = null
) {
    private val _state = MutableStateFlow(AudioEngineState())
    val state: StateFlow<AudioEngineState> = _state.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private var playbackTickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        startTicker()
        setupServiceListener()
    }

    private fun setupServiceListener() {
        val listener = object : com.example.soniq.playback.MediaPlaybackService.PlaybackActionListener {
            override fun onPlayRequested() {
                play()
            }
            override fun onPauseRequested() {
                pause()
            }
            override fun onTogglePlayRequested() {
                togglePlayPause()
            }
            override fun onNextRequested() {
                playNext()
            }
            override fun onPreviousRequested() {
                playPrevious()
            }
            override fun onSeekRequested(positionMs: Long) {
                seekTo(positionMs)
            }
            override fun onStopRequested() {
                pause()
            }
            override fun onDuckVolumeRequested(duckLevel: Float) {
                audioTrack?.setVolume((_state.value.volume * duckLevel).coerceIn(0f, 1f))
            }
            override fun onRestoreVolumeRequested() {
                audioTrack?.setVolume(_state.value.volume)
            }
        }
        com.example.soniq.playback.MediaPlaybackService.actionListener = listener

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
            com.example.soniq.playback.MediaPlaybackService.syncState(
                ctx,
                _state.value.currentTrack,
                _state.value.isPlaying,
                _state.value.currentPositionMs
            )
        }
    }

    private fun startTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(200)
                if (_state.value.isPlaying) {
                    val current = _state.value.currentPositionMs
                    val duration = _state.value.durationMs
                    val speed = _state.value.playbackSpeed
                    val nextPos = current + (200 * speed).toLong()

                    if (nextPos >= duration) {
                        when (_state.value.repeatMode) {
                            RepeatMode.ONE -> {
                                _state.value = _state.value.copy(currentPositionMs = 0L)
                            }
                            RepeatMode.ALL -> {
                                playNext(autoAdvance = true)
                            }
                            RepeatMode.OFF -> {
                                if (_state.value.queueIndex < _state.value.queue.size - 1) {
                                    playNext(autoAdvance = true)
                                } else {
                                    pause()
                                    _state.value = _state.value.copy(currentPositionMs = duration)
                                }
                            }
                        }
                    } else {
                        // Increment XP subtly when listening
                        val newXp = if (nextPos % 5000 < 200) {
                            (_state.value.listeningXp + 2).coerceAtMost(3000)
                        } else {
                            _state.value.listeningXp
                        }
                        _state.value = _state.value.copy(
                            currentPositionMs = nextPos,
                            listeningXp = newXp
                        )
                    }
                }
            }
        }
    }

    fun playTrack(track: MediaItem) {
        val q = _state.value.queue
        val index = q.indexOfFirst { it.id == track.id }
        val newIndex = if (index >= 0) index else 0
        _state.value = _state.value.copy(
            currentTrack = track,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            queueIndex = newIndex,
            isPlaying = true
        )
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
        _state.value = _state.value.copy(isPlaying = true)
        startAudioSynthesis()
        updateServiceState()
    }

    fun pause() {
        _state.value = _state.value.copy(isPlaying = false)
        stopAudioSynthesis()
        updateServiceState()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs)
        _state.value = _state.value.copy(currentPositionMs = clamped)
        updateServiceState()
    }

    fun playNext(autoAdvance: Boolean = false) {
        val q = _state.value.queue
        if (q.isEmpty()) return
        val nextIndex = if (_state.value.isShuffle) {
            val validIndices = q.indices.filter { it != _state.value.queueIndex }
            if (validIndices.isNotEmpty()) validIndices.random() else 0
        } else {
            (_state.value.queueIndex + 1) % q.size
        }
        val nextTrack = q[nextIndex]
        _state.value = _state.value.copy(
            currentTrack = nextTrack,
            queueIndex = nextIndex,
            currentPositionMs = 0L,
            durationMs = nextTrack.durationMs,
            isPlaying = true
        )
        startAudioSynthesis()
        updateServiceState()
    }

    fun playPrevious() {
        if (_state.value.currentPositionMs > 4000L) {
            seekTo(0L)
            return
        }
        val q = _state.value.queue
        if (q.isEmpty()) return
        val prevIndex = if (_state.value.queueIndex - 1 < 0) q.size - 1 else _state.value.queueIndex - 1
        val prevTrack = q[prevIndex]
        _state.value = _state.value.copy(
            currentTrack = prevTrack,
            queueIndex = prevIndex,
            currentPositionMs = 0L,
            durationMs = prevTrack.durationMs,
            isPlaying = true
        )
        startAudioSynthesis()
        updateServiceState()
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(isShuffle = !_state.value.isShuffle)
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        audioTrack?.setVolume(clamped)
    }

    fun toggleTheme() {
        _state.value = _state.value.copy(isDarkTheme = !_state.value.isDarkTheme)
    }

    fun toggleFavorite(trackId: String) {
        val updatedQueue = _state.value.queue.map {
            if (it.id == trackId) it.copy(isFavorite = !it.isFavorite) else it
        }
        val updatedCurrent = if (_state.value.currentTrack.id == trackId) {
            _state.value.currentTrack.copy(isFavorite = !_state.value.currentTrack.isFavorite)
        } else {
            _state.value.currentTrack
        }
        _state.value = _state.value.copy(
            queue = updatedQueue,
            currentTrack = updatedCurrent
        )
    }

    fun addToQueue(track: MediaItem) {
        val newQueue = _state.value.queue.toMutableList().apply { add(track) }
        _state.value = _state.value.copy(queue = newQueue)
    }

    fun removeFromQueue(index: Int) {
        if (index in _state.value.queue.indices) {
            val newQueue = _state.value.queue.toMutableList().apply { removeAt(index) }
            val newIndex = if (_state.value.queueIndex >= newQueue.size) {
                (newQueue.size - 1).coerceAtLeast(0)
            } else {
                _state.value.queueIndex
            }
            _state.value = _state.value.copy(queue = newQueue, queueIndex = newIndex)
        }
    }

    fun clearQueue() {
        val current = _state.value.currentTrack
        _state.value = _state.value.copy(queue = listOf(current), queueIndex = 0)
    }

    // DSP and Equalizer Controls
    fun toggleDsp() {
        _state.value = _state.value.copy(isDspEnabled = !_state.value.isDspEnabled)
    }

    fun selectPreset(preset: EqualizerPreset) {
        _state.value = _state.value.copy(
            currentPreset = preset,
            bandGains = preset.bandGainsDb,
            bassBoost = preset.bassBoostPercent,
            virtualizer = preset.virtualizerPercent,
            crystalizer = preset.crystalizerPercent
        )
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in _state.value.bandGains.indices) {
            val updated = _state.value.bandGains.toMutableList()
            updated[bandIndex] = gainDb.coerceIn(-12.0f, 12.0f)
            _state.value = _state.value.copy(
                bandGains = updated,
                currentPreset = _state.value.currentPreset.copy(name = "Custom Tuning")
            )
        }
    }

    fun resetEqToUnity() {
        val unity = List(10) { 0.0f }
        _state.value = _state.value.copy(
            bandGains = unity,
            bassBoost = 0,
            virtualizer = 0,
            crystalizer = 0,
            currentPreset = SampleMediaRepository.defaultPresets.find { it.id == "flat" }
                ?: _state.value.currentPreset
        )
    }

    fun setBassBoost(percent: Int) {
        _state.value = _state.value.copy(bassBoost = percent.coerceIn(0, 100))
    }

    fun setVirtualizer(percent: Int) {
        _state.value = _state.value.copy(virtualizer = percent.coerceIn(0, 100))
    }

    fun setCrystalizer(percent: Int) {
        _state.value = _state.value.copy(crystalizer = percent.coerceIn(0, 100))
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _state.value = _state.value.copy(sleepTimerMinutesLeft = minutes)
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch(Dispatchers.Default) {
                var remaining = minutes
                while (remaining > 0 && isActive) {
                    delay(60000L)
                    remaining -= 1
                    _state.value = _state.value.copy(sleepTimerMinutesLeft = remaining)
                }
                // Fade out volume and pause
                for (v in 10 downTo 0) {
                    setVolume(v / 10f * _state.value.volume)
                    delay(150)
                }
                pause()
                _state.value = _state.value.copy(sleepTimerMinutesLeft = null)
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
