package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.ActiveGesture
import com.example.data.model.PlayerUiState
import com.example.data.model.ResizeMode
import com.example.data.model.SubtitleConfig
import com.example.data.model.SubtitleTrack
import com.example.data.model.VideoItem
import com.example.data.repository.MediaRepository
import com.example.util.SubtitleParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MCPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Library State
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isLoadingVideos = MutableStateFlow(true)
    val isLoadingVideos: StateFlow<Boolean> = _isLoadingVideos.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    // Player State
    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    private var positionTrackerJob: Job? = null
    private var controlsTimeoutJob: Job? = null
    private var gestureDismissJob: Job? = null

    // Cached pre-gesture values
    private var preGestureSeekTarget: Long = 0L
    private var preLongPressSpeed: Float = 1.0f

    init {
        initPlayer()
        loadVideos()
    }

    private fun initPlayer() {
        val player = ExoPlayer.Builder(getApplication()).build()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = player.duration.coerceAtLeast(0L)
                _playerState.update { it.copy(durationMs = duration) }
                if (playbackState == Player.STATE_ENDED) {
                    if (_playerState.value.isLooping) {
                        player.seekTo(0)
                        player.play()
                    } else {
                        _playerState.update { it.copy(isPlaying = false) }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startPositionTracker()
                    resetControlsTimer()
                } else {
                    stopPositionTracker()
                    saveCurrentPosition()
                }
            }
        })
        exoPlayer = player

        // Init initial volume
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volPercent = if (maxVol > 0) (curVol * 100) / maxVol else 50
        _playerState.update { it.copy(volumePercent = volPercent) }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoadingVideos.value = true
            val list = repository.getVideos()
            _videos.value = list
            _isLoadingVideos.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    fun playVideo(video: VideoItem) {
        val player = exoPlayer ?: return
        viewModelScope.launch {
            val offset = repository.getSubtitleOffset(video.id)
            val sampleSubtitles = if (video.isSample) repository.getSampleSubtitles(video.id) else emptyList()
            val initialTrackId = sampleSubtitles.firstOrNull()?.id

            _playerState.update {
                it.copy(
                    currentVideo = video,
                    currentPositionMs = video.lastPositionMs,
                    durationMs = video.durationMs,
                    subtitleTracks = sampleSubtitles,
                    selectedSubtitleTrackId = initialTrackId,
                    subtitleConfig = it.subtitleConfig.copy(syncOffsetMs = offset),
                    areControlsVisible = true,
                    isScreenLocked = false,
                    zoomScale = 1.0f,
                    zoomPanX = 0f,
                    zoomPanY = 0f
                )
            }

            val mediaItem = MediaItem.fromUri(video.uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            if (video.lastPositionMs > 0 && video.lastPositionMs < video.durationMs - 3000) {
                player.seekTo(video.lastPositionMs)
            }
            player.play()
        }
    }

    fun playVideoFromUri(uri: Uri, name: String) {
        val item = VideoItem(
            id = "picker_${System.currentTimeMillis()}",
            title = name,
            uriString = uri.toString(),
            durationMs = 0L,
            sizeBytes = 0L,
            resolution = "Custom File",
            folderName = "Storage Picker",
            thumbnailUri = uri.toString(),
            isSample = false
        )
        playVideo(item)
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        resetControlsTimer()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs.coerceAtLeast(0L))
        player.seekTo(clamped)
        _playerState.update { it.copy(currentPositionMs = clamped) }
        resetControlsTimer()
    }

    fun seekRelative(deltaMs: Long) {
        val current = _playerState.value.currentPositionMs
        seekTo(current + deltaMs)
    }

    // GESTURES IMPLEMENTATION

    fun onVerticalDrag(isLeftHalf: Boolean, deltaFractionY: Float, onBrightnessChanged: (Float) -> Unit) {
        if (_playerState.value.isScreenLocked) return

        // Note: Swiping UP gives negative delta, so we subtract
        val delta = -deltaFractionY * 100

        if (isLeftHalf) {
            // Brightness control
            val current = _playerState.value.brightnessPercent
            val next = (current + delta.toInt()).coerceIn(0, 100)
            _playerState.update {
                it.copy(
                    brightnessPercent = next,
                    activeGesture = ActiveGesture.Brightness(next)
                )
            }
            onBrightnessChanged(next / 100f)
        } else {
            // Volume control with audio boost support
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = _playerState.value.volumePercent
            val maxAllowed = if (_playerState.value.isAudioBoostEnabled) 150 else 100
            val next = (current + delta.toInt()).coerceIn(0, maxAllowed)

            if (next <= 100) {
                val streamVol = (next * maxVol) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                exoPlayer?.volume = 1.0f
            } else {
                // Boost mode: system volume is max, software volume scales 1.0 to 1.5
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                val boostScale = 1.0f + ((next - 100) / 100f)
                exoPlayer?.volume = boostScale
            }

            _playerState.update {
                it.copy(
                    volumePercent = next,
                    activeGesture = ActiveGesture.Volume(next, isBoost = next > 100)
                )
            }
        }

        scheduleGestureDismiss()
    }

    fun onHorizontalDragStart() {
        if (_playerState.value.isScreenLocked) return
        preGestureSeekTarget = _playerState.value.currentPositionMs
    }

    fun onHorizontalDrag(deltaFractionX: Float) {
        if (_playerState.value.isScreenLocked) return
        val totalMs = _playerState.value.durationMs.coerceAtLeast(1L)
        // Full screen swipe traverses 90 seconds or 10% of video
        val seekSpan = (totalMs * 0.15f).toLong().coerceIn(30000L, 180000L)
        val deltaMs = (deltaFractionX * seekSpan).toLong()
        val targetMs = (preGestureSeekTarget + deltaMs).coerceIn(0L, totalMs)

        _playerState.update {
            it.copy(
                activeGesture = ActiveGesture.Seek(targetMs, deltaMs, totalMs)
            )
        }
        preGestureSeekTarget = targetMs
        scheduleGestureDismiss()
    }

    fun onHorizontalDragEnd() {
        if (_playerState.value.isScreenLocked) return
        val gesture = _playerState.value.activeGesture
        if (gesture is ActiveGesture.Seek) {
            seekTo(gesture.targetPositionMs)
        }
        scheduleGestureDismiss(delayMs = 600)
    }

    fun onDoubleTap(xRatio: Float) {
        if (_playerState.value.isScreenLocked) return
        when {
            xRatio < 0.33f -> {
                seekRelative(-10000L)
                _playerState.update {
                    it.copy(activeGesture = ActiveGesture.DoubleTapSeek(isForward = false, seconds = 10))
                }
            }
            xRatio > 0.66f -> {
                seekRelative(10000L)
                _playerState.update {
                    it.copy(activeGesture = ActiveGesture.DoubleTapSeek(isForward = true, seconds = 10))
                }
            }
            else -> {
                togglePlayPause()
            }
        }
        scheduleGestureDismiss(delayMs = 800)
    }

    fun onLongPressStart() {
        if (_playerState.value.isScreenLocked) return
        gestureDismissJob?.cancel()
        preLongPressSpeed = _playerState.value.playbackSpeed
        exoPlayer?.setPlaybackSpeed(2.0f)
        _playerState.update {
            it.copy(
                isSpeedBoostActive = true,
                activeGesture = ActiveGesture.SpeedBoost
            )
        }
    }

    fun onLongPressEnd() {
        if (_playerState.value.isScreenLocked) return
        exoPlayer?.setPlaybackSpeed(preLongPressSpeed)
        _playerState.update {
            it.copy(
                isSpeedBoostActive = false,
                activeGesture = if (it.activeGesture is ActiveGesture.SpeedBoost) null else it.activeGesture
            )
        }
    }

    fun onPinchZoom(scaleMultiplier: Float) {
        if (_playerState.value.isScreenLocked) return
        val currentScale = _playerState.value.zoomScale
        val newScale = (currentScale * scaleMultiplier).coerceIn(1.0f, 3.0f)
        _playerState.update {
            it.copy(
                zoomScale = newScale,
                activeGesture = ActiveGesture.Zoom(newScale)
            )
        }
        scheduleGestureDismiss(delayMs = 1000)
    }

    fun onSingleTap() {
        if (_playerState.value.isScreenLocked) {
            // Flash unlock icon if locked
            return
        }
        val next = !_playerState.value.areControlsVisible
        _playerState.update { it.copy(areControlsVisible = next) }
        if (next) resetControlsTimer()
    }

    fun toggleScreenLock() {
        val nextLock = !_playerState.value.isScreenLocked
        _playerState.update {
            it.copy(
                isScreenLocked = nextLock,
                areControlsVisible = !nextLock,
                activeGesture = null
            )
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun setResizeMode(mode: ResizeMode) {
        _playerState.update { it.copy(resizeMode = mode, zoomScale = 1.0f) }
    }

    fun cycleAspectRatio() {
        val modes = ResizeMode.values()
        val nextIndex = (modes.indexOf(_playerState.value.resizeMode) + 1) % modes.size
        setResizeMode(modes[nextIndex])
    }

    fun toggleLooping(loop: Boolean) {
        _playerState.update { it.copy(isLooping = loop) }
    }

    fun toggleAudioBoost(boost: Boolean) {
        _playerState.update { it.copy(isAudioBoostEnabled = boost) }
    }

    // SUBTITLE CONTROLS

    fun selectSubtitleTrack(trackId: String?) {
        _playerState.update {
            it.copy(
                selectedSubtitleTrackId = trackId,
                currentSubtitleText = if (trackId == null) "" else it.currentSubtitleText
            )
        }
    }

    fun loadExternalSubtitle(uri: Uri, name: String) {
        viewModelScope.launch {
            val track = repository.parseExternalSubtitle(uri, name)
            if (track != null) {
                _playerState.update {
                    it.copy(
                        subtitleTracks = it.subtitleTracks + track,
                        selectedSubtitleTrackId = track.id,
                        subtitleConfig = it.subtitleConfig.copy(isEnabled = true)
                    )
                }
            }
        }
    }

    fun toggleSubtitles(enabled: Boolean) {
        _playerState.update {
            it.copy(subtitleConfig = it.subtitleConfig.copy(isEnabled = enabled))
        }
    }

    fun setSubtitleSyncOffset(offsetMs: Long) {
        val videoId = _playerState.value.currentVideo?.id ?: return
        _playerState.update {
            it.copy(subtitleConfig = it.subtitleConfig.copy(syncOffsetMs = offsetMs))
        }
        viewModelScope.launch {
            repository.saveSubtitleOffset(videoId, offsetMs)
        }
    }

    fun setSubtitleFontSize(size: Float) {
        _playerState.update {
            it.copy(subtitleConfig = it.subtitleConfig.copy(fontSizeSp = size))
        }
    }

    fun setSubtitleTextColor(colorHex: Long) {
        _playerState.update {
            it.copy(subtitleConfig = it.subtitleConfig.copy(textColorHex = colorHex))
        }
    }

    fun setSubtitleSheetVisible(visible: Boolean) {
        _playerState.update { it.copy(showSubtitleSheet = visible) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _playerState.update { it.copy(showSettingsSheet = visible) }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
            loadVideos()
        }
    }

    // TIMERS & ENGINE HELPERS

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = viewModelScope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val buf = player.bufferedPosition.coerceAtLeast(0L)

                    // Lookup active subtitle
                    val activeTrack = _playerState.value.subtitleTracks.firstOrNull {
                        it.id == _playerState.value.selectedSubtitleTrackId
                    }
                    val subText = if (activeTrack != null && _playerState.value.subtitleConfig.isEnabled) {
                        SubtitleParser.findActiveCue(
                            activeTrack.cues,
                            pos,
                            _playerState.value.subtitleConfig.syncOffsetMs
                        )
                    } else ""

                    _playerState.update {
                        it.copy(
                            currentPositionMs = pos,
                            bufferedPositionMs = buf,
                            currentSubtitleText = subText
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopPositionTracker() {
        positionTrackerJob?.cancel()
    }

    private fun resetControlsTimer() {
        controlsTimeoutJob?.cancel()
        controlsTimeoutJob = viewModelScope.launch {
            delay(4000)
            if (_playerState.value.isPlaying && !_playerState.value.showSubtitleSheet && !_playerState.value.showSettingsSheet) {
                _playerState.update { it.copy(areControlsVisible = false) }
            }
        }
    }

    private fun scheduleGestureDismiss(delayMs: Long = 1200) {
        gestureDismissJob?.cancel()
        gestureDismissJob = viewModelScope.launch {
            delay(delayMs)
            _playerState.update { it.copy(activeGesture = null) }
        }
    }

    private fun saveCurrentPosition() {
        val video = _playerState.value.currentVideo ?: return
        val pos = _playerState.value.currentPositionMs
        val dur = _playerState.value.durationMs
        viewModelScope.launch {
            repository.savePlaybackPosition(video, pos, dur)
        }
    }

    fun stopPlayback() {
        saveCurrentPosition()
        exoPlayer?.stop()
        _playerState.update {
            it.copy(
                currentVideo = null,
                isPlaying = false,
                currentPositionMs = 0L,
                activeGesture = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentPosition()
        positionTrackerJob?.cancel()
        controlsTimeoutJob?.cancel()
        gestureDismissJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
