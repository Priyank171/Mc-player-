package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ResizeMode
import com.example.ui.components.CustomSubtitleOverlay
import com.example.ui.components.GestureHudOverlays
import com.example.ui.components.PlaybackSettingsBottomSheet
import com.example.ui.components.PlayerControls
import com.example.ui.components.SubtitleBottomSheet
import com.example.viewmodel.MCPlayerViewModel
import kotlin.math.abs

@Composable
fun PlayerScreen(
    viewModel: MCPlayerViewModel,
    onBack: () -> Unit,
    onPickSubtitleFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.playerState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    BackHandler {
        viewModel.stopPlayback()
        onBack()
    }

    // Keep screen ON during playback and manage window brightness
    DisposableEffect(state.isPlaying) {
        val window = activity?.window
        if (state.isPlaying) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Restore system auto brightness
            window?.attributes = window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    var componentWidth by remember { mutableFloatStateOf(1080f) }
    var componentHeight by remember { mutableFloatStateOf(1920f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged {
                componentWidth = it.width.toFloat()
                componentHeight = it.height.toFloat()
            }
    ) {
        // 1. VIDEO SURFACE VIEW (ExoPlayer PlayerView)
        val exoPlayer = viewModel.exoPlayer
        if (exoPlayer != null) {
            val scale = state.zoomScale
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false // We use our custom Compose PlayerControls
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { playerView ->
                        playerView.player = exoPlayer
                        playerView.resizeMode = when (state.resizeMode) {
                            ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            ResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                            ResizeMode.FIXED_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. GESTURE TOUCH DISPATCHER LAYER
        // Handles: Drag (vertical brightness/volume, horizontal seek), Pinch Zoom, Double-tap seek, and Long-press speed boost
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Transform gestures for Two-Finger Pinch Zoom
                .pointerInput(state.isScreenLocked) {
                    if (!state.isScreenLocked) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (abs(zoom - 1f) > 0.02f) {
                                viewModel.onPinchZoom(zoom)
                            }
                        }
                    }
                }
                // Tap, Double-tap, and Long-press (2x speed hold)
                .pointerInput(state.isScreenLocked) {
                    detectTapGestures(
                        onTap = {
                            viewModel.onSingleTap()
                        },
                        onDoubleTap = { offset ->
                            val xRatio = offset.x / componentWidth
                            viewModel.onDoubleTap(xRatio)
                        },
                        onLongPress = {
                            viewModel.onLongPressStart()
                        },
                        onPress = {
                            try {
                                tryAwaitRelease()
                            } finally {
                                if (viewModel.playerState.value.isSpeedBoostActive) {
                                    viewModel.onLongPressEnd()
                                }
                            }
                        }
                    )
                }
                // Drag gestures: Left vertical (brightness), Right vertical (volume), Horizontal (seek)
                .pointerInput(state.isScreenLocked) {
                    if (!state.isScreenLocked) {
                        var startX = 0f
                        var startY = 0f
                        var isHorizontalSeeking = false
                        var isVerticalAdjusting = false
                        var isLeftHalf = false

                        detectDragGestures(
                            onDragStart = { offset ->
                                startX = offset.x
                                startY = offset.y
                                isLeftHalf = offset.x < (componentWidth / 2f)
                                isHorizontalSeeking = false
                                isVerticalAdjusting = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val totalDeltaX = change.position.x - startX
                                val totalDeltaY = change.position.y - startY

                                if (!isHorizontalSeeking && !isVerticalAdjusting) {
                                    if (abs(totalDeltaX) > 25f && abs(totalDeltaX) > abs(totalDeltaY)) {
                                        isHorizontalSeeking = true
                                        viewModel.onHorizontalDragStart()
                                    } else if (abs(totalDeltaY) > 25f) {
                                        isVerticalAdjusting = true
                                    }
                                }

                                if (isHorizontalSeeking) {
                                    val deltaFraction = dragAmount.x / componentWidth
                                    viewModel.onHorizontalDrag(deltaFraction)
                                } else if (isVerticalAdjusting) {
                                    val deltaFractionY = dragAmount.y / componentHeight
                                    viewModel.onVerticalDrag(
                                        isLeftHalf = isLeftHalf,
                                        deltaFractionY = deltaFractionY,
                                        onBrightnessChanged = { brightness ->
                                            val lp = activity?.window?.attributes
                                            lp?.screenBrightness = brightness
                                            activity?.window?.attributes = lp
                                        }
                                    )
                                }
                            },
                            onDragEnd = {
                                if (isHorizontalSeeking) {
                                    viewModel.onHorizontalDragEnd()
                                }
                                isHorizontalSeeking = false
                                isVerticalAdjusting = false
                            },
                            onDragCancel = {
                                isHorizontalSeeking = false
                                isVerticalAdjusting = false
                            }
                        )
                    }
                }
        )

        // 3. SUBTITLE TEXT OVERLAY
        CustomSubtitleOverlay(
            subtitleText = state.currentSubtitleText,
            config = state.subtitleConfig
        )

        // 4. GESTURE HUD OVERLAYS (Brightness, Volume, Seek, Speed, Zoom)
        GestureHudOverlays(
            activeGesture = state.activeGesture,
            isSpeedBoostActive = state.isSpeedBoostActive
        )

        // 5. PLAYER ON-SCREEN CONTROLS (Top Bar, Center Play, Bottom Scrub)
        PlayerControls(
            state = state,
            onBack = {
                viewModel.stopPlayback()
                onBack()
            },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeek = { pos -> viewModel.seekTo(pos) },
            onSeekRelative = { delta -> viewModel.seekRelative(delta) },
            onToggleLock = { viewModel.toggleScreenLock() },
            onOpenSubtitles = { viewModel.setSubtitleSheetVisible(true) },
            onOpenSettings = { viewModel.setSettingsSheetVisible(true) },
            onCycleAspectRatio = { viewModel.cycleAspectRatio() },
            onTogglePip = {
                if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    try {
                        activity.enterPictureInPictureMode(
                            android.app.PictureInPictureParams.Builder().build()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )

        // 6. SUBTITLE BOTTOM SHEET
        if (state.showSubtitleSheet) {
            SubtitleBottomSheet(
                tracks = state.subtitleTracks,
                selectedTrackId = state.selectedSubtitleTrackId,
                config = state.subtitleConfig,
                onTrackSelected = { trackId -> viewModel.selectSubtitleTrack(trackId) },
                onPickExternalFile = onPickSubtitleFile,
                onToggleEnabled = { enabled -> viewModel.toggleSubtitles(enabled) },
                onUpdateSyncOffset = { offset -> viewModel.setSubtitleSyncOffset(offset) },
                onUpdateFontSize = { size -> viewModel.setSubtitleFontSize(size) },
                onUpdateTextColor = { colorHex -> viewModel.setSubtitleTextColor(colorHex) },
                onDismiss = { viewModel.setSubtitleSheetVisible(false) }
            )
        }

        // 7. PLAYBACK SETTINGS BOTTOM SHEET
        if (state.showSettingsSheet) {
            PlaybackSettingsBottomSheet(
                currentSpeed = state.playbackSpeed,
                currentResizeMode = state.resizeMode,
                isLooping = state.isLooping,
                isAudioBoost = state.isAudioBoostEnabled,
                onSpeedSelected = { speed -> viewModel.setPlaybackSpeed(speed) },
                onResizeModeSelected = { mode -> viewModel.setResizeMode(mode) },
                onToggleLooping = { loop -> viewModel.toggleLooping(loop) },
                onToggleAudioBoost = { boost -> viewModel.toggleAudioBoost(boost) },
                onDismiss = { viewModel.setSettingsSheetVisible(false) }
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
