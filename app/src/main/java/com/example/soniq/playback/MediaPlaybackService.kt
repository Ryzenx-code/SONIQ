package com.example.soniq.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.exoplayer.ExoPlayer
import com.example.soniq.MainActivity
import com.example.soniq.data.MediaItem as SoniqMediaItem
import com.example.soniq.data.SampleMediaRepository

/**
 * Robust Foreground Media Playback Service for SONIQ.
 * Built with:
 * - AndroidX MediaSessionCompat for universal media and lock screen controls
 * - NotificationCompat.Builder with MediaStyle for notification shade controls
 * - AudioManagerCompat & AudioFocusRequestCompat for safe audio focus management and ducking
 * - BroadcastReceiver for ACTION_AUDIO_BECOMING_NOISY (headphone disconnects)
 * - AndroidX Media3 ExoPlayer integration for robust audio streaming and local playback
 * - Android 14+ (API 34+) compliant FOREGROUND_SERVICE_MEDIA_PLAYBACK implementation
 */
class MediaPlaybackService : Service() {

    private val binder = LocalBinder()
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequestCompat? = null
    private var exoPlayer: ExoPlayer? = null

    private var hasAudioFocus = false
    private var wasPlayingBeforeTransientLoss = false

    private var currentTrack: SoniqMediaItem = SampleMediaRepository.defaultTracks[0]
    private var isPlaying: Boolean = false
    private var currentPositionMs: Long = 0L

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pause playback automatically when headphones unplug or Bluetooth drops
                actionListener?.onPauseRequested()
                pausePlayback()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
        fun getExoPlayer(): ExoPlayer? = exoPlayer
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        createNotificationChannel()
        setupMediaSessionCompat()
        setupExoPlayer()

