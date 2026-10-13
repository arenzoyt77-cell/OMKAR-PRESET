package com.example.engine

import com.example.model.OmkarPreset
import com.example.model.PresetClip
import com.example.model.PresetKeyframe
import com.example.model.ValidationResult

object PresetValidator {

    /**
     * Validates an OmkarPreset structurally.
     */
    fun validateStructure(preset: OmkarPreset): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (preset.format != OmkarPreset.FORMAT_IDENTIFIER) {
            errors.add("Identifier mismatch: expected ${OmkarPreset.FORMAT_IDENTIFIER}, found ${preset.format}")
        }
        if (preset.schemaVersion < 1) {
            errors.add("Invalid schema version: ${preset.schemaVersion}")
        }
        if (preset.sourceDurationMs < 0) {
            errors.add("Negative source duration: ${preset.sourceDurationMs}")
        }

        // Validate splits
        val seenSplits = mutableSetOf<Long>()
        val duplicates = mutableListOf<Long>()
        for (split in preset.splitPointsMs) {
            if (split < 0) {
                errors.add("Split timestamp cannot be negative: $split")
            }
            if (!seenSplits.add(split)) {
                duplicates.add(split)
            }
        }
        if (duplicates.isNotEmpty()) {
            warnings.add("Duplicate split points detected and merged: $duplicates")
        }

        // Validate keyframes
        for (kf in preset.keyframes) {
            if (kf.timestampMs < 0) {
                errors.add("Keyframe timestamp cannot be negative: ${kf.timestampMs}")
            }
            if (kf.zoomPercent <= 0) {
                errors.add("Keyframe zoomPercent must be positive: ${kf.zoomPercent}")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validates an OmkarPreset against a target video duration in milliseconds.
     * Checks if any split points or keyframes exceed target video duration.
     * Does NOT silently modify or discard anything! Returns clear warnings.
     */
    fun validateAgainstTargetVideo(preset: OmkarPreset, targetDurationMs: Long): ValidationResult {
        val baseValidation = validateStructure(preset)
        if (!baseValidation.isValid) return baseValidation

        val errors = baseValidation.errors.toMutableList()
        val warnings = baseValidation.warnings.toMutableList()

        val outOfRangeSplits = preset.splitPointsMs.filter { it > targetDurationMs }
        val outOfRangeKeyframes = preset.keyframes.filter { it.timestampMs > targetDurationMs }

        if (outOfRangeSplits.isNotEmpty()) {
            warnings.add("${outOfRangeSplits.size} split points exceed the target video duration (${targetDurationMs}ms). Max split is ${outOfRangeSplits.maxOrNull()}ms.")
        }

        if (outOfRangeKeyframes.isNotEmpty()) {
            warnings.add("${outOfRangeKeyframes.size} keyframes exceed the target video duration (${targetDurationMs}ms). Max keyframe is ${outOfRangeKeyframes.maxOfOrNull { it.timestampMs }}ms.")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            outOfRangeSplits = outOfRangeSplits,
            outOfRangeKeyframes = outOfRangeKeyframes
        )
    }

    /**
     * Rebuilds clips from ordered unique split points for a given total duration.
     * Boundary rule: split points partition [0, durationMs] into clips.
     */
    fun buildClipsFromSplits(splitPointsMs: List<Long>, totalDurationMs: Long): List<PresetClip> {
        val validSplits = splitPointsMs
            .filter { it in 1 until totalDurationMs }
            .sorted()
            .distinct()

        if (totalDurationMs <= 0) return emptyList()

        val clips = mutableListOf<PresetClip>()
        var currentStart = 0L

        validSplits.forEachIndexed { index, split ->
            clips.add(
                PresetClip(
                    startMs = currentStart,
                    endMs = split,
                    name = "Clip ${index + 1}"
                )
            )
            currentStart = split
        }

        // Final clip
        clips.add(
            PresetClip(
                startMs = currentStart,
                endMs = totalDurationMs,
                name = "Clip ${clips.size + 1}"
            )
        )

        return clips
    }
}
