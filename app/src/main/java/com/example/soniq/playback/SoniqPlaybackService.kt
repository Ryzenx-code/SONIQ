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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.example.soniq.MainActivity
import com.example.soniq.data.MediaItem
import com.example.soniq.data.SampleMediaRepository

/**
 * Robust Foreground Media Playback Service for SONIQ.
 * Integrates:
 * - Immediate startForeground() invocation complying with Android 8+ foreground service requirements
 * - Android MediaSession for system & lock screen media controls
 * - Android Notification.MediaStyle with action buttons
 * - Audio focus management (ducking, transient loss, and gain)
 * - Headphone disconnect detection (ACTION_AUDIO_BECOMING_NOISY)
 * - Safe background execution conforming to API 34+ FOREGROUND_SERVICE_MEDIA_PLAYBACK
 */
class SoniqPlaybackService : Service() {

    private val binder = LocalBinder()
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var wasPlayingBeforeTransientLoss = false

    private var currentTrack: MediaItem = SampleMediaRepository.defaultTracks[0]
    private var isPlaying: Boolean = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Headphone unplugged or Bluetooth disconnected -> safely pause playback
                actionListener?.onPauseRequested()
            }
        }
    }
    private var isNoisyReceiverRegistered = false

    inner class LocalBinder : Binder() {
        fun getService(): SoniqPlaybackService = this@SoniqPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        createNotificationChannel()
        setupMediaSession()

        // Immediate startForeground call to satisfy Android 8+ foreground service contract
        val initialNotification = buildNotification(currentTrack, false)
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
            // Guard against system security / foreground restrictions
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SONIQ High-Fidelity Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls and track metadata for active audio playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "SoniqMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    actionListener?.onPlayRequested()
                }

                override fun onPause() {
                    actionListener?.onPauseRequested()
                }

                override fun onSkipToNext() {
                    actionListener?.onNextRequested()
                }

                override fun onSkipToPrevious() {
                    actionListener?.onPreviousRequested()
                }

                override fun onStop() {
                    actionListener?.onStopRequested()
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    actionListener?.onSeekRequested(pos)
                }
            })
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        // Extract track details from intent extras or fallback
        val title = intent?.getStringExtra(EXTRA_TRACK_TITLE) ?: currentTrack.title
        val artist = intent?.getStringExtra(EXTRA_TRACK_ARTIST) ?: currentTrack.artist
        val album = intent?.getStringExtra(EXTRA_TRACK_ALBUM) ?: currentTrack.album
        val duration = intent?.getLongExtra(EXTRA_TRACK_DURATION, currentTrack.durationMs) ?: currentTrack.durationMs
        val playing = intent?.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying) ?: isPlaying
        val position = intent?.getLongExtra(EXTRA_POSITION_MS, 0L) ?: 0L

        currentTrack = currentTrack.copy(
            title = title,
            artist = artist,
            album = album,
            durationMs = duration
        )
        isPlaying = playing

        if (action == ACTION_STOP) {
            actionListener?.onStopRequested()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        // Handle Audio Focus
        if (playing) {
            requestAudioFocus()
            registerNoisyReceiver()
        } else {
            unregisterNoisyReceiver()
        }

        // Update MediaSession State
        updateMediaSession(currentTrack, playing, position)

        // Build & immediately post foreground notification
        val notification = buildNotification(currentTrack, playing)
        try {
            if (playing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                // If paused, ensure startForeground was called to satisfy startForegroundService, then detach
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

        // Handle interactive transport action intents
        when (action) {
            ACTION_PLAY -> actionListener?.onPlayRequested()
            ACTION_PAUSE -> actionListener?.onPauseRequested()
            ACTION_TOGGLE_PLAY -> actionListener?.onTogglePlayRequested()
            ACTION_NEXT -> actionListener?.onNextRequested()
            ACTION_PREVIOUS -> actionListener?.onPreviousRequested()
        }

        return START_NOT_STICKY
    }

    private fun updateMediaSession(track: MediaItem, playing: Boolean, positionMs: Long) {
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO or
                        PlaybackState.ACTION_STOP
            )
            .setState(
                if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                positionMs,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())

        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, createDynamicAlbumArt(track.title))

        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun buildNotification(track: MediaItem, playing: Boolean): Notification {
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
            Intent(this, SoniqPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val togglePlayIntent = PendingIntent.getService(
            this, 2,
            Intent(this, SoniqPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, SoniqPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, SoniqPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder.setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.codec.label}")
            .setSubText(track.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(createDynamicAlbumArt(track.title))
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(playing)

        // Actions: Prev, Play/Pause, Next
        builder.addAction(
            Notification.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevIntent
            ).build()
        )

        val playPauseIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (playing) "Pause" else "Play"
        builder.addAction(
            Notification.Action.Builder(
                playPauseIcon,
                playPauseText,
                togglePlayIntent
            ).build()
        )

        builder.addAction(
            Notification.Action.Builder(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent
            ).build()
        )

        // Apply MediaStyle
        val mediaStyle = Notification.MediaStyle()
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }
        mediaStyle.setShowActionsInCompactView(0, 1, 2)
        builder.style = mediaStyle

        return builder.build()
    }

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        val am = audioManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            val res = am.requestAudioFocus(audioFocusRequest!!)
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        } else {
            @Suppress("DEPRECATION")
            val res = am.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeTransientLoss = false
                actionListener?.onPauseRequested()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeTransientLoss = isPlaying
                actionListener?.onPauseRequested()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Duck volume (lower temporarily)
                actionListener?.onDuckVolumeRequested(0.25f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                actionListener?.onRestoreVolumeRequested()
                if (wasPlayingBeforeTransientLoss) {
                    wasPlayingBeforeTransientLoss = false
                    actionListener?.onPlayRequested()
                }
            }
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
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

        // Accent Circle
        paint.color = Color.rgb(0, 229, 255)
        canvas.drawCircle(size / 2f, size / 2f, 80f, paint)

        paint.color = Color.rgb(11, 15, 23)
        canvas.drawCircle(size / 2f, size / 2f, 40f, paint)

        return bitmap
    }

    override fun onDestroy() {
        unregisterNoisyReceiver()
        abandonAudioFocus()
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
        const val NOTIFICATION_ID = 404
        const val CHANNEL_ID = "soniq_playback_channel"

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

        fun syncState(context: Context, track: MediaItem, isPlaying: Boolean, positionMs: Long) {
            try {
                val intent = Intent(context, SoniqPlaybackService::class.java).apply {
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
                    // When not playing, start standard service (or let running service update)
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Safeguard against OS background limits or cold-launch restrictions
            }
        }
    }
}
