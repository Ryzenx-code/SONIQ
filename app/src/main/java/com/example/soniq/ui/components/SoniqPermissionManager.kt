package com.example.soniq.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.soniq.data.AudioCodec
import com.example.soniq.data.MediaItem
import com.example.soniq.data.MediaType
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.OledPitchBlack
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta

/**
 * Modern Android Permission & Privacy Controller for SONIQ.
 * Adheres to Google Play permissions minimization:
 * - Runtime Notification permission for lock-screen & background playback (Android 13+ / API 33+)
 * - Zero-permission Storage Access Framework (SAF) OpenDocument for local audio imports
 * - Transparent rationale dialogs with graceful degradation
 */
@Composable
fun NotificationPermissionRequester(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                // Show transparent rationale first
                showRationaleDialog = true
            } else {
                onPermissionResult(true)
            }
        } else {
            onPermissionResult(true)
        }
    }

    if (showRationaleDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            containerColor = SurfaceSlate,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonElectricBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Background Playback Controls",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "SONIQ uses notifications to show lock-screen controls, active master track metadata, and quick play/pause actions while you multitask.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Lock-screen media player\n• Background high-fidelity playback\n• Direct headphone controls",
                        color = NeonElectricBlue,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonElectricBlue),
                    modifier = Modifier.testTag("grant_notification_permission_btn")
                ) {
                    Text("Enable Controls", color = OledPitchBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        onPermissionResult(false)
                    }
                ) {
                    Text("Not Now", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

/**
 * Compliant Zero-Permission Local Audio File Picker using Storage Access Framework (SAF).
 * Does NOT require broad READ_EXTERNAL_STORAGE.
 */
@Composable
fun rememberLocalAudioImporter(
    onTrackImported: (MediaItem) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            val track = extractMediaItemFromUri(context, fileUri)
            onTrackImported(track)
        }
    }

    return {
        openDocumentLauncher.launch(arrayOf("audio/*", "application/ogg", "application/x-flac"))
    }
}

private fun extractMediaItemFromUri(context: Context, uri: Uri): MediaItem {
    var displayName = "Imported Track"
    var fileSize: Long = 0

    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}

    val cleanTitle = displayName.substringBeforeLast(".")
    val extension = displayName.substringAfterLast(".", "flac").uppercase()

    val codec = when (extension) {
        "DSD", "DSF", "DFF" -> AudioCodec.DSD256
        "DXD" -> AudioCodec.DXD
        "FLAC" -> AudioCodec.FLAC_HIRES
        "WAV" -> AudioCodec.WAV_PCM
        "MQA" -> AudioCodec.MQA
        else -> AudioCodec.FLAC_STD
    }

    return MediaItem(
        id = "imported_${System.currentTimeMillis()}",
        title = cleanTitle,
        artist = "Local Master File",
        album = "Imported Audio Archive",
        durationMs = 240000L,
        codec = codec,
        bitrateKbps = if (fileSize > 20_000_000) 9216 else 1411,
        mediaType = MediaType.AUDIO,
        isHiRes = true,
        isFavorite = false,
        audioChannels = "Bit-Perfect PCM",
        genre = "Local Storage",
        playCount = 1
    )
}
