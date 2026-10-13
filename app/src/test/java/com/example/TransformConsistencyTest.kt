package com.example

import com.example.engine.ExportPipeline
import com.example.engine.TransformEvaluator
import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetKeyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformConsistencyTest {

    @Test
    fun testPreviewAndExportUseIdenticalTransformCalculations() {
        val keyframes = listOf(
            PresetKeyframe(timestampMs = 0L, x = 0f, y = 0f, zoomPercent = 100f, interpolationMode = InterpolationMode.LINEAR),
            PresetKeyframe(timestampMs = 4_000L, x = 20f, y = -10f, zoomPercent = 150f, interpolationMode = InterpolationMode.EASE_IN_OUT),
            PresetKeyframe(timestampMs = 8_000L, x = -10f, y = 15f, zoomPercent = 200f, interpolationMode = InterpolationMode.HOLD),
            PresetKeyframe(timestampMs = 12_000L, x = 0f, y = 0f, zoomPercent = 100f, interpolationMode = InterpolationMode.LINEAR)
        )

        val preset = OmkarPreset(
            name = "Consistency Test",
            sourceDurationMs = 12_000L,
            splitPointsMs = listOf(4_000L, 8_000L),
            keyframes = keyframes
        )

        val exportReport = ExportPipeline.processExport(
            preset = preset,
            targetDurationMs = 12_000L,
            sampleFps = 10
        )

        // For every sampled clip and timestamp, verify ExportPipeline matches TransformEvaluator exactly
        exportReport.clipResults.forEach { clipResult ->
            val previewStart = TransformEvaluator.evaluate(keyframes, clipResult.clip.startMs)
            val previewEnd = TransformEvaluator.evaluate(keyframes, clipResult.clip.endMs)

            assertEquals(previewStart.x, clipResult.initialTransform.x, 0.0001f)
            assertEquals(previewStart.y, clipResult.initialTransform.y, 0.0001f)
            assertEquals(previewStart.zoomPercent, clipResult.initialTransform.zoomPercent, 0.0001f)

            assertEquals(previewEnd.x, clipResult.finalTransform.x, 0.0001f)
            assertEquals(previewEnd.y, clipResult.finalTransform.y, 0.0001f)
            assertEquals(previewEnd.zoomPercent, clipResult.finalTransform.zoomPercent, 0.0001f)

            clipResult.sampleTransforms.forEach { sample ->
                val direct = TransformEvaluator.evaluate(keyframes, sample.timestampMs)
                assertEquals(direct.x, sample.x, 0.0001f)
                assertEquals(direct.y, sample.y, 0.0001f)
                assertEquals(direct.zoomPercent, sample.zoomPercent, 0.0001f)
            }
        }
    }

    @Test
    fun testInterpolationModesEvaluation() {
        val k1 = PresetKeyframe(timestampMs = 0L, x = 0f, y = 0f, zoomPercent = 100f, interpolationMode = InterpolationMode.HOLD)
        val k2 = PresetKeyframe(timestampMs = 10_000L, x = 100f, y = 100f, zoomPercent = 200f)

        // Test HOLD: intermediate value stays at k1 values
        val holdMid = TransformEvaluator.evaluate(listOf(k1, k2), 5_000L)
        assertEquals(0f, holdMid.x, 0.001f)
        assertEquals(0f, holdMid.y, 0.001f)
        assertEquals(100f, holdMid.zoomPercent, 0.001f)

        // Test LINEAR: intermediate value at t=0.5 is exactly 50%
        val linearK1 = k1.copy(interpolationMode = InterpolationMode.LINEAR)
        val linearMid = TransformEvaluator.evaluate(listOf(linearK1, k2), 5_000L)
        assertEquals(50f, linearMid.x, 0.001f)
        assertEquals(50f, linearMid.y, 0.001f)
        assertEquals(150f, linearMid.zoomPercent, 0.001f)

        // Test EASE_IN_OUT: smoothstep at t=0.5 is also 0.5 (by symmetry), but at t=0.25 is smoother
        val easeK1 = k1.copy(interpolationMode = InterpolationMode.EASE_IN_OUT)
        val easeT25 = TransformEvaluator.evaluate(listOf(easeK1, k2), 2_500L) // t = 0.25
        // smoothstep(0.25) = 0.25 * 0.25 * (3 - 2 * 0.25) = 0.0625 * 2.5 = 0.15625 < 0.25 (slower start)
        assertTrue(easeT25.x < 25f)
        assertEquals(15.625f, easeT25.x, 0.01f)
    }
}
