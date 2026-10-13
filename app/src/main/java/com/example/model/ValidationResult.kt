package com.example.model

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val outOfRangeSplits: List<Long> = emptyList(),
    val outOfRangeKeyframes: List<PresetKeyframe> = emptyList()
) {
    val hasDurationWarnings: Boolean
        get() = outOfRangeSplits.isNotEmpty() || outOfRangeKeyframes.isNotEmpty()
}
