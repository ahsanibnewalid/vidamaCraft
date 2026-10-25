package com.axiomlabs.vidamacraft

import java.net.URI

/** Validates URL shape and rejects known video-sharing pages; it does not inspect response headers. */
object DownloadUrlValidator {
    fun validate(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return "Enter a direct download URL."
        return try {
            val uri = URI(value)
            when {
                !uri.isAbsolute -> "Use a complete URL beginning with https://."
                !uri.scheme.equals("https", ignoreCase = true) -> "Only HTTPS download links are supported."
                uri.host.isNullOrBlank() -> "The URL must include a valid host."
                uri.userInfo != null -> "URLs containing embedded credentials are not supported."
                isKnownStreamingPageHost(uri.host) ->
                    "This is a video-sharing page, not a direct file link. Paste a direct HTTPS media URL instead."
                else -> null
            }
        } catch (_: Exception) {
            "Enter a valid HTTPS URL."
        }
    }

    private fun isKnownStreamingPageHost(host: String): Boolean {
        val normalized = host.lowercase().removePrefix("www.")
        return normalized == "youtube.com" ||
            normalized.endsWith(".youtube.com") ||
            normalized == "youtu.be" ||
            normalized == "m.youtube.com"
    }
}
