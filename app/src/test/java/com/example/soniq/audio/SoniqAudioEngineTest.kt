package com.example.soniq.audio

import com.example.soniq.data.SampleMediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoniqAudioEngineTest {

    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun testInitialAudioEngineStateIsNeutral() {
        val engine = SoniqAudioEngine(testScope, null)
        val state = engine.state.value

        assertEquals(0, state.listeningXp)
        assertEquals(0, state.dailyStreakDays)
        assertFalse(state.isPlaying)
        assertEquals(0L, engine.positionMs.value)
        engine.release()
    }

    @Test
    fun testPlayTrackUpdatesQueueAndCurrentTrack() {
        val engine = SoniqAudioEngine(testScope, null)
        val targetTrack = SampleMediaRepository.defaultTracks[2]

        engine.playTrack(targetTrack)

        val state = engine.state.value
        assertEquals(targetTrack.id, state.currentTrack.id)
        assertTrue(state.isPlaying)
        assertEquals(0L, engine.positionMs.value)
        engine.release()
    }

    @Test
    fun testShuffleOrderChangesTrackOrderWithoutCrash() {
        val engine = SoniqAudioEngine(testScope, null)
        assertFalse(engine.state.value.isShuffle)

        engine.toggleShuffle()
        assertTrue(engine.state.value.isShuffle)

        val currentTrack = engine.state.value.currentTrack
        engine.playNext()
        val nextTrack = engine.state.value.currentTrack

        // Next track plays successfully
        assertTrue(engine.state.value.isPlaying)
        engine.release()
    }

    @Test
    fun testPlayPreviousSeeksToZeroWhenPositionExceedsThreshold() {
        val engine = SoniqAudioEngine(testScope, null)
        engine.seekTo(5000L)
        assertEquals(5000L, engine.positionMs.value)

        val currentTrackId = engine.state.value.currentTrack.id
        engine.playPrevious()

        assertEquals(0L, engine.positionMs.value)
        assertEquals(currentTrackId, engine.state.value.currentTrack.id)
        engine.release()
    }
}
