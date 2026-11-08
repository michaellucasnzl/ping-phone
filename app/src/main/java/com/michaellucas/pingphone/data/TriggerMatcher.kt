package com.michaellucas.pingphone.data

/**
 * Pure Kotlin trigger-matching and validation logic. Has no Android or coroutine
 * dependencies so it can be exercised by plain JVM unit tests.
 */
object TriggerMatcher {

    /** Maximum allowed length of a trigger code (matches SettingsRepository's legacy guard). */
    const val MAX_CODE_LENGTH = 50

    /**
     * Returns true if [triggerCode] is a usable code: non-blank and not longer than
     * [MAX_CODE_LENGTH]. Whitespace-only codes are rejected.
     */
    fun isValidCode(triggerCode: String): Boolean {
        // Reject blank and whitespace-only codes. Use a length cap to bound storage.
        return triggerCode.isNotBlank() && triggerCode.length <= MAX_CODE_LENGTH
    }

    /**
     * Returns true if [body] contains the [triggerCode] as a word-bounded substring.
     * Regex metacharacters in the trigger code are matched literally (via [Regex.escape]).
     * A blank or invalid trigger code never matches. A blank body never matches a valid code.
     *
     * "Word-bounded" uses `\b`, so e.g. "777" does not match inside "1777" or "7779".
     */
    fun matches(body: String, triggerCode: String): Boolean {
        if (!isValidCode(triggerCode)) return false
        if (body.isEmpty()) return false
        val pattern = Regex("\\b${Regex.escape(triggerCode)}\\b")
        return pattern.containsMatchIn(body)
    }
}
