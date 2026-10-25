package com.axiomlabs.vidamacraft

import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadUrlValidatorTest {
    @Test fun acceptsAbsoluteHttpsUrl() { assertNull(DownloadUrlValidator.validate("https://example.com/video.mp4")) }
    @Test fun trimsWhitespace() { assertNull(DownloadUrlValidator.validate("  https://example.com/file.zip  ")) }
    @Test fun rejectsEmptyInput() { assertNotNull(DownloadUrlValidator.validate("  ")) }
    @Test fun rejectsPlainHttp() { assertNotNull(DownloadUrlValidator.validate("http://example.com/video.mp4")) }
    @Test fun rejectsRelativeUrl() { assertNotNull(DownloadUrlValidator.validate("/video.mp4")) }
    @Test fun rejectsEmbeddedCredentials() { assertNotNull(DownloadUrlValidator.validate("https://user:pass@example.com/file.mp4")) }

    @Test fun rejectsYoutubeShortPage() {
        val message = DownloadUrlValidator.validate("https://youtu.be/exampleId")
        assertNotNull(message)
        assertTrue(message!!.contains("direct HTTPS media URL"))
    }

    @Test fun rejectsYoutubeWatchPage() {
        assertNotNull(DownloadUrlValidator.validate("https://www.youtube.com/watch?v=exampleId"))
    }

    @Test fun acceptsOtherHttpsHosts() {
        assertNull(DownloadUrlValidator.validate("https://cdn.example.org/download?id=123"))
    }
}
