package com.example.soniq.data

import kotlin.random.Random

object SampleMediaRepository {

    private fun generateWaveform(seed: Int, size: Int = 48): List<Float> {
        val rnd = Random(seed)
        return List(size) {
            0.15f + rnd.nextFloat() * 0.85f
        }
    }

    val defaultTracks = listOf(
        MediaItem(
            id = "track_1",
            title = "Solar Echoes",
            artist = "Nebula Drift",
            album = "Master Tape V",
            durationMs = 312000L, // 5:12
            codec = AudioCodec.DSD256,
            bitrateKbps = 11289,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = true,
            audioChannels = "Direct DAC DSD",
            genre = "Cosmic Ambient",
            playCount = 184,
            waveformData = generateWaveform(101),
            lyrics = listOf(
                LyricLine(0L, "Floating through the silent void"),
                LyricLine(12000L, "Solar echoes guide our path"),
                LyricLine(28000L, "Harmonics of a thousand stars"),
                LyricLine(45000L, "Resonating in pristine light"),
                LyricLine(68000L, "Cosmic dust beneath the glow"),
                LyricLine(92000L, "Electric signals pulse alive"),
                LyricLine(120000L, "Can you hear the sonic flow?"),
                LyricLine(150000L, "Ultra-high frequencies breathe"),
                LyricLine(190000L, "In the matrix of sound and soul"),
                LyricLine(240000L, "Infinite echoes fade away...")
            )
        ),
        MediaItem(
            id = "track_2",
            title = "Chroma Dream",
            artist = "Ultramarine Echo",
            album = "Prismatic Horizon",
            durationMs = 248000L, // 4:08
            codec = AudioCodec.FLAC_HIRES,
            bitrateKbps = 9216,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = false,
            audioChannels = "2.0 Lossless 192k",
            genre = "Synthwave",
            playCount = 126,
            waveformData = generateWaveform(102),
            lyrics = listOf(
                LyricLine(0L, "Vibrant fluid gradients mix"),
                LyricLine(16000L, "Ultramarine and neon pink"),
                LyricLine(34000L, "Chroma drifting on the wave"),
                LyricLine(58000L, "Every ripple shines today"),
                LyricLine(88000L, "Synthesizer in the deep"),
                LyricLine(120000L, "Waking from a timeless sleep")
            )
        ),
        MediaItem(
            id = "track_3",
            title = "Quantum Resonance",
            artist = "Hyperion & Stellar",
            album = "Direct DAC Sessions",
            durationMs = 365000L, // 6:05
            codec = AudioCodec.DXD,
            bitrateKbps = 18432,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = true,
            audioChannels = "Bit-Perfect 384k",
            genre = "Audiophile Electronic",
            playCount = 92,
            waveformData = generateWaveform(103),
            lyrics = listOf(
                LyricLine(0L, "Subatomic particles chime"),
                LyricLine(22000L, "Zero jitter across space and time"),
                LyricLine(49000L, "Purest converter, true acoustic line"),
                LyricLine(85000L, "32-bit depth where dimensions align"),
                LyricLine(135000L, "High dynamic range untouched and divine")
            )
        ),
        MediaItem(
            id = "track_4",
            title = "Midnight Velocity",
            artist = "Neon Pulse",
            album = "Cyber Drive 2088",
            durationMs = 215000L, // 3:35
            codec = AudioCodec.MQA,
            bitrateKbps = 7680,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = false,
            audioChannels = "MQA Full Unfold",
            genre = "Cyberpunk",
            playCount = 153,
            waveformData = generateWaveform(104),
            lyrics = listOf(
                LyricLine(0L, "Engines purr on midnight streets"),
                LyricLine(14000L, "High-octane synth bass beats"),
                LyricLine(35000L, "Reflections on wet asphalt shine"),
                LyricLine(62000L, "Cruising beyond the redline"),
                LyricLine(95000L, "Velocity unlocks the mind")
            )
        ),
        MediaItem(
            id = "track_5",
            title = "Sub-Zero Ambient",
            artist = "Cryo Void",
            album = "Glacial Soundscape",
            durationMs = 420000L, // 7:00
            codec = AudioCodec.FLAC_HIRES,
            bitrateKbps = 4608,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = false,
            audioChannels = "Wide Soundstage",
            genre = "Cosmic Ambient",
            playCount = 48,
            waveformData = generateWaveform(105),
            lyrics = listOf(
                LyricLine(0L, "Glacial winds whisper soft"),
                LyricLine(30000L, "Frozen crystals held aloft"),
                LyricLine(70000L, "Deep sub bass rumble low"),
                LyricLine(120000L, "Aurora dancing in the snow")
            )
        ),
        MediaItem(
            id = "track_6",
            title = "Starlight Acoustic",
            artist = "Lyra Vance",
            album = "Pure Strings Studio",
            durationMs = 198000L, // 3:18
            codec = AudioCodec.WAV_PCM,
            bitrateKbps = 4608,
            mediaType = MediaType.AUDIO,
            isHiRes = false,
            isFavorite = true,
            audioChannels = "Acoustic Natural",
            genre = "Acoustic",
            playCount = 74,
            waveformData = generateWaveform(106),
            lyrics = listOf(
                LyricLine(0L, "Gentle fingers pick the chord"),
                LyricLine(15000L, "Wooden resonance restored"),
                LyricLine(32000L, "Notes that linger in the air"),
                LyricLine(60000L, "Honest, pure, without compare")
            )
        ),
        MediaItem(
            id = "track_video_1",
            title = "Cyberpunk Odyssey (Director's Cut)",
            artist = "Apex Visuals",
            album = "Neo Tokyo 4K",
            durationMs = 480000L, // 8:00
            codec = AudioCodec.FLAC_HIRES,
            bitrateKbps = 24500,
            mediaType = MediaType.VIDEO,
            isHiRes = true,
            isFavorite = true,
            videoResolution = "4K UHD 60fps HDR10+",
            audioChannels = "Dolby Atmos 7.1.4",
            genre = "Cyberpunk",
            playCount = 65,
            waveformData = generateWaveform(201)
        ),
        MediaItem(
            id = "track_video_2",
            title = "Spatial Ambisonic Dome 8K",
            artist = "SoundScape Matrix",
            album = "Acoustic Architecture",
            durationMs = 310000L, // 5:10
            codec = AudioCodec.DXD,
            bitrateKbps = 48000,
            mediaType = MediaType.VIDEO,
            isHiRes = true,
            isFavorite = false,
            videoResolution = "8K Cinematic 120fps",
            audioChannels = "360 Spatial Ambisonic",
            genre = "Spatial Ambisonic",
            playCount = 31,
            waveformData = generateWaveform(202)
        )
    )

