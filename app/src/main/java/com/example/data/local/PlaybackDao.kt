package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM playback_history WHERE videoId = :id LIMIT 1")
    suspend fun getHistoryById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: VideoEntity)

    @Query("UPDATE playback_history SET lastPositionMs = :positionMs, lastPlayedTimestamp = :timestamp WHERE videoId = :id")
    suspend fun updatePosition(id: String, positionMs: Long, timestamp: Long)

    @Query("UPDATE playback_history SET isFavorite = :isFav WHERE videoId = :id")
    suspend fun setFavorite(id: String, isFav: Boolean)

    @Query("UPDATE playback_history SET subtitleOffsetMs = :offsetMs WHERE videoId = :id")
    suspend fun updateSubtitleOffset(id: String, offsetMs: Long)

    @Query("DELETE FROM playback_history WHERE videoId = :id")
    suspend fun deleteHistory(id: String)
}
