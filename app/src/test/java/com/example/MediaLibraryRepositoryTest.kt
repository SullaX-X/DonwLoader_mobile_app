package com.example

import com.example.data.DownloadFormat
import com.example.data.MediaLibraryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryRepositoryTest {

    @Test
    fun extractUrlsFromText_parsesMixedBatchAndDeduplicates() {
        val raw = """
            # Comment line https://ignored.example.com
            https://www.youtube.com/watch?v=abc123
            Check this out: https://vimeo.com/987654,
            https://www.youtube.com/watch?v=abc123
            not a url
        """.trimIndent()

        val urls = MediaLibraryRepository.extractUrlsFromText(raw)
        assertEquals(
            listOf(
                "https://www.youtube.com/watch?v=abc123",
                "https://vimeo.com/987654"
            ),
            urls
        )
    }

    @Test
    fun sanitizeFilename_replacesIllegalCharacters() {
        val sanitized = MediaLibraryRepository.sanitizeFilename("My: Video/Title*? <2026>.mp4")
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("*"))
        assertFalse(sanitized.contains("?"))
        assertTrue(sanitized.endsWith(".mp4"))
    }

    @Test
    fun formatEta_formatsMinutesAndHours() {
        assertEquals("", MediaLibraryRepository.formatEta(0))
        assertEquals("01:05", MediaLibraryRepository.formatEta(65))
        assertEquals("01:02:03", MediaLibraryRepository.formatEta(3723))
    }

    @Test
    fun extractSpeedFromLine_extractsYtdlpSpeed() {
        val line = "[download]  42.1% of ~ 120.50MiB at  8.45MiB/s ETA 00:08"
        assertEquals("8.45MiB/s", MediaLibraryRepository.extractSpeedFromLine(line))
        assertNull(MediaLibraryRepository.extractSpeedFromLine("Connecting to server..."))
    }

    @Test
    fun buildDisplayNameFromUrl_usesVideoIdAndExtension() {
        val name = MediaLibraryRepository.buildDisplayNameFromUrl(
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            DownloadFormat.MP3
        )
        assertEquals("youtube.com_dQw4w9WgXcQ.mp3", name)
    }
}
