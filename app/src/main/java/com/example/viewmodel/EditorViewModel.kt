package com.example.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ExportPipeline
import com.example.engine.ExportReport
import com.example.file.PresetFileManager
import com.example.engine.PresetValidator
import com.example.engine.TimelineManager
import com.example.engine.TimelineState
import com.example.engine.TransformEvaluator
import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetClip
import com.example.model.PresetKeyframe
import com.example.model.TransformEvaluation
import com.example.model.ValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WarningDialogData(
    val title: String,
    val message: String,
    val outOfRangeSplits: List<Long>,
    val outOfRangeKeyframes: List<PresetKeyframe>,
    val targetDurationMs: Long
)

data class EditorUiState(
    val videoUri: Uri? = null,
    val videoDurationMs: Long = 12_000L,
    val videoWidth: Int = 1920,
    val videoHeight: Int = 1080,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val splitPointsMs: List<Long> = emptyList(),
    val clips: List<PresetClip> = emptyList(),
    val keyframes: List<PresetKeyframe> = emptyList(),
    val selectedKeyframeId: String? = null,
    val currentTransform: TransformEvaluation = TransformEvaluation(0L, 0f, 0f, 100f),
    val presetName: String = "My Cinematic Preset",
    val warningDialogData: WarningDialogData? = null,
    val exportReport: ExportReport? = null,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val statusMessage: String? = null
) {
    val selectedKeyframe: PresetKeyframe?
        get() = keyframes.find { it.id == selectedKeyframeId }
}

class EditorViewModel : ViewModel() {

    private val timelineManager = TimelineManager(
        TimelineState(
            videoDurationMs = 12_000L,
            splitPointsMs = listOf(3_500L, 7_200L),
            keyframes = listOf(
                PresetKeyframe(timestampMs = 0L, x = 0f, y = 0f, zoomPercent = 100f, interpolationMode = InterpolationMode.LINEAR),
                PresetKeyframe(timestampMs = 3_500L, x = 15f, y = -10f, zoomPercent = 140f, interpolationMode = InterpolationMode.EASE_IN_OUT),
                PresetKeyframe(timestampMs = 7_200L, x = -20f, y = 15f, zoomPercent = 160f, interpolationMode = InterpolationMode.LINEAR),
                PresetKeyframe(timestampMs = 12_000L, x = 0f, y = 0f, zoomPercent = 100f, interpolationMode = InterpolationMode.LINEAR)
            )
        )
    )

    private val _uiState = MutableStateFlow(createUiStateFromManager())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var pendingImportPreset: OmkarPreset? = null

    init {
        updateTransform()
    }

    private fun createUiStateFromManager(): EditorUiState {
        val tmState = timelineManager.state
        val transform = TransformEvaluator.evaluate(tmState.keyframes, tmState.currentPositionMs)
        return EditorUiState(
            videoDurationMs = tmState.videoDurationMs,
            videoWidth = tmState.videoWidth,
            videoHeight = tmState.videoHeight,
            currentPositionMs = tmState.currentPositionMs,
            isPlaying = tmState.isPlaying,
            splitPointsMs = tmState.splitPointsMs,
            clips = tmState.clips,
            keyframes = tmState.keyframes,
            selectedKeyframeId = tmState.selectedKeyframeId,
            currentTransform = transform
        )
    }

    private fun syncState(status: String? = null) {
        val tmState = timelineManager.state
        val transform = TransformEvaluator.evaluate(tmState.keyframes, tmState.currentPositionMs)
        _uiState.update { current ->
            current.copy(
                videoDurationMs = tmState.videoDurationMs,
                videoWidth = tmState.videoWidth,
                videoHeight = tmState.videoHeight,
                currentPositionMs = tmState.currentPositionMs,
                isPlaying = tmState.isPlaying,
                splitPointsMs = tmState.splitPointsMs,
                clips = tmState.clips,
                keyframes = tmState.keyframes,
                selectedKeyframeId = tmState.selectedKeyframeId,
                currentTransform = transform,
                statusMessage = status ?: current.statusMessage
            )
        }
    }

    private fun updateTransform() {
        val tmState = timelineManager.state
        val transform = TransformEvaluator.evaluate(tmState.keyframes, tmState.currentPositionMs)
        _uiState.update { it.copy(currentTransform = transform) }
    }

    fun setVideoSource(uri: Uri?, durationMs: Long, width: Int = 1920, height: Int = 1080) {
        val dur = durationMs.coerceAtLeast(1000L)
        timelineManager.setVideo(dur, width, height)
        _uiState.update { it.copy(videoUri = uri) }
        syncState(status = "Video loaded (${dur}ms, ${width}x${height})")
    }

