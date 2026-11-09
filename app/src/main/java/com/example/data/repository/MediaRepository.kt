package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.local.AppDatabase
import com.example.data.local.VideoEntity
import com.example.data.model.SubtitleTrack
import com.example.data.model.VideoItem
import com.example.util.SubtitleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MediaRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val playbackDao = database.playbackDao()

    val playbackHistory: Flow<List<VideoEntity>> = playbackDao.getAllHistory()

    /**
     * Scans device video files and merges them with history and bundled samples.
     */
    suspend fun getVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val deviceVideos = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        try {
            val query = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            query?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val resCol = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val title = cursor.getString(titleCol) ?: cursor.getString(nameCol) ?: "Video $id"
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val resolution = if (resCol != -1) cursor.getString(resCol) ?: "1080p" else "1080p"
                    val folder = if (bucketCol != -1) cursor.getString(bucketCol) ?: "Videos" else "Videos"

                    deviceVideos.add(
                        VideoItem(
                            id = "local_$id",
                            title = title,
                            uriString = contentUri.toString(),
                            durationMs = duration,
                            sizeBytes = size,
                            resolution = resolution,
                            folderName = folder,
                            thumbnailUri = contentUri.toString(),
                            isSample = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Merge with database last position
        val historyMap = try {
            playbackDao.getAllHistory().firstOrNull()?.associateBy { it.videoId } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val enrichedDeviceVideos = deviceVideos.map { item ->
            val record = historyMap[item.id]
            if (record != null) {
                item.copy(
                    lastPositionMs = record.lastPositionMs,
                    isFavorite = record.isFavorite
                )
            } else item
        }

        val sampleVideos = getSampleVideos().map { item ->
            val record = historyMap[item.id]
            if (record != null) {
                item.copy(
                    lastPositionMs = record.lastPositionMs,
                    isFavorite = record.isFavorite
                )
            } else item
        }

        // Return combined list: local videos first, then offline showcase samples
        enrichedDeviceVideos + sampleVideos
    }

    suspend fun savePlaybackPosition(video: VideoItem, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        playbackDao.insertOrUpdate(
            VideoEntity(
                videoId = video.id,
                title = video.title,
                uriString = video.uriString,
                durationMs = if (durationMs > 0) durationMs else video.durationMs,
                lastPositionMs = positionMs,
                lastPlayedTimestamp = System.currentTimeMillis(),
                isFavorite = video.isFavorite
            )
        )
    }

    suspend fun toggleFavorite(video: VideoItem) = withContext(Dispatchers.IO) {
        val newFav = !video.isFavorite
        playbackDao.setFavorite(video.id, newFav)
    }

    suspend fun getSubtitleOffset(videoId: String): Long = withContext(Dispatchers.IO) {
        playbackDao.getHistoryById(videoId)?.subtitleOffsetMs ?: 0L
    }

    suspend fun saveSubtitleOffset(videoId: String, offsetMs: Long) = withContext(Dispatchers.IO) {
        playbackDao.updateSubtitleOffset(videoId, offsetMs)
    }

    /**
     * Reads external subtitle file from URI and parses it into SubtitleTrack.
     */
    suspend fun parseExternalSubtitle(uri: Uri, fileName: String): SubtitleTrack? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val rawText = reader.readText()
            reader.close()
            val cues = SubtitleParser.parse(rawText)
            SubtitleTrack(
                id = "ext_${System.currentTimeMillis()}",
                label = fileName,
                language = "External",
                content = rawText,
                uriString = uri.toString(),
                isExternal = true,
                cues = cues
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * High quality showcase videos with pre-bundled sample subtitles.
     */
    fun getSampleVideos(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "sample_bbb",
                title = "Big Buck Bunny (1080p Cinema Demo)",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                durationMs = 596000L,
                sizeBytes = 158 * 1024 * 1024L,
                resolution = "1920x1080 (60fps)",
                folderName = "Showcase Demos",
                thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                isSample = true
            ),
            VideoItem(
                id = "sample_sintel",
                title = "Sintel (Fantasy Animated Short)",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                durationMs = 888000L,
                sizeBytes = 210 * 1024 * 1024L,
                resolution = "1920x1080 (HD)",
                folderName = "Showcase Demos",
                thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                isSample = true
            ),
            VideoItem(
                id = "sample_tears",
                title = "Tears of Steel (Sci-Fi VFX Short)",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                durationMs = 734000L,
                sizeBytes = 192 * 1024 * 1024L,
                resolution = "4K Cinema",
                folderName = "Showcase Demos",
                thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                isSample = true
            ),
            VideoItem(
                id = "sample_elephants",
                title = "Elephants Dream (Open Movie Project)",
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                durationMs = 653000L,
                sizeBytes = 145 * 1024 * 1024L,
                resolution = "1080p Surround",
                folderName = "Showcase Demos",
                thumbnailUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                isSample = true
            )
        )
    }

    /**
     * Default bundled subtitle tracks for demo videos.
     */
    fun getSampleSubtitles(videoId: String): List<SubtitleTrack> {
        val sampleSrt = """
1
00:00:01,000 --> 00:00:05,000
Blender Foundation presents: A Open Movie Showcase.

2
00:00:06,000 --> 00:00:10,500
Enjoy full gesture controls: swipe left for brightness, right for volume.

3
00:00:11,000 --> 00:00:16,000
Swipe horizontally across the screen to scrub seek forward or backward.

4
00:00:17,000 --> 00:00:22,000
Double tap left or right to skip 10 seconds. Hold to play at 2.0x speed!

5
00:00:23,000 --> 00:00:28,000
Pinch with two fingers to zoom in up to 300% or pan around the video.

6
00:00:29,000 --> 00:00:35,000
Subtitles support live sync timing adjustments and custom styling.

7
00:00:36,000 --> 00:00:42,000
(Forest ambiance and birds chirping)

8
00:00:43,000 --> 00:00:49,000
A large rabbit emerges peacefully into the sunlit woodland clearing.

9
00:00:50,000 --> 00:00:58,000
Look at the butterflies fluttering around his ears!

10
00:01:00,000 --> 00:01:08,000
Three mischievous forest critters watch from the branches above...
        """.trimIndent()

        val sampleSpanishSrt = """
1
00:00:01,000 --> 00:00:05,000
La Fundación Blender presenta una película de código abierto.

2
00:00:06,000 --> 00:00:10,500
Desliza hacia arriba a la izquierda para el brillo, a la derecha para el volumen.

3
00:00:11,000 --> 00:00:16,000
Desliza horizontalmente para buscar cualquier segundo del vídeo.

4
00:00:17,000 --> 00:00:22,000
Toca dos veces para avanzar 10s. ¡Mantén presionado para velocidad 2x!

5
00:00:23,000 --> 00:00:28,000
Pellizca con dos dedos para hacer zoom y desplazarte por la pantalla.
        """.trimIndent()

        return listOf(
            SubtitleTrack(
                id = "track_en",
                label = "English (Tutorial & Dialogue)",
                language = "en",
                content = sampleSrt,
                isExternal = false,
                cues = SubtitleParser.parse(sampleSrt)
            ),
            SubtitleTrack(
                id = "track_es",
                label = "Español (Subtítulos)",
                language = "es",
                content = sampleSpanishSrt,
                isExternal = false,
                cues = SubtitleParser.parse(sampleSpanishSrt)
            )
        )
    }
}
