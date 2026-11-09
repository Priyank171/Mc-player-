package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class VideoEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val uriString: String,
    val durationMs: Long,
    val lastPositionMs: Long,
    val lastPlayedTimestamp: Long,
    val isFavorite: Boolean = false,
    val subtitleOffsetMs: Long = 0L
)
