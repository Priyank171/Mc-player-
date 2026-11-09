package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveGesture
import com.example.ui.theme.AmberLight
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GestureHudBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GestureHudOverlays(
    activeGesture: ActiveGesture?,
    isSpeedBoostActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Small transparent 2X icon badge at top
        if (isSpeedBoostActive || activeGesture is ActiveGesture.SpeedBoost) {
            SpeedBoostHud(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 10.dp)
            )
        }

        when (activeGesture) {
            is ActiveGesture.Brightness -> {
                BrightnessHud(
                    percent = activeGesture.percent,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                )
            }
            is ActiveGesture.Volume -> {
                VolumeHud(
                    percent = activeGesture.percent,
                    isBoost = activeGesture.isBoost,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                )
            }
            is ActiveGesture.Seek -> {
                SeekHud(
                    targetMs = activeGesture.targetPositionMs,
                    deltaMs = activeGesture.deltaMs,
                    totalMs = activeGesture.totalDurationMs
                )
            }
            is ActiveGesture.Zoom -> {
                ZoomHud(scale = activeGesture.scale)
            }
            is ActiveGesture.DoubleTapSeek -> {
                DoubleTapSeekHud(
                    isForward = activeGesture.isForward,
                    seconds = activeGesture.seconds,
                    modifier = Modifier.align(
                        if (activeGesture.isForward) Alignment.CenterEnd else Alignment.CenterStart
                    )
                )
            }
            is ActiveGesture.SpeedBoost, null -> {
                // Handled above
            }
        }
    }
}

@Composable
fun BrightnessHud(
    percent: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(GestureHudBg)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = "Brightness",
            tint = AmberPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        // Vertical progress track
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight((percent / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(AmberLight, AmberPrimary)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "$percent%",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun VolumeHud(
    percent: Int,
    isBoost: Boolean,
    modifier: Modifier = Modifier
) {
    val icon = when {
        percent <= 0 -> Icons.Default.VolumeMute
        percent < 50 -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }
    val barColor = if (isBoost || percent > 100) CyanAccent else AmberPrimary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(GestureHudBg)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
            .padding(horizontal = 14.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Volume",
            tint = barColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        // Vertical track
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val progress = (percent / 150f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (percent > 100) "Boost $percent%" else "$percent%",
            color = if (percent > 100) CyanAccent else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SeekHud(
    targetMs: Long,
    deltaMs: Long,
    totalMs: Long,
    modifier: Modifier = Modifier
) {
    val deltaFormatted = if (deltaMs >= 0) {
        "+${formatTime(deltaMs)}"
    } else {
        "-${formatTime(-deltaMs)}"
    }
    val deltaColor = if (deltaMs >= 0) CyanAccent else AmberPrimary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GestureHudBg)
            .border(1.5.dp, Color(0x4000E5FF), RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTime(targetMs),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "[$deltaFormatted]",
                color = deltaColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "/ ${formatTime(totalMs)}",
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { if (totalMs > 0) (targetMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f },
            modifier = Modifier
                .width(180.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = CyanAccent,
            trackColor = Color(0x33FFFFFF)
        )
    }
}

@Composable
fun SpeedBoostHud(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x66000000)) // Translucent semi-transparent
            .border(0.75.dp, Color(0x40FFFFFF), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FastForward,
            contentDescription = "2x Speed",
            tint = AmberPrimary.copy(alpha = 0.95f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "2X",
            color = Color.White.copy(alpha = 0.95f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ZoomHud(
    scale: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(GestureHudBg)
            .border(1.dp, CyanAccent, CircleShape)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Zoom: ${(scale * 100).toInt()}%",
            color = CyanAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DoubleTapSeekHud(
    isForward: Boolean,
    seconds: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 40.dp)
            .size(90.dp)
            .clip(CircleShape)
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = if (isForward) "Forward" else "Rewind",
                tint = CyanAccent,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (isForward) "+" else "-"}${seconds}s",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun formatTime(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSec = timeMs / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hrs = totalSec / 3600
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
