package com.example.engine

import com.example.model.InterpolationMode
import com.example.model.OmkarPreset
import com.example.model.PresetClip
import com.example.model.PresetKeyframe
import org.json.JSONArray
import org.json.JSONObject

object PresetParser {

    /**
     * Serializes an OmkarPreset into UTF-8 JSON string.
     */
    fun toJson(preset: OmkarPreset): String {
        val root = JSONObject()
        root.put("format", preset.format)
        root.put("schemaVersion", preset.schemaVersion)
        root.put("id", preset.id)
        root.put("name", preset.name)
        root.put("createdAtMs", preset.createdAtMs)
        root.put("sourceDurationMs", preset.sourceDurationMs)
        root.put("sourceWidth", preset.sourceWidth)
        root.put("sourceHeight", preset.sourceHeight)

        // Split points (strictly ordered, unique)
        val splitsArray = JSONArray()
        preset.splitPointsMs.sorted().distinct().forEach { splitsArray.put(it) }
        root.put("splitPointsMs", splitsArray)

        // Clips
        val clipsArray = JSONArray()
        preset.clips.forEach { clip ->
            val cObj = JSONObject()
            cObj.put("id", clip.id)
            cObj.put("startMs", clip.startMs)
            cObj.put("endMs", clip.endMs)
            cObj.put("name", clip.name)
            clipsArray.put(cObj)
        }
        root.put("clips", clipsArray)

        // Keyframes (sorted by timestampMs)
        val kfArray = JSONArray()
        preset.keyframes.sortedBy { it.timestampMs }.forEach { kf ->
            val kfObj = JSONObject()
            kfObj.put("id", kf.id)
            kfObj.put("timestampMs", kf.timestampMs)
            kfObj.put("x", kf.x.toDouble())
            kfObj.put("y", kf.y.toDouble())
            kfObj.put("zoomPercent", kf.zoomPercent.toDouble())
            kfObj.put("interpolationMode", kf.interpolationMode.name)
            kfArray.put(kfObj)
        }
        root.put("keyframes", kfArray)

        return root.toString(2)
    }

    /**
     * Parses a UTF-8 JSON string into an OmkarPreset.
     * Safely rejects malformed files or files without OMKAR_PRESET identifier.
     */
    @Throws(IllegalArgumentException::class)
    fun fromJson(jsonString: String): OmkarPreset {
        try {
            val root = JSONObject(jsonString)

            val format = root.optString("format", "")
            if (format != OmkarPreset.FORMAT_IDENTIFIER) {
                throw IllegalArgumentException("Invalid format identifier: '$format'. Expected '${OmkarPreset.FORMAT_IDENTIFIER}'.")
            }

            val schemaVersion = root.optInt("schemaVersion", 1)
            val id = root.optString("id", "")
            val name = root.optString("name", "Untitled Preset")
            val createdAtMs = root.optLong("createdAtMs", System.currentTimeMillis())
            val sourceDurationMs = root.optLong("sourceDurationMs", 0L)
            val sourceWidth = root.optInt("sourceWidth", 1920)
            val sourceHeight = root.optInt("sourceHeight", 1080)

            // Split points
            val splitPoints = mutableListOf<Long>()
            val splitsArray = root.optJSONArray("splitPointsMs")
            if (splitsArray != null) {
                for (i in 0 until splitsArray.length()) {
                    val split = splitsArray.optLong(i, -1L)
                    if (split >= 0) {
                        splitPoints.add(split)
                    }
                }
            }

            // Clips
            val clips = mutableListOf<PresetClip>()
            val clipsArray = root.optJSONArray("clips")
            if (clipsArray != null) {
                for (i in 0 until clipsArray.length()) {
                    val cObj = clipsArray.optJSONObject(i)
                    if (cObj != null) {
                        val cid = cObj.optString("id")
                        val startMs = cObj.optLong("startMs", 0L)
                        val endMs = cObj.optLong("endMs", startMs)
                        val cName = cObj.optString("name", "")
                        clips.add(PresetClip(id = cid, startMs = startMs, endMs = endMs, name = cName))
                    }
                }
            }

            // Keyframes
            val keyframes = mutableListOf<PresetKeyframe>()
            val kfArray = root.optJSONArray("keyframes")
            if (kfArray != null) {
                for (i in 0 until kfArray.length()) {
                    val kObj = kfArray.optJSONObject(i)
                    if (kObj != null) {
                        val kid = kObj.optString("id")
                        val ts = kObj.optLong("timestampMs", 0L)
                        val x = kObj.optDouble("x", 0.0).toFloat()
                        val y = kObj.optDouble("y", 0.0).toFloat()
                        val zoom = kObj.optDouble("zoomPercent", 100.0).toFloat()
                        val modeStr = kObj.optString("interpolationMode", "LINEAR")
                        keyframes.add(
                            PresetKeyframe(
                                id = kid,
                                timestampMs = ts,
                                x = x,
                                y = y,
                                zoomPercent = zoom,
                                interpolationMode = InterpolationMode.fromString(modeStr)
                            )
                        )
                    }
                }
            }

            return OmkarPreset(
                format = format,
                schemaVersion = schemaVersion,
                id = if (id.isNotBlank()) id else java.util.UUID.randomUUID().toString(),
                name = name,
                createdAtMs = createdAtMs,
                sourceDurationMs = sourceDurationMs,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                splitPointsMs = splitPoints.sorted().distinct(),
                clips = clips.sortedBy { it.startMs },
                keyframes = keyframes.sortedBy { it.timestampMs }
            )
        } catch (e: Exception) {
            if (e is IllegalArgumentException) throw e
            throw IllegalArgumentException("Malformed .omkarpreset content: ${e.message}", e)
        }
    }
}
