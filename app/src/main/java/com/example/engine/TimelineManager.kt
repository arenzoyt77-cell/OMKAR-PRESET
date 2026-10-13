package com.example.engine

import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetClip
import com.example.model.PresetKeyframe

data class TimelineState(
    val videoDurationMs: Long = 10_000L, // Default 10s if no video loaded
    val videoWidth: Int = 1920,
    val videoHeight: Int = 1080,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val splitPointsMs: List<Long> = emptyList(),
    val clips: List<PresetClip> = emptyList(),
    val keyframes: List<PresetKeyframe> = emptyList(),
    val selectedClipId: String? = null,
    val selectedKeyframeId: String? = null
)

class TimelineManager(initialState: TimelineState = TimelineState()) {
    var state: TimelineState = initialState
        private set

    init {
        rebuildClips()
    }

    fun setVideo(durationMs: Long, width: Int = 1920, height: Int = 1080) {
        state = state.copy(
            videoDurationMs = durationMs.coerceAtLeast(100L),
            videoWidth = width,
            videoHeight = height,
            currentPositionMs = state.currentPositionMs.coerceIn(0L, durationMs)
        )
        rebuildClips()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, state.videoDurationMs)
        state = state.copy(currentPositionMs = clamped)
    }

    fun setPlaying(playing: Boolean) {
        state = state.copy(isPlaying = playing)
    }

    fun addSplitPoint(timestampMs: Long): Boolean {
        val clamped = timestampMs.coerceIn(1L, state.videoDurationMs - 1L)
        if (state.splitPointsMs.contains(clamped)) {
            return false // Duplicate rejected
        }
        val updatedSplits = (state.splitPointsMs + clamped).sorted()
        state = state.copy(splitPointsMs = updatedSplits)
        rebuildClips()
        return true
    }

    fun moveSplitPoint(oldTimestampMs: Long, newTimestampMs: Long): Boolean {
        if (!state.splitPointsMs.contains(oldTimestampMs)) return false
        val clamped = newTimestampMs.coerceIn(1L, state.videoDurationMs - 1L)
        val withoutOld = state.splitPointsMs.filter { it != oldTimestampMs }
        val updated = (withoutOld + clamped).sorted().distinct()
        state = state.copy(splitPointsMs = updated)
        rebuildClips()
        return true
    }

    fun deleteSplitPoint(timestampMs: Long): Boolean {
        if (!state.splitPointsMs.contains(timestampMs)) return false
        val updated = state.splitPointsMs.filter { it != timestampMs }
        state = state.copy(splitPointsMs = updated)
        rebuildClips()
        return true
    }

    fun addOrUpdateKeyframe(
        timestampMs: Long,
        x: Float,
        y: Float,
        zoomPercent: Float,
        mode: InterpolationMode
    ): PresetKeyframe {
        val clampedTs = timestampMs.coerceAtLeast(0L)
        // If a keyframe already exists at this exact timestamp, update it
        val existingIndex = state.keyframes.indexOfFirst { it.timestampMs == clampedTs }
        val kf: PresetKeyframe
        val updatedList: List<PresetKeyframe>

        if (existingIndex >= 0) {
            val existing = state.keyframes[existingIndex]
            kf = existing.copy(
                x = x,
                y = y,
                zoomPercent = zoomPercent,
                interpolationMode = mode
            )
            val mutable = state.keyframes.toMutableList()
            mutable[existingIndex] = kf
            updatedList = mutable.sortedBy { it.timestampMs }
        } else {
            kf = PresetKeyframe(
                timestampMs = clampedTs,
                x = x,
                y = y,
                zoomPercent = zoomPercent,
                interpolationMode = mode
            )
            updatedList = (state.keyframes + kf).sortedBy { it.timestampMs }
        }

        state = state.copy(
            keyframes = updatedList,
            selectedKeyframeId = kf.id
        )
        return kf
    }

    fun updateKeyframe(
        id: String,
        x: Float? = null,
        y: Float? = null,
        zoomPercent: Float? = null,
        mode: InterpolationMode? = null,
        timestampMs: Long? = null
    ): Boolean {
        val index = state.keyframes.indexOfFirst { it.id == id }
        if (index < 0) return false

        val current = state.keyframes[index]
        val updated = current.copy(
            timestampMs = timestampMs ?: current.timestampMs,
            x = x ?: current.x,
            y = y ?: current.y,
            zoomPercent = zoomPercent ?: current.zoomPercent,
            interpolationMode = mode ?: current.interpolationMode
        )

        val mutable = state.keyframes.toMutableList()
        mutable[index] = updated
        state = state.copy(keyframes = mutable.sortedBy { it.timestampMs })
        return true
    }

    fun deleteKeyframe(id: String): Boolean {
        val filtered = state.keyframes.filter { it.id != id }
        if (filtered.size == state.keyframes.size) return false
        state = state.copy(
            keyframes = filtered,
            selectedKeyframeId = if (state.selectedKeyframeId == id) null else state.selectedKeyframeId
        )
        return true
    }

    fun selectKeyframe(id: String?) {
        state = state.copy(selectedKeyframeId = id)
    }

    fun selectClip(id: String?) {
        state = state.copy(selectedClipId = id)
    }

    /**
     * Applies preset at EXACT milliseconds.
     * Does NOT silently scale or shift timestamps.
     * If keepOutOfRange is true, keeps splits/keyframes even if > targetDuration.
     * If keepOutOfRange is false, clamps them.
     */
    fun applyPreset(preset: OmkarPreset, keepOutOfRange: Boolean = true) {
        val splits: List<Long>
        val kfs: List<PresetKeyframe>

        if (keepOutOfRange) {
            // Exact milliseconds preserved
            splits = preset.splitPointsMs.sorted().distinct()
            kfs = preset.keyframes.sortedBy { it.timestampMs }
        } else {
            // Clamped to video duration
            splits = preset.splitPointsMs
                .filter { it in 1 until state.videoDurationMs }
                .sorted()
                .distinct()

            kfs = preset.keyframes
                .map {
                    if (it.timestampMs > state.videoDurationMs) {
                        it.copy(timestampMs = state.videoDurationMs)
                    } else it
                }
                .sortedBy { it.timestampMs }
        }

        state = state.copy(
            splitPointsMs = splits,
            keyframes = kfs,
            selectedKeyframeId = kfs.firstOrNull()?.id
        )
        rebuildClips()
    }

    fun toPreset(name: String): OmkarPreset {
        return OmkarPreset(
            name = name.ifBlank { "Omkar Preset" },
            sourceDurationMs = state.videoDurationMs,
            sourceWidth = state.videoWidth,
            sourceHeight = state.videoHeight,
            splitPointsMs = state.splitPointsMs.sorted().distinct(),
            clips = state.clips,
            keyframes = state.keyframes.sortedBy { it.timestampMs }
        )
    }

    private fun rebuildClips() {
        val newClips = PresetValidator.buildClipsFromSplits(
            state.splitPointsMs,
            state.videoDurationMs
        )
        state = state.copy(clips = newClips)
    }
}
