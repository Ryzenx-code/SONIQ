package com.example.soniq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soniq.data.SoniqTab
import com.example.soniq.ui.theme.NeonElectricBlue

@Composable
fun SoniqBottomNavBar(
    selectedTab: SoniqTab,
    onTabSelected: (SoniqTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .border(width = 1.dp, color = Color(0x1AFFFFFF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = selectedTab == SoniqTab.HOME,
                testTag = "nav_home",
                onClick = { onTabSelected(SoniqTab.HOME) }
            )
            NavItem(
                icon = Icons.Default.LibraryMusic,
                label = "Library",
                isSelected = selectedTab == SoniqTab.LIBRARY,
                testTag = "nav_library",
                onClick = { onTabSelected(SoniqTab.LIBRARY) }
            )
            NavItem(
                icon = Icons.Default.Tune,
                label = "Audio Studio",
                isSelected = selectedTab == SoniqTab.AUDIO_STUDIO,
                testTag = "nav_audio_studio",
                onClick = { onTabSelected(SoniqTab.AUDIO_STUDIO) }
            )
            NavItem(
                icon = Icons.Default.Security,
                label = "Vault",
                isSelected = selectedTab == SoniqTab.VAULT,
                testTag = "nav_vault",
                onClick = { onTabSelected(SoniqTab.VAULT) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val activeColor = NeonElectricBlue
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (isSelected) activeColor else inactiveColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
