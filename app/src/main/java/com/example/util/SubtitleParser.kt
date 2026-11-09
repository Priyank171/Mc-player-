package com.example.util

import com.example.data.model.SubtitleCue

object SubtitleParser {

    /**
     * Parses an SRT or WebVTT string into a sorted list of SubtitleCue objects.
     */
    fun parse(rawText: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        if (rawText.isBlank()) return cues

        // Normalize newlines
        val lines = rawText.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("NOTE")) {
                i++
                continue
            }

            // Check if current line or next line contains timestamp arrow "-->"
            var timestampLine = ""
            if (line.contains("-->")) {
                timestampLine = line
                i++
            } else if (i + 1 < lines.size && lines[i + 1].contains("-->")) {
                // Current line is cue index number, next line is timestamp
                timestampLine = lines[i + 1].trim()
                i += 2
            } else {
                i++
                continue
            }

            val times = timestampLine.split("-->")
            if (times.size == 2) {
                val startMs = parseTimestamp(times[0].trim())
                val endMs = parseTimestamp(times[1].trim().split(" ")[0]) // strip styling options if any

                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(stripHtmlTags(lines[i].trim()))
                    i++
                }

                val text = textBuilder.toString().trim()
                if (startMs >= 0 && endMs > startMs && text.isNotEmpty()) {
                    cues.add(SubtitleCue(startMs, endMs, text))
                }
            }
        }

        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseTimestamp(raw: String): Long {
        try {
            // Standard format: HH:mm:ss,SSS or HH:mm:ss.SSS or mm:ss.SSS
            val normalized = raw.replace(',', '.')
            val parts = normalized.split(":")
            return when (parts.size) {
                3 -> {
                    val hours = parts[0].toLongOrNull() ?: 0L
                    val minutes = parts[1].toLongOrNull() ?: 0L
                    val secParts = parts[2].split(".")
                    val seconds = secParts[0].toLongOrNull() ?: 0L
                    val millis = if (secParts.size > 1) {
                        secParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    } else 0L
                    (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
                }
                2 -> {
                    val minutes = parts[0].toLongOrNull() ?: 0L
                    val secParts = parts[1].split(".")
                    val seconds = secParts[0].toLongOrNull() ?: 0L
                    val millis = if (secParts.size > 1) {
                        secParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    } else 0L
                    (minutes * 60 + seconds) * 1000 + millis
                }
                else -> -1L
            }
        } catch (e: Exception) {
            return -1L
        }
    }

    private fun stripHtmlTags(input: String): String {
        return input.replace(Regex("<[^>]*>"), "")
    }

    /**
     * Binary search to find active cue matching (currentTimeMs + offsetMs).
     */
    fun findActiveCue(cues: List<SubtitleCue>, currentTimeMs: Long, syncOffsetMs: Long): String {
        if (cues.isEmpty()) return ""
        val targetMs = currentTimeMs + syncOffsetMs

        // Standard linear search or binary search
        var low = 0
        var high = cues.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]
            when {
                targetMs < cue.startTimeMs -> high = mid - 1
                targetMs > cue.endTimeMs -> low = mid + 1
                else -> return cue.text
            }
        }

        return ""
    }
}
