package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResizeMode
import com.example.data.model.SubtitleConfig
import com.example.data.model.SubtitleTrack
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SubtitleCyan
import com.example.ui.theme.SubtitleWhite
import com.example.ui.theme.SubtitleYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleBottomSheet(
    tracks: List<SubtitleTrack>,
    selectedTrackId: String?,
    config: SubtitleConfig,
    onTrackSelected: (String?) -> Unit,
    onPickExternalFile: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateSyncOffset: (Long) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateTextColor: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x66FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = "Subtitles",
                        tint = CyanAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Subtitle Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1E1400),
                        checkedTrackColor = AmberPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Tracks
            Text(
                text = "SUBTITLE TRACKS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Option: None (Off)
            TrackItem(
                label = "None (Off)",
                subtext = "Disable subtitle overlay",
                isSelected = selectedTrackId == null,
                onClick = { onTrackSelected(null) }
            )

            tracks.forEach { track ->
                TrackItem(
                    label = track.label,
                    subtext = if (track.isExternal) "External file (${track.cues.size} cues)" else "Embedded track (${track.language})",
                    isSelected = track.id == selectedTrackId,
                    onClick = { onTrackSelected(track.id) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickExternalFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_subtitle_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load External Subtitle (.srt / .vtt)")
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Timing & Sync
            Text(
                text = "SUBTITLE SYNC TIMING",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onUpdateSyncOffset(config.syncOffsetMs - 200) }) {
                    Icon(Icons.Default.FastRewind, contentDescription = "-200ms", tint = AmberPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val offsetSec = config.syncOffsetMs / 1000.0
                    Text(
                        text = String.format("%+.2fs", offsetSec),
                        color = if (config.syncOffsetMs != 0L) CyanAccent else TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (config.syncOffsetMs == 0L) "Synchronized" else if (config.syncOffsetMs > 0) "Subtitle delayed" else "Subtitle earlier",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = { onUpdateSyncOffset(config.syncOffsetMs + 200) }) {
                    Icon(Icons.Default.FastForward, contentDescription = "+200ms", tint = AmberPrimary)
                }

                if (config.syncOffsetMs != 0L) {
                    IconButton(onClick = { onUpdateSyncOffset(0L) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Sync", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Font Size
            Text(
                text = "FONT SIZE",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(16f to "Small", 19f to "Medium", 24f to "Large").forEach { (size, label) ->
                    val selected = config.fontSizeSp == size
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateFontSize(size) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color(0xFF1E1400)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Text Color
            Text(
                text = "TEXT COLOR",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(
                    0xFFFFFFFF to "White",
                    0xFFFFEB3B to "Yellow",
                    0xFF00E5FF to "Cyan"
                ).forEach { (colorHex, name) ->
                    val isChosen = config.textColorHex == colorHex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdateTextColor(colorHex) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isChosen) 3.dp else 1.dp,
                                    color = if (isChosen) AmberPrimary else Color(0x66FFFFFF),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colorHex == 0xFFFFFFFF) Color.Black else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = name, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun TrackItem(
    label: String,
    subtext: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) AmberPrimary.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                color = if (isSelected) AmberPrimary else TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = subtext,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AmberPrimary)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsBottomSheet(
    currentSpeed: Float,
    currentResizeMode: ResizeMode,
    isLooping: Boolean,
    isAudioBoost: Boolean,
    onSpeedSelected: (Float) -> Unit,
    onResizeModeSelected: (ResizeMode) -> Unit,
    onToggleLooping: (Boolean) -> Unit,
    onToggleAudioBoost: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x66FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = AmberPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Playback & Video Settings",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Speed
            Text(
                text = "PLAYBACK SPEED",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = currentSpeed == speed
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSpeedSelected(speed) },
                        label = { Text("${speed}x") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color(0xFF1E1400)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(16.dp))

            // Aspect Ratio
            Text(
                text = "ASPECT RATIO",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            ResizeMode.values().forEach { mode ->
                val isSelected = currentResizeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onResizeModeSelected(mode) }
                        .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) AmberPrimary else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AmberPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(16.dp))

            // Audio Boost Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Audio Boost (Up to 150%)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Amplifies volume for quiet media", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = isAudioBoost,
                    onCheckedChange = onToggleAudioBoost,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1E1400),
                        checkedTrackColor = CyanAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Repeat Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Loop Playback", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Repeat this video automatically", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = isLooping,
                    onCheckedChange = onToggleLooping,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1E1400),
                        checkedTrackColor = AmberPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
