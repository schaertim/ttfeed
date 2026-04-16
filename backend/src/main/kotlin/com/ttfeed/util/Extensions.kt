package com.ttfeed.util

import java.util.UUID

/** Parses a UUID string, returning null on any malformed input instead of throwing. */
fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
