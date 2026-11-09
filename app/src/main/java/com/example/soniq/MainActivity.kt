package com.example.soniq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.soniq.data.SoniqTab
import com.example.soniq.ui.components.SoniqBottomNavBar
import com.example.soniq.ui.components.SoniqMiniPlayer
import com.example.soniq.ui.components.SoniqTopBar
import com.example.soniq.ui.screens.AudioStudioScreen
import com.example.soniq.ui.screens.HomeScreen
import com.example.soniq.ui.screens.LibraryScreen
import com.example.soniq.ui.screens.LyricsSheet
import com.example.soniq.ui.screens.NowPlayingScreen
import com.example.soniq.ui.screens.PrivateLockerScreen
import com.example.soniq.ui.screens.QueueSheet
import com.example.soniq.ui.screens.VideoPlayerScreen
import com.example.soniq.ui.components.NotificationPermissionRequester
import com.example.soniq.ui.theme.SoniqTheme

open class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val engineState by viewModel.engineState.collectAsState()

            SoniqTheme(darkTheme = engineState.isDarkTheme) {
                // Request Android 13+ Notification permission for background playback controls
                NotificationPermissionRequester()

                // System Back Button Handling
                BackHandler(
                    enabled = uiState.activeVideoItem != null ||
                            uiState.isLyricsSheetOpen ||
                            uiState.isQueueSheetOpen ||
                            uiState.isNowPlayingExpanded ||
                            uiState.selectedTab != SoniqTab.HOME
                ) {
                    when {
                        uiState.activeVideoItem != null -> viewModel.closeVideoPlayer()
                        uiState.isLyricsSheetOpen -> viewModel.closeLyricsSheet()
                        uiState.isQueueSheetOpen -> viewModel.closeQueueSheet()
                        uiState.isNowPlayingExpanded -> viewModel.collapseNowPlaying()
                        uiState.selectedTab != SoniqTab.HOME -> viewModel.selectTab(SoniqTab.HOME)
                    }
                }

                // Video Fullscreen View
                val activeVideo = uiState.activeVideoItem
                if (activeVideo != null) {
                    VideoPlayerScreen(
                        videoItem = activeVideo,
                        onBack = { viewModel.closeVideoPlayer() }
                    )
                } else if (uiState.isNowPlayingExpanded) {
                    // Full Screen Now Playing View
                    NowPlayingScreen(
                        state = engineState,
                        onCollapse = { viewModel.collapseNowPlaying() },
                        onTogglePlay = { viewModel.audioEngine.togglePlayPause() },
                        onSeekTo = { viewModel.audioEngine.seekTo(it) },
                        onNext = { viewModel.audioEngine.playNext() },
                        onPrevious = { viewModel.audioEngine.playPrevious() },
                        onToggleShuffle = { viewModel.audioEngine.toggleShuffle() },
                        onCycleRepeat = { viewModel.audioEngine.cycleRepeatMode() },
                        onToggleFavorite = { viewModel.audioEngine.toggleFavorite(it) },
                        onSetSpeed = { viewModel.audioEngine.setPlaybackSpeed(it) },
                        onSetSleepTimer = { viewModel.audioEngine.setSleepTimer(it) },
                        onOpenStudio = {
                            viewModel.collapseNowPlaying()
                            viewModel.selectTab(SoniqTab.AUDIO_STUDIO)
                        },
                        onOpenQueue = { viewModel.openQueueSheet() },
                        onOpenLyrics = { viewModel.openLyricsSheet() },
                        onSetBandGain = { idx, gain -> viewModel.audioEngine.setBandGain(idx, gain) },
                        onToggleDsp = { viewModel.audioEngine.toggleDsp() },
                        onResetUnity = { viewModel.audioEngine.resetEqToUnity() },
                        onSelectPreset = { viewModel.audioEngine.selectPreset(it) },
                        onSetVolume = { viewModel.audioEngine.setVolume(it) }
                    )
                } else {
                    // Standard App Shell with Top Bar, Bottom Bar, and Floating Mini-Player
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            SoniqTopBar(
                                isDarkTheme = engineState.isDarkTheme,
                                onToggleTheme = { viewModel.audioEngine.toggleTheme() },
                                onVaultClick = { viewModel.selectTab(SoniqTab.VAULT) }
                            )
                        },
                        bottomBar = {
                            SoniqBottomNavBar(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            // Primary Tab Screen
                            when (uiState.selectedTab) {
                                SoniqTab.HOME -> {
                                    HomeScreen(
                                        state = engineState,
                                        onPlayTrack = { viewModel.playTrack(it) },
                                        onNavigateLibrary = { viewModel.selectTab(SoniqTab.LIBRARY) },
                                        onNavigateStudio = { viewModel.selectTab(SoniqTab.AUDIO_STUDIO) },
                                        onNavigateVault = { viewModel.selectTab(SoniqTab.VAULT) }
                                    )
                                }
                                SoniqTab.LIBRARY -> {
                                    LibraryScreen(
                                        state = engineState,
                                        onPlayTrack = { viewModel.playTrack(it) },
                                        onAddToQueue = { viewModel.audioEngine.addToQueue(it) },
                                        onToggleFavorite = { viewModel.audioEngine.toggleFavorite(it) }
                                    )
                                }
                                SoniqTab.AUDIO_STUDIO -> {
                                    AudioStudioScreen(
                                        state = engineState,
                                        onToggleDsp = { viewModel.audioEngine.toggleDsp() },
                                        onSelectPreset = { viewModel.audioEngine.selectPreset(it) },
                                        onSetBandGain = { idx, gain -> viewModel.audioEngine.setBandGain(idx, gain) },
                                        onResetUnity = { viewModel.audioEngine.resetEqToUnity() },
                                        onSetBassBoost = { viewModel.audioEngine.setBassBoost(it) },
                                        onSetVirtualizer = { viewModel.audioEngine.setVirtualizer(it) },
                                        onSetCrystalizer = { viewModel.audioEngine.setCrystalizer(it) }
                                    )
                                }
                                SoniqTab.VAULT -> {
                                    PrivateLockerScreen(
                                        onPlayTrack = { viewModel.playTrack(it) }
                                    )
                                }
                            }

                            // Floating Mini Player docked above BottomNavBar with Gesture Listeners
                            SoniqMiniPlayer(
                                currentTrack = engineState.currentTrack,
                                isPlaying = engineState.isPlaying,
                                currentPositionMs = engineState.currentPositionMs,
                                durationMs = engineState.durationMs,
                                onTogglePlay = { viewModel.audioEngine.togglePlayPause() },
                                onSkipNext = { viewModel.audioEngine.playNext() },
                                onSkipPrevious = { viewModel.audioEngine.playPrevious() },
                                volume = engineState.volume,
                                onSetVolume = { viewModel.audioEngine.setVolume(it) },
                                onClick = { viewModel.expandNowPlaying() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                // Modal Queue Sheet
                AnimatedVisibility(
                    visible = uiState.isQueueSheetOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    QueueSheet(
                        state = engineState,
                        onDismiss = { viewModel.closeQueueSheet() },
                        onPlayTrack = { viewModel.playTrack(it) },
                        onRemoveFromQueue = { viewModel.audioEngine.removeFromQueue(it) },
                        onClearQueue = { viewModel.audioEngine.clearQueue() }
                    )
                }

                // Modal Lyrics Sheet
                AnimatedVisibility(
                    visible = uiState.isLyricsSheetOpen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    LyricsSheet(
                        state = engineState,
                        onDismiss = { viewModel.closeLyricsSheet() },
                        onSeekTo = { viewModel.audioEngine.seekTo(it) }
                    )
                }
            }
        }
    }
}
