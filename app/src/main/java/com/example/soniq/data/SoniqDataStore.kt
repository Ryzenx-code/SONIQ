package com.example.soniq.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "soniq_preferences")

data class PersistedPlaybackState(
    val lastTrackId: String?,
    val lastPositionMs: Long,
    val volume: Float,
    val isShuffle: Boolean,
    val repeatModeIndex: Int,
    val presetId: String,
    val isDspEnabled: Boolean
)

class SoniqDataStore(private val context: Context) {

    companion object {
        val KEY_LAST_TRACK_ID = stringPreferencesKey("last_track_id")
        val KEY_LAST_POS_MS = longPreferencesKey("last_pos_ms")
        val KEY_VOLUME = floatPreferencesKey("volume")
        val KEY_SHUFFLE = booleanPreferencesKey("is_shuffle")
        val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
        val KEY_PRESET_ID = stringPreferencesKey("preset_id")
        val KEY_DSP_ENABLED = booleanPreferencesKey("dsp_enabled")
    }

    val playbackStateFlow: Flow<PersistedPlaybackState> = context.dataStore.data.map { prefs ->
        PersistedPlaybackState(
            lastTrackId = prefs[KEY_LAST_TRACK_ID],
            lastPositionMs = prefs[KEY_LAST_POS_MS] ?: 0L,
            volume = prefs[KEY_VOLUME] ?: 0.85f,
            isShuffle = prefs[KEY_SHUFFLE] ?: false,
            repeatModeIndex = prefs[KEY_REPEAT_MODE] ?: 0,
            presetId = prefs[KEY_PRESET_ID] ?: "custom_1",
            isDspEnabled = prefs[KEY_DSP_ENABLED] ?: true
        )
    }

    suspend fun savePlaybackState(
        trackId: String,
        positionMs: Long,
        volume: Float,
        isShuffle: Boolean,
        repeatModeIndex: Int,
        presetId: String,
        isDspEnabled: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_TRACK_ID] = trackId
            prefs[KEY_LAST_POS_MS] = positionMs
            prefs[KEY_VOLUME] = volume
            prefs[KEY_SHUFFLE] = isShuffle
            prefs[KEY_REPEAT_MODE] = repeatModeIndex
            prefs[KEY_PRESET_ID] = presetId
            prefs[KEY_DSP_ENABLED] = isDspEnabled
        }
    }
}
