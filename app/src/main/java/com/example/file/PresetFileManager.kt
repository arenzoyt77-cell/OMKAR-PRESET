package com.example.file

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.engine.PresetParser
import com.example.model.OmkarPreset
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object PresetFileManager {

    /**
     * Writes OmkarPreset to an Android Content URI opened via ACTION_CREATE_DOCUMENT.
     */
    fun writePresetToUri(contentResolver: ContentResolver, uri: Uri, preset: OmkarPreset): Result<Unit> {
        return runCatching {
            val jsonString = PresetParser.toJson(preset)
            contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
                outputStream.flush()
            } ?: throw IllegalStateException("Could not open output stream for URI: $uri")
        }
    }

    /**
     * Reads and parses an OmkarPreset from an Android Content URI opened via ACTION_OPEN_DOCUMENT.
     */
    fun readPresetFromUri(contentResolver: ContentResolver, uri: Uri): Result<OmkarPreset> {
        return runCatching {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            } ?: throw IllegalStateException("Could not open input stream for URI: $uri")

            PresetParser.fromJson(jsonString)
        }
    }

    /**
     * Writes preset to a cached .omkarpreset file in the app cache directory.
     */
    fun writePresetToCache(context: Context, preset: OmkarPreset): File {
        val presetsDir = File(context.cacheDir, "presets").apply { mkdirs() }
        val safeName = preset.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_").ifBlank { "preset" }
        val presetFile = File(presetsDir, "$safeName.omkarpreset")

        val jsonString = PresetParser.toJson(preset)
        FileOutputStream(presetFile).use { fos ->
            fos.write(jsonString.toByteArray(StandardCharsets.UTF_8))
            fos.flush()
        }
        return presetFile
    }

    /**
     * Creates an Android native share Intent with FileProvider for the preset.
     */
    fun createShareIntent(context: Context, preset: OmkarPreset): Intent {
        val file = writePresetToCache(context, preset)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "OMKAR PRESET: ${preset.name}")
            putExtra(Intent.EXTRA_TEXT, "Import this video preset into OMKAR PRESET: ${preset.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
