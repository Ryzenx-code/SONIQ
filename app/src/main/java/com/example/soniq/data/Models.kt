package com.example.soniq.data

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class AudioCodec(
    val label: String,
    val bitDepth: String,
    val sampleRate: String,
    val isMaster: Boolean = false
) {
    DSD256("DSD 256", "1-bit", "11.289 MHz", true),
    FLAC_HIRES("FLAC HD", "24-bit", "192 kHz", true),
    MQA("MQA Master", "24-bit", "352.8 kHz", true),
    DXD("DXD Master", "32-bit", "384 kHz", true),
    FLAC_STD("FLAC", "16-bit", "44.1 kHz", false),
    WAV_PCM("WAV PCM", "24-bit", "96 kHz", false),
    AAC_HD("AAC 320", "16-bit", "48 kHz", false)
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val codec: AudioCodec,
    val bitrateKbps: Int,
    val mediaType: MediaType = MediaType.AUDIO,
    val isHiRes: Boolean = true,
    val isFavorite: Boolean = false,
    val isPrivateVault: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val videoResolution: String = "",
    val audioChannels: String = "2.0 Stereo",
    val waveformData: List<Float> = emptyList(),
    val genre: String = "Electronic",
    val playCount: Int = 0,
    val releaseYear: Int = 2026
)

data class EqualizerPreset(
    val id: String,
    val name: String,
    val bandGainsDb: List<Float>, // 10 bands: 31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val crystalizerPercent: Int = 0,
    val replayGainDb: Float = -1.5f
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SoniqTab {
    HOME,
    LIBRARY,
    AUDIO_STUDIO,
    VAULT
}
