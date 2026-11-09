package com.example.data.model

import android.net.Uri

enum class ResizeMode(val label: String) {
    FIT("Fit to Screen"),
    FILL("Fill / Crop"),
    ZOOM("Zoom 150%"),
    FIXED_16_9("16:9"),
    FIXED_4_3("4:3")
}

data class VideoItem(
    val id: String,
    val title: String,
    val uriString: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val resolution: String = "1080p",
    val folderName: String = "Internal",
    val thumbnailUri: String? = null,
    val isSample: Boolean = false,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false
) {
    val uri: Uri get() = Uri.parse(uriString)

    fun formattedDuration(): String {
        if (durationMs <= 0) return "--:--"
        val totalSec = durationMs / 1000
        val sec = totalSec % 60
        val min = (totalSec / 60) % 60
        val hrs = totalSec / 3600
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, min, sec)
        } else {
            String.format("%02d:%02d", min, sec)
        }
    }

    fun formattedSize(): String {
        if (sizeBytes <= 0) return "Online / Bundled"
        val mb = sizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            String.format("%.2f GB", mb / 1024.0)
        } else {
            String.format("%.1f MB", mb)
        }
    }
}

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String = "en",
    val content: String? = null,
    val uriString: String? = null,
    val isExternal: Boolean = false,
    val cues: List<SubtitleCue> = emptyList()
)

data class SubtitleConfig(
    val isEnabled: Boolean = true,
    val fontSizeSp: Float = 19f,
    val textColorHex: Long = 0xFFFFFFFF,
    val syncOffsetMs: Long = 0L,
    val backgroundOpacity: Float = 0.65f,
    val verticalOffsetPercent: Float = 0.08f // Distance from bottom
)

sealed class ActiveGesture {
    data class Brightness(val percent: Int) : ActiveGesture()
    data class Volume(val percent: Int, val isBoost: Boolean = false) : ActiveGesture()
    data class Seek(val targetPositionMs: Long, val deltaMs: Long, val totalDurationMs: Long) : ActiveGesture()
    data class Zoom(val scale: Float) : ActiveGesture()
    object SpeedBoost : ActiveGesture()
    data class DoubleTapSeek(val isForward: Boolean, val seconds: Int) : ActiveGesture()
}

data class PlayerUiState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isSpeedBoostActive: Boolean = false,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val zoomScale: Float = 1.0f,
    val zoomPanX: Float = 0f,
    val zoomPanY: Float = 0f,
    val brightnessPercent: Int = 60,
    val volumePercent: Int = 70,
    val activeGesture: ActiveGesture? = null,
    val areControlsVisible: Boolean = true,
    val isScreenLocked: Boolean = false,
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitleTrackId: String? = null,
    val currentSubtitleText: String = "",
    val subtitleConfig: SubtitleConfig = SubtitleConfig(),
    val isAudioBoostEnabled: Boolean = false,
    val isLooping: Boolean = false,
    val showSubtitleSheet: Boolean = false,
    val showSettingsSheet: Boolean = false
)
