package com.ttfeed.util

import java.util.UUID

/** Parses a UUID string, returning null on any malformed input instead of throwing. */
fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

/**
 * Converts a knob.ch name ("Lastname Firstname") to "Firstname Lastname".
 * Splits on the first space only, so compound first names ("Hans Peter") are preserved.
 */
fun normalizeKnobName(raw: String): String {
    val trimmed = raw.trim()
    val idx = trimmed.indexOf(' ')
    return if (idx == -1) trimmed else "${trimmed.substring(idx + 1)} ${trimmed.substring(0, idx)}"
}

/**
 * Converts a click-tt name ("Lastname, Firstname") to "Firstname Lastname".
 */
fun normalizeClickTtName(raw: String): String {
    val parts = raw.split(",", limit = 2)
    return if (parts.size == 2) "${parts[1].trim()} ${parts[0].trim()}" else raw.trim()
}
