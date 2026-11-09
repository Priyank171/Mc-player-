package com.example

import com.example.data.model.ResizeMode
import com.example.data.model.VideoItem
import com.example.util.SubtitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSubtitleParsingAndSync() {
        val srt = """
            1
            00:00:01,500 --> 00:00:04,500
            Welcome to MC Player!
            
            2
            00:00:05,000 --> 00:00:09,000
            Swipe left for brightness, right for volume.
        """.trimIndent()

        val cues = SubtitleParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals(1500L, cues[0].startTimeMs)
        assertEquals(4500L, cues[0].endTimeMs)
        assertEquals("Welcome to MC Player!", cues[0].text)

        // Test active lookup at 3.0s
        val active = SubtitleParser.findActiveCue(cues, 3000L, 0L)
        assertEquals("Welcome to MC Player!", active)

        // Test sync offset of +2000ms at 3.5s -> effective 5.5s, which hits cue #2!
        val delayedActive = SubtitleParser.findActiveCue(cues, 3500L, 2000L)
        assertEquals("Swipe left for brightness, right for volume.", delayedActive)

        // Out of range
        val none = SubtitleParser.findActiveCue(cues, 15000L, 0L)
        assertEquals("", none)
    }

    @Test
    fun testVideoFormatting() {
        val video = VideoItem(
            id = "demo_1",
            title = "Test Movie",
            uriString = "http://example.com/video.mp4",
            durationMs = 3665000L, // 1h 1m 5s
            sizeBytes = 1024 * 1024 * 500L // 500MB
        )

        assertEquals("01:01:05", video.formattedDuration())
        assertEquals("500.0 MB", video.formattedSize())
    }
}
