package com.axiomlabs.vidamacraft

import java.net.URI

/** Validates URL shape only; it does not determine source permissions or media type. */
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
                else -> null
            }
        } catch (_: Exception) {
            "Enter a valid HTTPS URL."
        }
    }
}
