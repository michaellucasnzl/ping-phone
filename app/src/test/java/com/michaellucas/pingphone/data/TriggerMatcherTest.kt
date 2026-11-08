package com.michaellucas.pingphone.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerMatcherTest {

    @Test
    fun `default 777 trigger matches ordinary message text`() {
        assertTrue(TriggerMatcher.matches("Hey the code is 777 right?", "777"))
    }

    @Test
    fun `default 777 trigger matches punctuation-delimited text`() {
        assertTrue(TriggerMatcher.matches("ping:777!", "777"))
        assertTrue(TriggerMatcher.matches("[777]", "777"))
    }

    @Test
    fun `1777 does not match trigger 777`() {
        // Word-boundary regex must not match inside a longer numeric token.
        assertFalse(TriggerMatcher.matches("call 1777 times", "777"))
    }

    @Test
    fun `7779 does not match trigger 777`() {
        assertFalse(TriggerMatcher.matches("my code is 7779 actually", "777"))
    }

    @Test
    fun `whitespace-only trigger code never matches`() {
        assertFalse(TriggerMatcher.matches("anything 777 here", "   "))
    }

    @Test
    fun `blank trigger code never matches`() {
        assertFalse(TriggerMatcher.matches("anything 777 here", ""))
    }

    @Test
    fun `blank body never matches a valid code`() {
        assertFalse(TriggerMatcher.matches("", "777"))
    }

    @Test
    fun `regex metacharacters in custom code are matched literally`() {
        // A trigger code containing regex metacharacters must NOT be interpreted as regex.
        assertTrue(TriggerMatcher.matches("the code is a.b*c", "a.b*c"))
        assertFalse(TriggerMatcher.matches("the code is aXXc", "a.b*c"))
    }

    @Test
    fun `valid code at 50-character boundary is accepted`() {
        val code = "a".repeat(50)
        assertTrue(TriggerMatcher.isValidCode(code))
        assertTrue(TriggerMatcher.matches("trigger $code now", code))
    }

    @Test
    fun `51-character code is rejected as invalid`() {
        val code = "a".repeat(51)
        assertFalse(TriggerMatcher.isValidCode(code))
        assertFalse(TriggerMatcher.matches("trigger $code now", code))
    }

    @Test
    fun `whitespace-only code is rejected as invalid`() {
        assertFalse(TriggerMatcher.isValidCode("   "))
        assertFalse(TriggerMatcher.isValidCode("\t\n"))
    }

    @Test
    fun `blank code is rejected as invalid`() {
        assertFalse(TriggerMatcher.isValidCode(""))
    }

    @Test
    fun `nonblank short code is accepted as valid`() {
        assertTrue(TriggerMatcher.isValidCode("777"))
        assertTrue(TriggerMatcher.isValidCode("find-my-phone"))
    }
}
