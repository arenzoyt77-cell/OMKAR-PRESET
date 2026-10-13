package com.example

import com.example.engine.PresetValidator
import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetKeyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetValidationTest {

    @Test
    fun testDuplicateSplitHandling() {
        val preset = OmkarPreset(
            name = "Duplicate Split Preset",
            sourceDurationMs = 10_000L,
            splitPointsMs = listOf(2_000L, 5_000L, 2_000L, 8_000L, 5_000L)
        )

        val validation = PresetValidator.validateStructure(preset)
        assertTrue(validation.isValid)
        // Detects duplicates in warnings
        assertTrue(validation.warnings.any { it.contains("Duplicate split points detected") })

        // When building clips, duplicates are merged and strictly sorted
        val clips = PresetValidator.buildClipsFromSplits(preset.splitPointsMs, 10_000L)
        assertEquals(4, clips.size)
        assertEquals(0L, clips[0].startMs)
        assertEquals(2_000L, clips[0].endMs)
        assertEquals(2_000L, clips[1].startMs)
        assertEquals(5_000L, clips[1].endMs)
        assertEquals(5_000L, clips[2].startMs)
        assertEquals(8_000L, clips[2].endMs)
        assertEquals(8_000L, clips[3].startMs)
        assertEquals(10_000L, clips[3].endMs)
    }

    @Test
    fun testOutOfRangeWarningsForDifferentDurationVideo() {
        val preset = OmkarPreset(
            name = "Long Preset",
            sourceDurationMs = 20_000L,
            splitPointsMs = listOf(5_000L, 12_000L, 18_000L),
            keyframes = listOf(
                PresetKeyframe(timestampMs = 0L, zoomPercent = 100f),
                PresetKeyframe(timestampMs = 4_000L, zoomPercent = 120f),
                PresetKeyframe(timestampMs = 15_000L, zoomPercent = 150f)
            )
        )

        // Target video is only 10,000ms (10 seconds)
        val targetVideoDurationMs = 10_000L
        val validation = PresetValidator.validateAgainstTargetVideo(preset, targetVideoDurationMs)

        assertTrue(validation.isValid)
        assertTrue(validation.hasDurationWarnings)

        // Verify out-of-range splits identified (12,000ms and 18,000ms)
        assertEquals(listOf(12_000L, 18_000L), validation.outOfRangeSplits)

        // Verify out-of-range keyframes identified (15,000ms)
        assertEquals(1, validation.outOfRangeKeyframes.size)
        assertEquals(15_000L, validation.outOfRangeKeyframes[0].timestampMs)
    }

    @Test
    fun testExactMillisecondPreservation() {
        val splits = listOf(1_337L, 4_242L, 9_999L)
        val keyframes = listOf(
            PresetKeyframe(timestampMs = 777L, x = 10f, y = 20f, zoomPercent = 110f),
            PresetKeyframe(timestampMs = 5_555L, x = -15f, y = -5f, zoomPercent = 175f)
        )
        val preset = OmkarPreset(
            name = "Exact Preset",
            sourceDurationMs = 12_000L,
            splitPointsMs = splits,
            keyframes = keyframes
        )

        assertEquals(1_337L, preset.splitPointsMs[0])
        assertEquals(4_242L, preset.splitPointsMs[1])
        assertEquals(9_999L, preset.splitPointsMs[2])
        assertEquals(777L, preset.keyframes[0].timestampMs)
        assertEquals(5_555L, preset.keyframes[1].timestampMs)
    }
}
