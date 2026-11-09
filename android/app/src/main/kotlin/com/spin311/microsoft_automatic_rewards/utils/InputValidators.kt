package com.spin311.microsoft_automatic_rewards.utils

object InputValidators {
    const val MIN_SEARCH_COUNT = 1
    const val MAX_SEARCH_COUNT = 50
    const val MIN_DELAY_SECONDS = 0.5

    fun validateSearchCount(value: String?): String? {
        if (value.isNullOrBlank()) return "Required"
        val count = value.trim().toIntOrNull() ?: return "Enter a valid number"
        if (count < MIN_SEARCH_COUNT) return "Must be > 0"
        if (count > MAX_SEARCH_COUNT) return "Max $MAX_SEARCH_COUNT searches"
        return null
    }

    fun validateDelay(value: String?): String? {
        if (value.isNullOrBlank()) return "Required"
        val delay = value.trim().toDoubleOrNull() ?: return "Enter a valid number"
        if (delay < MIN_DELAY_SECONDS) return "Min $MIN_DELAY_SECONDS seconds"
        return null
    }
}
