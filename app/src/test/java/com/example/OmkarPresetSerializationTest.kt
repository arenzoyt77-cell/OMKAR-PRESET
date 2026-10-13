package com.example

import com.example.engine.PresetParser
import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetClip
import com.example.model.PresetKeyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OmkarPresetSerializationTest {

    @Test
    fun testRoundTripSerializationPreservesExactMilliseconds() {
        val original = OmkarPreset(
            name = "Test Cinematic Preset",
            sourceDurationMs = 15_432L,
            sourceWidth = 3840,
            sourceHeight = 2160,
            splitPointsMs = listOf(1_234L, 5_678L, 10_999L),
            clips = listOf(
                PresetClip(id = "c1", startMs = 0L, endMs = 1_234L, name = "Intro"),
                PresetClip(id = "c2", startMs = 1_234L, endMs = 5_678L, name = "Middle"),
                PresetClip(id = "c3", startMs = 5_678L, endMs = 15_432L, name = "Outro")
            ),
            keyframes = listOf(
                PresetKeyframe(id = "k1", timestampMs = 0L, x = 0f, y = 0f, zoomPercent = 100f, interpolationMode = InterpolationMode.LINEAR),
                PresetKeyframe(id = "k2", timestampMs = 1_234L, x = 12.5f, y = -8.2f, zoomPercent = 145f, interpolationMode = InterpolationMode.EASE_IN_OUT),
                PresetKeyframe(id = "k3", timestampMs = 5_678L, x = -30.0f, y = 20.0f, zoomPercent = 180f, interpolationMode = InterpolationMode.HOLD)
            )
        )

        val json = PresetParser.toJson(original)
        assertTrue(json.contains("\"format\": \"OMKAR_PRESET\""))
        assertTrue(json.contains("\"sourceDurationMs\": 15432"))

        val restored = PresetParser.fromJson(json)

        assertEquals("OMKAR_PRESET", restored.format)
        assertEquals(original.name, restored.name)
        assertEquals(15_432L, restored.sourceDurationMs)
        assertEquals(3840, restored.sourceWidth)
        assertEquals(2160, restored.sourceHeight)

        // Exact split timestamps
        assertEquals(listOf(1_234L, 5_678L, 10_999L), restored.splitPointsMs)

        // Exact clips timestamps
        assertEquals(3, restored.clips.size)
        assertEquals(0L, restored.clips[0].startMs)
        assertEquals(1_234L, restored.clips[0].endMs)
        assertEquals(5_678L, restored.clips[1].endMs)

        // Exact keyframe timestamps & values
        assertEquals(3, restored.keyframes.size)
        assertEquals(0L, restored.keyframes[0].timestampMs)
        assertEquals(1_234L, restored.keyframes[1].timestampMs)
        assertEquals(12.5f, restored.keyframes[1].x, 0.001f)
        assertEquals(-8.2f, restored.keyframes[1].y, 0.001f)
        assertEquals(145f, restored.keyframes[1].zoomPercent, 0.001f)
        assertEquals(InterpolationMode.EASE_IN_OUT, restored.keyframes[1].interpolationMode)

        assertEquals(5_678L, restored.keyframes[2].timestampMs)
        assertEquals(InterpolationMode.HOLD, restored.keyframes[2].interpolationMode)
    }

    @Test
    fun testRejectsMalformedFilesAndMissingFormatIdentifier() {
        val nonOmkarJson = """{"format": "OTHER_APP", "version": 1}"""
        assertThrows(IllegalArgumentException::class.java) {
            PresetParser.fromJson(nonOmkarJson)
        }

        val corruptedJson = "{ broken json here ... "
        assertThrows(IllegalArgumentException::class.java) {
            PresetParser.fromJson(corruptedJson)
        }
    }
}
