package com.example.soniq.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real device media provider querying android.provider.MediaStore for Audio and Video tracks.
 */
class RealMediaProvider(private val context: Context) {

    suspend fun queryDeviceAudioTracks(): List<MediaItem> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Title"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val durationMs = c.getLong(durationCol)
                    val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    tracks.add(
                        MediaItem(
                            id = "device_audio_$id",
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Local Music" else album,
                            durationMs = if (durationMs > 0) durationMs else 180000L,
                            codec = AudioCodec.FLAC_HIRES,
                            bitrateKbps = 1411,
                            mediaType = MediaType.AUDIO,
                            isHiRes = false,
                            isFavorite = false,
                            audioChannels = "Stereo 44.1kHz",
                            genre = "Local Audio",
                            playCount = 0,
                            waveformData = List(32) { (0.2f + (it % 5) * 0.15f) },
                            lyrics = emptyList(),
                            mediaUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission or querying exception
        }

        tracks
    }

    suspend fun queryDeviceVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RESOLUTION
        )

        val sortOrder = "${MediaStore.Video.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val resCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Video $id"
                    val durationMs = c.getLong(durationCol)
                    val res = c.getString(resCol) ?: "1080p"
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    videos.add(
                        MediaItem(
                            id = "device_video_$id",
                            title = title,
                            artist = "Local Video ($res)",
                            album = "Gallery Videos",
                            durationMs = if (durationMs > 0) durationMs else 120000L,
                            codec = AudioCodec.WAV_PCM,
                            bitrateKbps = 12000,
                            mediaType = MediaType.VIDEO,
                            isHiRes = true,
                            isFavorite = false,
                            audioChannels = "Stereo",
                            genre = "Video",
                            playCount = 0,
                            waveformData = emptyList(),
                            lyrics = emptyList(),
                            mediaUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        videos
    }
}
