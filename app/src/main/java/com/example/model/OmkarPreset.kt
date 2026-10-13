package com.example.model

import java.util.UUID

data class OmkarPreset(
    val format: String = FORMAT_IDENTIFIER,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val sourceDurationMs: Long,
    val sourceWidth: Int = 1920,
    val sourceHeight: Int = 1080,
    val splitPointsMs: List<Long> = emptyList(),
    val clips: List<PresetClip> = emptyList(),
    val keyframes: List<PresetKeyframe> = emptyList()
) {
    companion object {
        const val FORMAT_IDENTIFIER = "OMKAR_PRESET"
        const val CURRENT_SCHEMA_VERSION = 1
        const val FILE_EXTENSION = ".omkarpreset"
        const val MIME_TYPE = "application/json"
    }

    init {
        require(format == FORMAT_IDENTIFIER) { "Invalid format identifier: $format" }
        require(schemaVersion >= 1) { "Invalid schema version: $schemaVersion" }
        require(sourceDurationMs >= 0) { "Source duration cannot be negative: $sourceDurationMs" }
    }
}
