package com.example.model

import java.util.UUID

data class PresetClip(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val name: String = ""
) {
    init {
        require(startMs >= 0) { "Clip startMs must be non-negative: $startMs" }
        require(endMs >= startMs) { "Clip endMs ($endMs) cannot be before startMs ($startMs)" }
    }

    val durationMs: Long get() = endMs - startMs
}
