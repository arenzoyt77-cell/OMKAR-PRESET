package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.file.PresetFileManager
import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetKeyframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OMKAR PRESET", appName)
    }

    @Test
    fun testPresetFileCacheAndSharingUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preset = OmkarPreset(
            name = "Robolectric Preset",
            sourceDurationMs = 8_500L,
            splitPointsMs = listOf(2_000L, 5_500L),
            keyframes = listOf(
                PresetKeyframe(timestampMs = 0L, zoomPercent = 100f),
                PresetKeyframe(timestampMs = 3_000L, x = 10f, y = -5f, zoomPercent = 130f, interpolationMode = InterpolationMode.EASE_IN_OUT)
            )
        )

        // Write to cache file
        val file = PresetFileManager.writePresetToCache(context, preset)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.endsWith(".omkarpreset"))

        // Create share intent
        val shareIntent = PresetFileManager.createShareIntent(context, preset)
        assertNotNull(shareIntent)
        assertEquals("application/json", shareIntent.type)
        assertTrue(shareIntent.hasExtra(android.content.Intent.EXTRA_STREAM))
    }
}
