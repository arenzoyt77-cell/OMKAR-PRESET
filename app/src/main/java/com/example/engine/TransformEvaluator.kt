package com.example.engine

import com.example.model.InterpolationMode
import com.example.model.PresetKeyframe
import com.example.model.TransformEvaluation

object TransformEvaluator {

    /**
     * Evaluates the precise 2D transform (X pan, Y pan, zoom percent) at a given timestamp in ms.
     * Guaranteed identical results for both real-time preview and export pipeline.
     */
    fun evaluate(keyframes: List<PresetKeyframe>, currentMs: Long): TransformEvaluation {
        if (keyframes.isEmpty()) {
            return TransformEvaluation(
                timestampMs = currentMs,
                x = 0f,
                y = 0f,
                zoomPercent = 100f
            )
        }

        val sorted = keyframes.sortedBy { it.timestampMs }

        // If at or before the first keyframe
        if (currentMs <= sorted.first().timestampMs) {
            val first = sorted.first()
            return TransformEvaluation(
                timestampMs = currentMs,
                x = first.x,
                y = first.y,
                zoomPercent = first.zoomPercent
            )
        }

        // If at or after the last keyframe
        if (currentMs >= sorted.last().timestampMs) {
            val last = sorted.last()
            return TransformEvaluation(
                timestampMs = currentMs,
                x = last.x,
                y = last.y,
                zoomPercent = last.zoomPercent
            )
        }

        // Find surrounding keyframes k1 and k2
        var k1 = sorted.first()
        var k2 = sorted.last()

        for (i in 0 until sorted.size - 1) {
            if (currentMs >= sorted[i].timestampMs && currentMs <= sorted[i + 1].timestampMs) {
                k1 = sorted[i]
                k2 = sorted[i + 1]
                break
            }
        }

        val deltaT = k2.timestampMs - k1.timestampMs
        if (deltaT <= 0L) {
            return TransformEvaluation(
                timestampMs = currentMs,
                x = k1.x,
                y = k1.y,
                zoomPercent = k1.zoomPercent
            )
        }

        val t = (currentMs - k1.timestampMs).toDouble() / deltaT.toDouble()
        val clampedT = t.coerceIn(0.0, 1.0)

        val factor: Double = when (k1.interpolationMode) {
            InterpolationMode.HOLD -> 0.0
            InterpolationMode.EASE_IN_OUT -> clampedT * clampedT * (3.0 - 2.0 * clampedT) // Smoothstep
            InterpolationMode.LINEAR -> clampedT
        }

        val evalX = (k1.x + (k2.x - k1.x) * factor).toFloat()
        val evalY = (k1.y + (k2.y - k1.y) * factor).toFloat()
        val evalZoom = (k1.zoomPercent + (k2.zoomPercent - k1.zoomPercent) * factor).toFloat()

        return TransformEvaluation(
            timestampMs = currentMs,
            x = evalX,
            y = evalY,
            zoomPercent = evalZoom
        )
    }
}