    fun seekTo(positionMs: Long) {
        timelineManager.seekTo(positionMs)
        syncState()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        timelineManager.setPlaying(true)
        _uiState.update { it.copy(isPlaying = true) }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val stepMs = 33L // ~30 fps
            while (isActive && timelineManager.state.isPlaying) {
                delay(stepMs)
                var next = timelineManager.state.currentPositionMs + stepMs
                if (next > timelineManager.state.videoDurationMs) {
                    next = 0L // Loop
                }
                timelineManager.seekTo(next)
                syncState()
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        timelineManager.setPlaying(false)
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun addSplitAtPlayhead() {
        val pos = _uiState.value.currentPositionMs
        val added = timelineManager.addSplitPoint(pos)
        if (added) {
            syncState(status = "Split added at ${pos}ms")
        } else {
            _uiState.update { it.copy(statusMessage = "Cannot add split: duplicate or boundary at ${pos}ms") }
        }
    }

    fun deleteSplit(splitMs: Long) {
        val deleted = timelineManager.deleteSplitPoint(splitMs)
        if (deleted) {
            syncState(status = "Split at ${splitMs}ms deleted")
        }
    }

    fun moveSplit(oldMs: Long, newMs: Long) {
        val moved = timelineManager.moveSplitPoint(oldMs, newMs)
        if (moved) {
            syncState(status = "Split moved from ${oldMs}ms to ${newMs}ms")
        }
    }

    fun addKeyframeAtPlayhead(
        x: Float = _uiState.value.currentTransform.x,
        y: Float = _uiState.value.currentTransform.y,
        zoom: Float = _uiState.value.currentTransform.zoomPercent,
        mode: InterpolationMode = InterpolationMode.LINEAR
    ) {
        val pos = _uiState.value.currentPositionMs
        val kf = timelineManager.addOrUpdateKeyframe(pos, x, y, zoom, mode)
        syncState(status = "Keyframe set at ${pos}ms (Zoom: ${zoom.toInt()}%)")
    }

    fun updateSelectedKeyframe(
        x: Float? = null,
        y: Float? = null,
        zoom: Float? = null,
        mode: InterpolationMode? = null
    ) {
        val kfId = _uiState.value.selectedKeyframeId ?: return
        timelineManager.updateKeyframe(id = kfId, x = x, y = y, zoomPercent = zoom, mode = mode)
        syncState()
    }

    fun deleteKeyframe(id: String) {
        val deleted = timelineManager.deleteKeyframe(id)
        if (deleted) {
            syncState(status = "Keyframe removed")
        }
    }

    fun selectKeyframe(id: String?) {
        timelineManager.selectKeyframe(id)
        syncState()
    }

    fun setPresetName(name: String) {
        _uiState.update { it.copy(presetName = name) }
    }

    fun savePresetToUri(contentResolver: ContentResolver, uri: Uri) {
        val preset = timelineManager.toPreset(_uiState.value.presetName)
        val result = PresetFileManager.writePresetToUri(contentResolver, uri, preset)
        result.onSuccess {
            _uiState.update { it.copy(statusMessage = "Preset '${preset.name}' saved successfully") }
        }.onFailure { err ->
            _uiState.update { it.copy(statusMessage = "Save error: ${err.message}") }
        }
    }

    fun importPresetFromUri(contentResolver: ContentResolver, uri: Uri) {
        val result = PresetFileManager.readPresetFromUri(contentResolver, uri)
        result.onSuccess { preset ->
            validateAndStagePreset(preset)
        }.onFailure { err ->
            _uiState.update { it.copy(statusMessage = "Failed to import preset: ${err.message}") }
        }
    }

    private fun validateAndStagePreset(preset: OmkarPreset) {
        val currentDuration = _uiState.value.videoDurationMs
        val validation = PresetValidator.validateAgainstTargetVideo(preset, currentDuration)

        if (!validation.isValid) {
            _uiState.update { it.copy(statusMessage = "Invalid preset: ${validation.errors.joinToString(", ")}") }
            return
        }

        if (validation.hasDurationWarnings) {
            // Out of range splits or keyframes exist: show warning dialog and let user decide!
            pendingImportPreset = preset
            _uiState.update {
                it.copy(
                    warningDialogData = WarningDialogData(
                        title = "Preset Exceeds Video Duration",
                        message = "The preset '${preset.name}' contains ${validation.outOfRangeSplits.size} splits and ${validation.outOfRangeKeyframes.size} keyframes that exceed your current video duration (${currentDuration}ms). How would you like to proceed?",
                        outOfRangeSplits = validation.outOfRangeSplits,
                        outOfRangeKeyframes = validation.outOfRangeKeyframes,
                        targetDurationMs = currentDuration
                    )
                )
            }
        } else {
            // Apply directly
            applyPresetDirectly(preset, keepOutOfRange = true)
        }
    }

    fun confirmImport(keepOutOfRange: Boolean) {
        val preset = pendingImportPreset ?: return
        pendingImportPreset = null
        _uiState.update { it.copy(warningDialogData = null) }
        applyPresetDirectly(preset, keepOutOfRange)
    }

    fun dismissWarning() {
        pendingImportPreset = null
        _uiState.update { it.copy(warningDialogData = null) }
    }

    private fun applyPresetDirectly(preset: OmkarPreset, keepOutOfRange: Boolean) {
        timelineManager.applyPreset(preset, keepOutOfRange)
        _uiState.update {
            it.copy(
                presetName = preset.name,
                statusMessage = "Applied preset '${preset.name}' with ${preset.splitPointsMs.size} splits and ${preset.keyframes.size} keyframes"
            )
        }
        syncState()
    }

    fun createShareIntent(context: Context): Intent {
        val preset = timelineManager.toPreset(_uiState.value.presetName)
        return PresetFileManager.createShareIntent(context, preset)
    }

    fun startExport() {
        val preset = timelineManager.toPreset(_uiState.value.presetName)
        val duration = _uiState.value.videoDurationMs
        val w = _uiState.value.videoWidth
        val h = _uiState.value.videoHeight

        _uiState.update { it.copy(isExporting = true, exportProgress = 0.1f) }

        viewModelScope.launch {
            // Simulate frame processing with progress
            for (p in 2..9) {
                delay(80L)
                _uiState.update { it.copy(exportProgress = p / 10f) }
            }

            val report = ExportPipeline.processExport(
                preset = preset,
                targetDurationMs = duration,
                width = w,
                height = h
            )

            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportProgress = 1.0f,
                    exportReport = report,
                    statusMessage = "Export complete: ${report.totalClips} clips, ${report.totalKeyframes} keyframes"
                )
            }
        }
    }

    fun dismissExportReport() {
        _uiState.update { it.copy(exportReport = null) }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
