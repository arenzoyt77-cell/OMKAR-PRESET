package com.example.model

import java.util.UUID

data class PresetKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long,
    val x: Float = 0f, // Centered at 0, range -100 to +100 (or percentage pan)
    val y: Float = 0f, // Centered at 0, range -100 to +100 (or percentage pan)
    val zoomPercent: Float = 100f, // 100% is normal 1x scale
    val interpolationMode: InterpolationMode = InterpolationMode.LINEAR
) {
    init {
        require(timestampMs >= 0) { "Keyframe timestamp must be non-negative: $timestampMs" }
        require(zoomPercent > 0) { "Zoom percentage must be positive: $zoomPercent" }
    }
}
