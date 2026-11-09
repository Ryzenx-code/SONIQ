package com.example.soniq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.data.MediaItem
import com.example.soniq.data.MediaType
import com.example.soniq.data.SampleMediaRepository
import com.example.soniq.ui.theme.NeonCyan
import com.example.soniq.ui.theme.NeonElectricBlue
import com.example.soniq.ui.theme.NeonViolet
import com.example.soniq.ui.theme.SonicFlowGradient
import com.example.soniq.ui.theme.SurfaceSlate
import com.example.soniq.ui.theme.SurfaceSlateHigh
import com.example.soniq.ui.theme.VibrantMagenta

import com.example.soniq.ui.theme.OledPitchBlack

enum class AuthMode {
    BIOMETRIC,
    PIN
}

@Composable
fun PrivateLockerScreen(
    onPlayTrack: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf(AuthMode.BIOMETRIC) }
    var pinDigits by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf(false) }
    val vaultItems = remember { mutableStateListOf(*SampleMediaRepository.vaultTracks.toTypedArray()) }
    var showImportSuccess by remember { mutableStateOf(false) }

    fun checkPin(digit: String) {
        if (pinDigits.length < 6) {
            val updated = pinDigits + digit
            pinDigits = updated
            if (updated.length == 6) {
                // Default test PIN is 123456 or any 6 digits
                if (updated == "123456" || updated == "000000") {
                    isUnlocked = true
                    authError = false
                } else {
                    authError = true
                    pinDigits = ""
                }
            }
        }
    }

    if (!isUnlocked) {
        // Locked Auth Screen
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(OledPitchBlack)
                .padding(20.dp)
                .testTag("private_locker_auth"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Knox Enclave Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceSlate)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(9999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "KNOX TEE HARDWARE ENCLAVE • STRONGBOX ACTIVE",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.04.sp
                )
            }

            // Concentric Rings & Dynamic Security Graphic
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                NeonViolet.copy(alpha = 0.35f),
                                Color(0xFF131926),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(brush = SonicFlowGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Verify Identity to Decrypt",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Biometric passkey or Master PIN (123456) required to release AES-256 GCM hardware keys.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                if (authError) {
                    Text(
                        text = "Incorrect PIN. Try default: 123456",
                        color = VibrantMagenta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Mode Selector: Biometric vs PIN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceSlate)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (authMode == AuthMode.BIOMETRIC) NeonElectricBlue else Color.Transparent)
                        .clickable { authMode = AuthMode.BIOMETRIC }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (authMode == AuthMode.BIOMETRIC) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Biometric",
                            color = if (authMode == AuthMode.BIOMETRIC) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (authMode == AuthMode.PIN) NeonElectricBlue else Color.Transparent)
                        .clickable { authMode = AuthMode.PIN }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pin,
                            contentDescription = null,
                            tint = if (authMode == AuthMode.PIN) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Master PIN",
                            color = if (authMode == AuthMode.PIN) Color.White else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Interactive Auth Trigger
            if (authMode == AuthMode.BIOMETRIC) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(SurfaceSlate)
                            .border(2.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                // Simulate successful biometric authentication
                                isUnlocked = true
                            }
                            .testTag("biometric_sensor_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Scan Fingerprint",
                            tint = NeonCyan,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Touch sensor to authenticate",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Class 3 Biometric • Android Keystore API v3",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            } else {
                // PIN Dots & Keypad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    // 6 Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        for (i in 0 until 6) {
                            val filled = i < pinDigits.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) NeonElectricBlue else SurfaceSlateHigh)
                                    .border(
                                        1.dp,
                                        if (filled) NeonElectricBlue else Color(0x33FFFFFF),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // 3x4 Virtual Keypad Grid
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keys.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceSlate)
                                            .clickable {
                                                when (key) {
                                                    "C" -> pinDigits = ""
                                                    "⌫" -> if (pinDigits.isNotEmpty()) pinDigits = pinDigits.dropLast(1)
                                                    else -> checkPin(key)
                                                }
                                            }
                                            .testTag("pin_key_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Unlocked Knox Vault Media List
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(OledPitchBlack)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("unlocked_vault_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Knox Private Vault",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "AES-256 GCM Hardware Enclave Decrypted",
                                color = NeonCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = { isUnlocked = false },
                        modifier = Modifier.testTag("lock_vault_btn")
                    ) {
                        Text("Lock Vault", color = VibrantMagenta, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Vault Actions: Import media / Decrypt Export
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = NeonElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Secure Import Media",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Encrypt audio & video to private sandbox",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(brush = SonicFlowGradient)
                                .clickable {
                                    // Add a sample imported recording
                                    vaultItems.add(
                                        MediaItem(
                                            id = "vault_${System.currentTimeMillis()}",
                                            title = "Analog Live Session (Confidential)",
                                            artist = "Audio Studio Master",
                                            album = "Private Archive",
                                            durationMs = 280000L,
                                            codec = com.example.soniq.data.AudioCodec.FLAC_HIRES,
                                            bitrateKbps = 9216,
                                            isPrivateVault = true,
                                            audioChannels = "AES-256 Encrypted"
                                        )
                                    )
                                    showImportSuccess = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("import_media_btn")
                        ) {
                            Text(
                                text = "IMPORT",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (showImportSuccess) {
                item {
                    Text(
                        text = "Encrypted 1 item into Knox Hardware Enclave",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                Text(
                    text = "DECRYPTED VAULT ITEMS (${vaultItems.size})",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.sp
                )
            }

            items(vaultItems) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayTrack(item) }
                        .testTag("vault_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (item.mediaType == MediaType.VIDEO) Color(0xFF6F00BE) else SurfaceSlateHigh
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.artist} • ${item.codec.label}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DECRYPTED",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
