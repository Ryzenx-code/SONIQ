# ==============================================================================
# SONIQ High-Fidelity Audio - R8 / ProGuard Shrinking & Obfuscation Rules
# ==============================================================================

# 1. General Optimization & Stack Trace Preservation
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 2. Android Architecture Components & Jetpack Compose
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.activity.ComponentActivity { <init>(); }
-keep class * extends android.app.Service { <init>(); }
-keep class * extends android.app.Application { <init>(); }
-keep class * extends android.content.BroadcastReceiver { <init>(); }

# Jetpack Compose State & Runtime Preservation
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# 3. AndroidX Media3 & ExoPlayer Rules
# Keep Media3 extractors, renderers, decoders, and audio processors
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.decoder.** { *; }

# Allow ExoPlayer dynamic extractor registration via reflection
-keep class * implements androidx.media3.extractor.Extractor {
    <init>();
}
-keep class * implements androidx.media3.exoplayer.audio.AudioRendererEventListener { *; }
-keep class * implements androidx.media3.exoplayer.video.VideoRendererEventListener { *; }
-keep class * implements androidx.media3.exoplayer.Renderer { <init>(...); }

-dontwarn androidx.media3.**

# 4. AndroidX Media & MediaSessionCompat Rules
-keep class android.support.v4.media.** { *; }
-keep class androidx.media.** { *; }
-keep class androidx.media.app.** { *; }
-keepclassmembers class * extends android.support.v4.media.session.MediaSessionCompat$Callback {
    public *;
}
-keep class android.support.v4.media.session.MediaSessionCompat$Token { *; }
-keep class android.support.v4.media.MediaMetadataCompat { *; }
-keep class android.support.v4.media.session.PlaybackStateCompat { *; }

# 5. Domain Models & Telemetry Data Classes
# Preserve data model getters, setters, and constructors
-keep class com.example.soniq.data.** { *; }
-keepclassmembers class com.example.soniq.data.** {
    <fields>;
    <methods>;
}

# Preserve all enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 6. Audio DSP, Synthesizer & AudioTrack Engine
-keep class com.example.soniq.audio.** { *; }
-keepclassmembers class com.example.soniq.audio.** {
    public *;
}

# 7. Playback Services
-keep class com.example.soniq.playback.** { *; }
-keepclassmembers class com.example.soniq.playback.** {
    public *;
}

# 8. BuildConfig & Environment Secrets
-keepclassmembers class com.example.BuildConfig {
    public static final java.lang.String *;
}

# 9. Kotlin Coroutines & Reflection
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
