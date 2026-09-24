package com.alvin.neuromind.domain

import com.alvin.neuromind.data.ToneLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneAnalyzerTest {

    @Test
    fun analyze_negative_text_returns_stressed_or_overwhelmed() {
        val result = ToneAnalyzer.analyze("I feel stressed and overwhelmed and exhausted")
        assertTrue(result.label == ToneLabel.STRESSED || result.label == ToneLabel.OVERWHELMED)
        assertTrue(result.sentimentScore < 0f)
    }

    @Test
    fun analyze_positive_text_returns_positive_or_calm() {
        val result = ToneAnalyzer.analyze("I feel calm, grateful, and productive today")
        assertTrue(result.label == ToneLabel.POSITIVE || result.label == ToneLabel.CALM)
        assertTrue(result.sentimentScore > 0f)
    }

    @Test
    fun analyze_neutral_text_returns_neutral() {
        val result = ToneAnalyzer.analyze("Went to class and finished two tasks")
        assertEquals(ToneLabel.NEUTRAL, result.label)
    }
}