    val vaultTracks = listOf(
        MediaItem(
            id = "vault_1",
            title = "Midnight Master Take (Confidential)",
            artist = "Nebula Drift",
            album = "Private Studio Archive",
            durationMs = 342000L,
            codec = AudioCodec.FLAC_HIRES,
            bitrateKbps = 9216,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = true,
            isPrivateVault = true,
            audioChannels = "AES-256 Encrypted",
            waveformData = generateWaveform(301),
            lyrics = listOf(
                LyricLine(0L, "[Vault Encrypted Session]"),
                LyricLine(15000L, "Unreleased acoustic improvisation"),
                LyricLine(40000L, "Raw dynamic analog take 4")
            )
        ),
        MediaItem(
            id = "vault_2",
            title = "Studio B Ambisonic Session",
            artist = "Apex Labs",
            album = "Classified Stems",
            durationMs = 380000L,
            codec = AudioCodec.DXD,
            bitrateKbps = 18432,
            mediaType = MediaType.VIDEO,
            isHiRes = true,
            isFavorite = false,
            isPrivateVault = true,
            videoResolution = "ProRes 422 HQ 4K",
            audioChannels = "Ambisonic B-Format",
            waveformData = generateWaveform(302)
        ),
        MediaItem(
            id = "vault_3",
            title = "Acoustic Vocal Stem Raw",
            artist = "Lyra Vance",
            album = "Dry Vocal Track",
            durationMs = 210000L,
            codec = AudioCodec.WAV_PCM,
            bitrateKbps = 4608,
            mediaType = MediaType.AUDIO,
            isHiRes = true,
            isFavorite = false,
            isPrivateVault = true,
            audioChannels = "Isolated Dry Stem",
            waveformData = generateWaveform(303)
        )
    )

    val defaultPresets = listOf(
        EqualizerPreset(
            id = "custom_1",
            name = "Custom 1",
            bandGainsDb = listOf(4.5f, 3.0f, 1.5f, 0.0f, -1.0f, 0.0f, 2.0f, 3.5f, 4.0f, 3.5f),
            bassBoostPercent = 65,
            virtualizerPercent = 80,
            crystalizerPercent = 45,
            replayGainDb = -1.5f
        ),
        EqualizerPreset(
            id = "flat",
            name = "Audiophile Flat",
            bandGainsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            bassBoostPercent = 0,
            virtualizerPercent = 0,
            crystalizerPercent = 0,
            replayGainDb = 0.0f
        ),
        EqualizerPreset(
            id = "bass_boost",
            name = "Bass Boost Max",
            bandGainsDb = listOf(7.0f, 6.0f, 4.5f, 2.0f, 0.5f, 0.0f, 0.5f, 1.5f, 2.5f, 3.0f),
            bassBoostPercent = 90,
            virtualizerPercent = 50,
            crystalizerPercent = 30,
            replayGainDb = -2.0f
        ),
        EqualizerPreset(
            id = "electronic",
            name = "Electronic",
            bandGainsDb = listOf(5.0f, 4.0f, 2.0f, 0.0f, -1.5f, 0.5f, 2.5f, 4.0f, 4.5f, 5.0f),
            bassBoostPercent = 75,
            virtualizerPercent = 70,
            crystalizerPercent = 60,
            replayGainDb = -1.5f
        ),
        EqualizerPreset(
            id = "vocal",
            name = "Vocal Clarify",
            bandGainsDb = listOf(-2.0f, -1.0f, 0.0f, 1.5f, 3.5f, 4.5f, 3.5f, 2.0f, 1.0f, 0.0f),
            bassBoostPercent = 20,
            virtualizerPercent = 40,
            crystalizerPercent = 55,
            replayGainDb = -1.0f
        ),
        EqualizerPreset(
            id = "acoustic",
            name = "Acoustic Jazz",
            bandGainsDb = listOf(2.5f, 2.0f, 1.0f, 0.0f, 1.0f, 2.0f, 2.5f, 3.0f, 3.5f, 3.0f),
            bassBoostPercent = 40,
            virtualizerPercent = 60,
            crystalizerPercent = 50,
            replayGainDb = -1.2f
        )
    )
}
