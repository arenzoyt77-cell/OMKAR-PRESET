package com.example.engine

import com.example.model.OmkarPreset
import com.example.model.PresetClip
import com.example.model.TransformEvaluation

data class ExportClipResult(
    val clip: PresetClip,
    val initialTransform: TransformEvaluation,
    val finalTransform: TransformEvaluation,
    val sampleTransforms: List<TransformEvaluation>
)

data class ExportReport(
    val presetName: String,
    val totalDurationMs: Long,
    val totalClips: Int,
    val clipResults: List<ExportClipResult>,
    val totalKeyframes: Int,
    val outputResolution: String,
    val audioSynchronized: Boolean = true,
    val exportTimestamp: Long = System.currentTimeMillis()
)

object ExportPipeline {

    /**
     * Executes the transform rendering evaluation pipeline across all clips.
     * Evaluates every clip and keyframe using the exact same TransformEvaluator as the preview.
     */
    fun processExport(
        preset: OmkarPreset,
        targetDurationMs: Long,
        width: Int = 1920,
        height: Int = 1080,
        sampleFps: Int = 10
    ): ExportReport {
        val clips = PresetValidator.buildClipsFromSplits(preset.splitPointsMs, targetDurationMs)
        val clipResults = mutableListOf<ExportClipResult>()

        val frameIntervalMs = (1000L / sampleFps).coerceAtLeast(10L)

        clips.forEach { clip ->
            val initTransform = TransformEvaluator.evaluate(preset.keyframes, clip.startMs)
            val finalTransform = TransformEvaluator.evaluate(preset.keyframes, clip.endMs)

            val samples = mutableListOf<TransformEvaluation>()
            var t = clip.startMs
            while (t <= clip.endMs) {
                samples.add(TransformEvaluator.evaluate(preset.keyframes, t))
                t += frameIntervalMs
            }

            clipResults.add(
                ExportClipResult(
                    clip = clip,
                    initialTransform = initTransform,
                    finalTransform = finalTransform,
                    sampleTransforms = samples
                )
            )
        }

        return ExportReport(
            presetName = preset.name,
            totalDurationMs = targetDurationMs,
            totalClips = clips.size,
            clipResults = clipResults,
            totalKeyframes = preset.keyframes.size,
            outputResolution = "${width}x${height}",
            audioSynchronized = true
        )
    }
}
