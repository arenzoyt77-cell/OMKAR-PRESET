package com.example.model

data class TransformEvaluation(
    val timestampMs: Long,
    val x: Float, // Pan X offset in percentage (-100% to +100%)
    val y: Float, // Pan Y offset in percentage (-100% to +100%)
    val zoomPercent: Float, // 100% is neutral
    val effectiveScale: Float = zoomPercent / 100f
)
