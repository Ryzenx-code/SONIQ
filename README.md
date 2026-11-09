# SONIQ - Audiophile Universal Media Player for Android

SONIQ is a high-fidelity universal audio and video media player built with modern Android standards: Kotlin, Jetpack Compose, Material 3, and native low-latency audio processing.

## Highlights & Features

- **Sonic Flow Design System**: Dark OLED True-Black continuum with chromatic neon glow accents (Electric Blue, Neon Violet, Vibrant Magenta) and fluid light mode option.
- **Audio Studio DSP Engine**:
  - Direct DAC / Bit-Perfect playback pipeline (up to 32-bit / 384kHz Master & DSD 256).
  - Real-time acoustic spectrum response curve visualizer with live frequency telemetry.
  - 10-band precision rack equalizer (-12dB to +12dB with neon sliders and instant Unity reset).
  - Tactile rotary controls for Dynamic Bass Boost (Sub-Harm) and 3D Surround Virtualizer (Binaural Soundstage).
  - Crystalizer for high-frequency harmonics restoration and ReplayGain leveler.
- **Universal Media Library**:
  - High-density 72dp rows with glowing neon playing indicator, format badges (`FLAC HD`, `DSD 256`, `MQA Master`, `DXD`).
  - Instant text filtering and category tabs (All, Hi-Res Lossless, 4K/8K Video, Favorites, Albums, Artists).
- **Now Playing Experience**:
  - Vinyl album artwork with dynamic ambient chromatic under-glow.
  - Interactive scrubber and waveform seeker with tabular numeric playback counters.
  - Full transport controls (Hero 64dp play/pause, shuffle, cycle repeat).
  - Quick action shortcuts for Equalizer, Synchronized Lyrics, and Play Queue.
- **Knox Private Vault**:
  - Biometric fingerprint passkey and 6-digit Master PIN authentication.
  - AES-256 GCM encrypted sandbox storage for private audio/video master recordings.
  - Secure media import and export.
- **Cinematic 4K/8K Video Player**:
  - Hardware-accelerated viewport with AV1/HEVC Main10 decoding.
  - Multi-stream audio selector (Dolby Atmos 7.1.4, Ambisonic 360, Stereo PCM).
  - Subtitle switcher with live closed captions.
- **Real-Time Sound Synthesis**:
  - Native `AudioTrack` synthesizer that generates harmonically rich ambient audio output during playback, directly shaped by equalizer and effect settings.

## Architecture

- **UI**: Jetpack Compose, Material 3
- **Language**: Kotlin 2.2 with built-in Kotlin support in AGP 9.1
- **Target SDK**: Android 36 (minSdk 26)
- **Audio Architecture**: Custom reactive `SoniqAudioEngine` backed by Kotlin Coroutines and StateFlow