        // Immediate startForeground call to satisfy Android 8+ foreground service contract
        val initialNotification = buildMediaNotification(currentTrack, false)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        } catch (_: Exception) {
            // Guard against system execution limits
        }
    }

    private fun setupExoPlayer() {
        try {
            exoPlayer = ExoPlayer.Builder(this)
                .setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .build(),
                    false // Manage focus via AudioManagerCompat below
                )
                .setHandleAudioBecomingNoisy(false)
                .build()
        } catch (_: Exception) {
            // Fallback gracefully if hardware codec is uninitialized
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SONIQ Master Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hi-Res Master Playback & Lock Screen Transport Controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSessionCompat() {
        mediaSession = MediaSessionCompat(this, "SoniqMediaSessionCompat").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    actionListener?.onPlayRequested()
                    startPlayback()
                }

                override fun onPause() {
                    actionListener?.onPauseRequested()
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    actionListener?.onNextRequested()
                }

                override fun onSkipToPrevious() {
                    actionListener?.onPreviousRequested()
                }

                override fun onSeekTo(pos: Long) {
                    currentPositionMs = pos
                    actionListener?.onSeekRequested(pos)
                    exoPlayer?.seekTo(pos)
                    updatePlaybackState(currentTrack, isPlaying, pos)
                }

                override fun onStop() {
                    actionListener?.onStopRequested()
                    stopPlayback()
                }
            })
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        // Extract track parameters
        val title = intent?.getStringExtra(EXTRA_TRACK_TITLE) ?: currentTrack.title
        val artist = intent?.getStringExtra(EXTRA_TRACK_ARTIST) ?: currentTrack.artist
        val album = intent?.getStringExtra(EXTRA_TRACK_ALBUM) ?: currentTrack.album
        val duration = intent?.getLongExtra(EXTRA_TRACK_DURATION, currentTrack.durationMs) ?: currentTrack.durationMs
        val playing = intent?.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying) ?: isPlaying
        val position = intent?.getLongExtra(EXTRA_POSITION_MS, currentPositionMs) ?: currentPositionMs

        currentTrack = currentTrack.copy(
            title = title,
            artist = artist,
            album = album,
            durationMs = duration
        )
        isPlaying = playing
        currentPositionMs = position

        if (action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }

        // Audio Focus & Noisy Receiver
        if (playing) {
            requestAudioFocus()
            registerNoisyReceiver()
        } else {
            unregisterNoisyReceiver()
        }

        // Update MediaSessionCompat
        updateMediaSessionCompat(currentTrack, playing, position)

        // Build & start foreground notification
        val notification = buildMediaNotification(currentTrack, playing)
        try {
            if (playing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
            }
        } catch (_: Exception) {}

        // Handle transport actions
        when (action) {
            ACTION_PLAY -> {
                actionListener?.onPlayRequested()
                startPlayback()
            }
            ACTION_PAUSE -> {
                actionListener?.onPauseRequested()
                pausePlayback()
            }
            ACTION_TOGGLE_PLAY -> {
                actionListener?.onTogglePlayRequested()
                if (isPlaying) pausePlayback() else startPlayback()
            }
            ACTION_NEXT -> actionListener?.onNextRequested()
            ACTION_PREVIOUS -> actionListener?.onPreviousRequested()
        }

        return START_NOT_STICKY
    }

    private fun startPlayback() {
        isPlaying = true
        requestAudioFocus()
        registerNoisyReceiver()
        updatePlaybackState(currentTrack, true, currentPositionMs)
    }

    private fun pausePlayback() {
        isPlaying = false
        unregisterNoisyReceiver()
        updatePlaybackState(currentTrack, false, currentPositionMs)
    }

    private fun stopPlayback() {
        isPlaying = false
        unregisterNoisyReceiver()
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    fun updatePlaybackState(track: SoniqMediaItem, playing: Boolean, positionMs: Long) {
        this.currentTrack = track
        this.isPlaying = playing
        this.currentPositionMs = positionMs

        updateMediaSessionCompat(track, playing, positionMs)

        val notification = buildMediaNotification(track, playing)
        try {
            if (playing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateMediaSessionCompat(track: SoniqMediaItem, playing: Boolean, positionMs: Long) {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                positionMs,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, createDynamicAlbumArt(track.title))

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun buildMediaNotification(track: SoniqMediaItem, playing: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val togglePlayIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionToken = mediaSession?.sessionToken

        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }

        val playPauseIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (playing) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.codec.label}")
            .setSubText(track.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(createDynamicAlbumArt(track.title))
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(playing)
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Previous", prevIntent))
            .addAction(NotificationCompat.Action(playPauseIcon, playPauseText, togglePlayIntent))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Next", nextIntent))
            .setStyle(mediaStyle)
            .build()
    }

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        val am = audioManager ?: return

        val attributes = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .build()

        audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .build()

        val res = AudioManagerCompat.requestAudioFocus(am, audioFocusRequest!!)
        hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeTransientLoss = false
                actionListener?.onPauseRequested()
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeTransientLoss = isPlaying
                actionListener?.onPauseRequested()
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                actionListener?.onDuckVolumeRequested(0.25f)
                exoPlayer?.volume = 0.25f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                actionListener?.onRestoreVolumeRequested()
                exoPlayer?.volume = 1.0f
                if (wasPlayingBeforeTransientLoss) {
                    wasPlayingBeforeTransientLoss = false
                    actionListener?.onPlayRequested()
                    startPlayback()
                }
            }
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        audioFocusRequest?.let {
            AudioManagerCompat.abandonAudioFocusRequest(am, it)
        }
        hasAudioFocus = false
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                registerReceiver(
                    becomingNoisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isNoisyReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
                isNoisyReceiverRegistered = false
        }
    }

    private fun createDynamicAlbumArt(title: String): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 24, 38)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // OLED Neon Accent Circle
        paint.color = Color.rgb(0, 229, 255)
        canvas.drawCircle(size / 2f, size / 2f, 80f, paint)

        paint.color = Color.rgb(11, 15, 23)
        canvas.drawCircle(size / 2f, size / 2f, 40f, paint)

        return bitmap
    }

    override fun onDestroy() {
        unregisterNoisyReceiver()
        abandonAudioFocus()
        exoPlayer?.release()
        exoPlayer = null
        mediaSession?.apply {
            isActive = false
            release()
        }
        super.onDestroy()
    }

    interface PlaybackActionListener {
        fun onPlayRequested()
        fun onPauseRequested()
        fun onTogglePlayRequested()
        fun onNextRequested()
        fun onPreviousRequested()
        fun onSeekRequested(positionMs: Long)
        fun onStopRequested()
        fun onDuckVolumeRequested(duckLevel: Float) {}
        fun onRestoreVolumeRequested() {}
    }

    companion object {
        const val NOTIFICATION_ID = 405
        const val CHANNEL_ID = "soniq_master_playback_channel"

        const val ACTION_PLAY = "com.example.soniq.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.soniq.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.example.soniq.ACTION_TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.soniq.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.soniq.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.soniq.ACTION_STOP"
        const val ACTION_UPDATE_STATE = "com.example.soniq.ACTION_UPDATE_STATE"
        const val ACTION_PAUSE_STATE = "com.example.soniq.ACTION_PAUSE_STATE"

        const val EXTRA_TRACK_ID = "extra_track_id"
        const val EXTRA_TRACK_TITLE = "extra_track_title"
        const val EXTRA_TRACK_ARTIST = "extra_track_artist"
        const val EXTRA_TRACK_ALBUM = "extra_track_album"
        const val EXTRA_TRACK_DURATION = "extra_track_duration"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_POSITION_MS = "extra_position_ms"

        var actionListener: PlaybackActionListener? = null

        fun syncState(context: Context, track: SoniqMediaItem, isPlaying: Boolean, positionMs: Long) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = if (isPlaying) ACTION_UPDATE_STATE else ACTION_PAUSE_STATE
                    putExtra(EXTRA_TRACK_ID, track.id)
                    putExtra(EXTRA_TRACK_TITLE, track.title)
                    putExtra(EXTRA_TRACK_ARTIST, track.artist)
                    putExtra(EXTRA_TRACK_ALBUM, track.album)
                    putExtra(EXTRA_TRACK_DURATION, track.durationMs)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                    putExtra(EXTRA_POSITION_MS, positionMs)
                }

                if (isPlaying) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Safeguard against OS background limits or cold-launch restrictions
            }
        }
    }
}
