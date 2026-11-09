package com.example.soniq.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.audio.AudioEngineState
import com.example.soniq.data.MediaItem
import com.example.soniq.data.MediaType
import com.example.soniq.data.SampleMediaRepository
import com.example.soniq.ui.components.rememberLocalAudioImporter
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.OledPitchBlack
import com.example.soniq.ui.theme.SonicFlowGradient
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta

enum class LibraryTab(val label: String) {
    ALL("All Tracks"),
    GENRE("By Genre"),
    ARTIST("By Artist"),
    FREQUENCY("Frequency")
}

enum class SortCriteria(val label: String) {
    DEFAULT("Default"),
    PLAY_COUNT("Most Played"),
    TITLE("Title (A-Z)"),
    ARTIST("Artist (A-Z)"),
    BITRATE("Highest Bitrate"),
    DURATION("Duration")
}

enum class LibraryQuickFilter(val label: String) {
    ALL("All"),
    HI_RES("Hi-Res Only"),
    VIDEO("Video"),
    FAVORITES("Favorites")
}

@Composable
fun LibraryScreen(
    state: AudioEngineState,
    onPlayTrack: (MediaItem) -> Unit,
    onAddToQueue: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedQuickFilter by remember { mutableStateOf(LibraryQuickFilter.ALL) }
    var selectedSort by remember { mutableStateOf(SortCriteria.DEFAULT) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    var localImportedTracks by remember { mutableStateOf(listOf<MediaItem>()) }
    val importLocalAudio = rememberLocalAudioImporter { newTrack ->
        localImportedTracks = localImportedTracks + newTrack
        onAddToQueue(newTrack)
    }

    val combinedTracks = remember(localImportedTracks) {
        SampleMediaRepository.defaultTracks + localImportedTracks
    }

    // Extract all distinct genres
    val distinctGenres = remember(combinedTracks) {
        combinedTracks.map { it.genre }.distinct().sorted()
    }

    // Extract all distinct artists
    val distinctArtists = remember(combinedTracks) {
        combinedTracks.map { it.artist }.distinct().sorted()
    }

    // Filter by search query
    val searchedTracks = remember(combinedTracks, searchQuery) {
        if (searchQuery.isBlank()) combinedTracks
        else {
            combinedTracks.filter { track ->
                track.title.contains(searchQuery, ignoreCase = true) ||
                        track.artist.contains(searchQuery, ignoreCase = true) ||
                        track.album.contains(searchQuery, ignoreCase = true) ||
                        track.genre.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledPitchBlack)
    ) {
        // 1. Search Bar & Local Storage Import Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSlate)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(NeonElectricBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_search_input"),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search title, genre, artist...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Zero-permission SAF Local Audio Importer Button
            IconButton(
                onClick = importLocalAudio,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSlate)
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    .testTag("import_audio_saf_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Import Local File",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Dynamic Primary Tab Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceSlate)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LibraryTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val bgModifier = if (isSelected) {
                    Modifier
                        .background(brush = SonicFlowGradient, shape = RoundedCornerShape(11.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(11.dp))
                } else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .then(bgModifier)
                        .clickable { selectedTab = tab }
                        .testTag("library_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                LibraryTab.ALL -> Icons.Default.MusicNote
                                LibraryTab.GENRE -> Icons.Default.Category
                                LibraryTab.ARTIST -> Icons.Default.Person
                                LibraryTab.FREQUENCY -> Icons.Default.LocalFireDepartment
                            },
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tab.label,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 3. Dynamic Sub-Content Based on Selected Tab
        when (selectedTab) {
            LibraryTab.ALL -> {
                AllTracksView(
                    tracks = searchedTracks,
                    state = state,
                    quickFilter = selectedQuickFilter,
                    onQuickFilterChange = { selectedQuickFilter = it },
                    sortCriteria = selectedSort,
                    onSortCriteriaChange = { selectedSort = it },
                    showSortMenu = showSortMenu,
                    onToggleSortMenu = { showSortMenu = it },
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite
                )
            }
            LibraryTab.GENRE -> {
                GenreCategorizedView(
                    tracks = searchedTracks,
                    genres = distinctGenres,
                    selectedGenre = selectedGenre,
                    onSelectGenre = { selectedGenre = it },
                    state = state,
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite
                )
            }
            LibraryTab.ARTIST -> {
                ArtistCategorizedView(
                    tracks = searchedTracks,
                    artists = distinctArtists,
                    state = state,
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite
                )
            }
            LibraryTab.FREQUENCY -> {
                PlaybackFrequencyCategorizedView(
                    tracks = searchedTracks,
                    state = state,
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

/**
 * All Tracks View with Quick Filters & Sorting Dropdown
 */
@Composable
private fun AllTracksView(
    tracks: List<MediaItem>,
    state: AudioEngineState,
    quickFilter: LibraryQuickFilter,
    onQuickFilterChange: (LibraryQuickFilter) -> Unit,
    sortCriteria: SortCriteria,
    onSortCriteriaChange: (SortCriteria) -> Unit,
    showSortMenu: Boolean,
    onToggleSortMenu: (Boolean) -> Unit,
    onPlayTrack: (MediaItem) -> Unit,
    onAddToQueue: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val filteredList = remember(tracks, quickFilter, state.queue) {
        tracks.filter { track ->
            when (quickFilter) {
                LibraryQuickFilter.ALL -> true
                LibraryQuickFilter.HI_RES -> track.isHiRes && track.mediaType == MediaType.AUDIO
                LibraryQuickFilter.VIDEO -> track.mediaType == MediaType.VIDEO
                LibraryQuickFilter.FAVORITES -> state.queue.find { it.id == track.id }?.isFavorite == true || track.isFavorite
            }
        }
    }

    val sortedList = remember(filteredList, sortCriteria) {
        when (sortCriteria) {
            SortCriteria.DEFAULT -> filteredList
            SortCriteria.PLAY_COUNT -> filteredList.sortedByDescending { it.playCount }
            SortCriteria.TITLE -> filteredList.sortedBy { it.title.lowercase() }
            SortCriteria.ARTIST -> filteredList.sortedBy { it.artist.lowercase() }
            SortCriteria.BITRATE -> filteredList.sortedByDescending { it.bitrateKbps }
            SortCriteria.DURATION -> filteredList.sortedByDescending { it.durationMs }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Filters & Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(LibraryQuickFilter.values(), key = { it.name }) { filter ->
                    val isSelected = quickFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) NeonElectricBlue else SurfaceSlate)
                            .border(
                                1.dp,
                                if (isSelected) NeonElectricBlue else Color(0x22FFFFFF),
                                RoundedCornerShape(9999.dp)
                            )
                            .clickable { onQuickFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("quick_filter_${filter.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sort Pill Dropdown
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceSlateHigh)
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(9999.dp))
                        .clickable { onToggleSortMenu(true) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("sort_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sortCriteria.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onToggleSortMenu(false) },
                    modifier = Modifier.background(SurfaceSlate)
                ) {
                    SortCriteria.values().forEach { criteria ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = criteria.label,
                                    color = if (sortCriteria == criteria) NeonCyan else Color.White,
                                    fontWeight = if (sortCriteria == criteria) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSortCriteriaChange(criteria)
                                onToggleSortMenu(false)
                            }
                        )
                    }
                }
            }
        }

        // Summary count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${sortedList.size} master recordings",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceSlate)
                    .clickable {
                        if (sortedList.isNotEmpty()) onPlayTrack(sortedList.random())
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Track List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("all_tracks_list"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(
                items = sortedList,
                key = { it.id },
                contentType = { "track_row" }
            ) { track ->
                val isPlaying = state.isPlaying && state.currentTrack.id == track.id
                val isFavorite = state.queue.find { it.id == track.id }?.isFavorite ?: track.isFavorite

                MediaListRow(
                    track = track,
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
                    onClick = { onPlayTrack(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) }
                )
            }
        }
    }
}

/**
 * Genre Categorization View with Dynamic Filter Carousel & Category Profiles
 */
@Composable
private fun GenreCategorizedView(
    tracks: List<MediaItem>,
    genres: List<String>,
    selectedGenre: String?,
    onSelectGenre: (String?) -> Unit,
    state: AudioEngineState,
    onPlayTrack: (MediaItem) -> Unit,
    onAddToQueue: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val activeGenre = selectedGenre ?: genres.firstOrNull() ?: "Electronic"
    val genreTracks = remember(tracks, activeGenre) {
        tracks.filter { it.genre.equals(activeGenre, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Genre Chips Horizontal Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres, key = { it }) { genre ->
                val isSelected = genre.equals(activeGenre, ignoreCase = true)
                val count = tracks.count { it.genre.equals(genre, ignoreCase = true) }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) NeonViolet else SurfaceSlate)
                        .border(
                            1.dp,
                            if (isSelected) NeonViolet else Color(0x22FFFFFF),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectGenre(genre) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("genre_chip_${genre.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = genre,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0x22FFFFFF))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$count",
                                color = if (isSelected) Color.White else Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Genre Sonic Profile Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceSlate)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeGenre.uppercase(),
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.05.sp
                    )
                    Text(
                        text = "${genreTracks.size} Master Recordings",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(brush = SonicFlowGradient)
                        .clickable {
                            if (genreTracks.isNotEmpty()) onPlayTrack(genreTracks.first())
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("play_genre_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play Genre",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Categorized Genre Tracks List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("genre_track_list"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(
                items = genreTracks,
                key = { it.id },
                contentType = { "track_row" }
            ) { track ->
                val isPlaying = state.isPlaying && state.currentTrack.id == track.id
                val isFavorite = state.queue.find { it.id == track.id }?.isFavorite ?: track.isFavorite

                MediaListRow(
                    track = track,
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
                    onClick = { onPlayTrack(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) }
                )
            }
        }
    }
}

/**
 * Artist Categorized View grouping tracks by creators
 */
@Composable
private fun ArtistCategorizedView(
    tracks: List<MediaItem>,
    artists: List<String>,
    state: AudioEngineState,
    onPlayTrack: (MediaItem) -> Unit,
    onAddToQueue: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var expandedArtist by remember { mutableStateOf<String?>(artists.firstOrNull()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("artist_categorized_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(artists, key = { it }) { artist ->
            val artistTracks = tracks.filter { it.artist.equals(artist, ignoreCase = true) }
            val isExpanded = expandedArtist == artist
            val highestCodec = artistTracks.maxByOrNull { it.bitrateKbps }?.codec?.label ?: "FLAC"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isExpanded) NeonElectricBlue.copy(alpha = 0.5f) else Color(0x1AFFFFFF),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedArtist = if (isExpanded) null else artist
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artist Avatar Initials
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(NeonViolet, Color(0xFF0F172A))
                                    )
                                )
                                .border(1.5.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = artist.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${artistTracks.size} tracks",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonElectricBlue.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = highestCodec,
                                        color = NeonElectricBlue,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Play Artist Discography
                        IconButton(
                            onClick = {
                                if (artistTracks.isNotEmpty()) onPlayTrack(artistTracks.first())
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceSlateHigh)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Artist",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Expanded Tracks List under Artist
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            artistTracks.forEach { track ->
                                val isPlaying = state.isPlaying && state.currentTrack.id == track.id
                                val isFavorite = state.queue.find { it.id == track.id }?.isFavorite ?: track.isFavorite

                                MediaListRow(
                                    track = track,
                                    isPlaying = isPlaying,
                                    isFavorite = isFavorite,
                                    onClick = { onPlayTrack(track) },
                                    onAddToQueue = { onAddToQueue(track) },
                                    onToggleFavorite = { onToggleFavorite(track.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/**
 * Playback Frequency Categorized View:
 * Categorizes tracks into Heavy Rotation (100+ plays), Frequent Listens (50-100 plays), and Casual Discovery (<50 plays)
 */
@Composable
private fun PlaybackFrequencyCategorizedView(
    tracks: List<MediaItem>,
    state: AudioEngineState,
    onPlayTrack: (MediaItem) -> Unit,
    onAddToQueue: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var sortDescending by remember { mutableStateOf(true) }

    val sortedByPlays = remember(tracks, sortDescending) {
        if (sortDescending) tracks.sortedByDescending { it.playCount }
        else tracks.sortedBy { it.playCount }
    }

    val heavyRotation = sortedByPlays.filter { it.playCount >= 100 }
    val frequentListens = sortedByPlays.filter { it.playCount in 50..99 }
    val casualDiscoveries = sortedByPlays.filter { it.playCount < 50 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("frequency_categorized_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Analytics Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACOUSTIC INTELLIGENCE",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.05.sp
                        )
                        Text(
                            text = "Playback Frequency Tiers",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Categorized by session rotation & DAC streaming telemetry",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    // Sort Order Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceSlateHigh)
                            .clickable { sortDescending = !sortDescending }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("frequency_sort_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (sortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortDescending) "Most Played" else "Least Played",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Heavy Rotation (100+ Plays)
        if (heavyRotation.isNotEmpty()) {
            item {
                FrequencySectionHeader(
                    title = "HEAVY ROTATION",
                    count = heavyRotation.size,
                    badgeText = "Top Listens",
                    accentColor = VibrantMagenta
                )
            }
            items(heavyRotation, key = { it.id }) { track ->
                FrequencyTrackRow(
                    track = track,
                    state = state,
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite,
                    badgeColor = VibrantMagenta
                )
            }
        }

        // Section 2: Frequent Listens (50-99 Plays)
        if (frequentListens.isNotEmpty()) {
            item {
                FrequencySectionHeader(
                    title = "FREQUENT ROTATION",
                    count = frequentListens.size,
                    badgeText = "Consistent Stream",
                    accentColor = NeonElectricBlue
                )
            }
            items(frequentListens, key = { it.id }) { track ->
                FrequencyTrackRow(
                    track = track,
                    state = state,
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite,
                    badgeColor = NeonElectricBlue
                )
            }
        }

        // Section 3: Casual Discoveries (<50 Plays)
        if (casualDiscoveries.isNotEmpty()) {
            item {
                FrequencySectionHeader(
                    title = "DISCOVERY ARCHIVE",
                    count = casualDiscoveries.size,
                    badgeText = "Fresh Discoveries",
                    accentColor = NeonCyan
                )
            }
            items(casualDiscoveries, key = { it.id }) { track ->
                FrequencyTrackRow(
                    track = track,
                    state = state,
                    onPlayTrack = onPlayTrack,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite,
                    badgeColor = NeonCyan
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
private fun FrequencySectionHeader(
    title: String,
    count: Int,
    badgeText: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.05.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "($count)",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                color = accentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FrequencyTrackRow(
    track: MediaItem,
    state: AudioEngineState,
    onPlayTrack: (MediaItem) -> Unit,
    onAddToQueue: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    badgeColor: Color
) {
    val isPlaying = state.isPlaying && state.currentTrack.id == track.id
    val isFavorite = state.queue.find { it.id == track.id }?.isFavorite ?: track.isFavorite

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayTrack(track) }
            .testTag("freq_track_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Frequency Indicator Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${track.playCount}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPlaying) NeonElectricBlue else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${track.artist} • ${track.genre}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceSlateHigh)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = track.codec.label,
                            color = NeonElectricBlue,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(
                onClick = { onToggleFavorite(track.id) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) VibrantMagenta else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaListRow(
    track: MediaItem,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                if (isPlaying) Color(0x1F0066FF) else Color.Transparent
            )
            .clickable { onClick() }
            .testTag("track_row_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neon playing accent indicator (3dp)
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(NeonElectricBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Thumbnail Artwork
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (track.mediaType == MediaType.VIDEO) Color(0xFF6F00BE) else SurfaceSlateHigh
                    )
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isPlaying) NeonElectricBlue else Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle Stack
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPlaying) NeonElectricBlue else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "${track.artist} • ${track.genre}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (track.codec.isMaster) VibrantMagenta.copy(alpha = 0.2f)
                                else NeonElectricBlue.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = track.codec.label,
                            color = if (track.codec.isMaster) VibrantMagenta else NeonElectricBlue,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Play Count Badge & Options Menu
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (track.playCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceSlateHigh)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${track.playCount}x",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                val mins = track.durationMs / 60000
                val secs = (track.durationMs % 60000) / 1000
                Text(
                    text = String.format("%d:%02d", mins, secs),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(SurfaceSlate)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play Next in Queue", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistPlay, null, tint = NeonElectricBlue)
                            },
                            onClick = {
                                onAddToQueue()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = VibrantMagenta
                                )
                            },
                            onClick = {
                                onToggleFavorite()
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
