package com.example.model

enum class InterpolationMode {
    LINEAR,
    EASE_IN_OUT,
    HOLD;

    companion object {
        fun fromString(value: String?): InterpolationMode {
            return when (value?.uppercase()) {
                "EASE_IN_OUT", "SMOOTH", "BEZIER" -> EASE_IN_OUT
                "HOLD", "STEP" -> HOLD
                else -> LINEAR
            }
        }
    }
}
