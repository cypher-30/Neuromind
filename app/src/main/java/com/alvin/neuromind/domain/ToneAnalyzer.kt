package com.alvin.neuromind.domain

import com.alvin.neuromind.data.ToneLabel
import kotlin.math.roundToInt

data class ToneAnalysis(
    val label: ToneLabel,
    val sentimentScore: Float
)

object ToneAnalyzer {

    private val positiveWords = mapOf(
        "good" to 1.0f,
        "great" to 1.2f,
        "happy" to 1.1f,
        "calm" to 1.1f,
        "relaxed" to 1.1f,
        "productive" to 1.2f,
        "proud" to 1.1f,
        "better" to 0.8f,
        "fine" to 0.4f,
        "okay" to 0.3f,
        "ok" to 0.3f,
        "grateful" to 1.0f,
        "hopeful" to 1.0f,
        "focused" to 1.1f,
        "stable" to 0.8f,
        "grounded" to 1.0f,
        "rested" to 1.0f,
        "energized" to 1.2f,
        "motivated" to 1.1f,
        "clear" to 0.8f
    )

    private val negativeWords = mapOf(
        "stressed" to -1.1f,
        "stress" to -1.0f,
        "anxious" to -1.2f,
        "anxiety" to -1.2f,
        "overwhelmed" to -1.4f,
        "panic" to -1.5f,
        "tired" to -0.8f,
        "exhausted" to -1.3f,
        "drained" to -1.2f,
        "sad" to -1.0f,
        "angry" to -1.1f,
        "frustrated" to -1.1f,
        "burnout" to -1.4f,
        "burned" to -1.0f,
        "burnt" to -1.1f,
        "stuck" to -0.8f,
        "spiraling" to -1.3f,
        "doom" to -1.1f,
        "meh" to -0.4f,
        "numb" to -0.8f,
        "foggy" to -0.9f,
        "lonely" to -1.0f,
        "hopeless" to -1.5f
    )

    private val negations = setOf("not", "never", "no", "hardly", "barely", "without")
    private val intensifiers = setOf("very", "really", "extremely", "super", "so", "too")

    fun analyze(text: String): ToneAnalysis {
        val tokens = text
            .lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) {
            return ToneAnalysis(ToneLabel.NEUTRAL, 0f)
        }

        var weightedScore = 0f
        tokens.forEachIndexed { index, token ->
            val base = positiveWords[token] ?: negativeWords[token] ?: 0f
            if (base == 0f) return@forEachIndexed
            val previous = tokens.getOrNull(index - 1)
            val beforePrevious = tokens.getOrNull(index - 2)
            val negated = previous in negations || beforePrevious in negations
            val intensified = previous in intensifiers
            var adjusted = if (negated) -base else base
            if (intensified) adjusted *= 1.25f
            weightedScore += adjusted
        }

        val score = (weightedScore / tokens.size.toFloat()).coerceIn(-1f, 1f)

        val label = when {
            score <= -0.22f -> ToneLabel.OVERWHELMED
            score < -0.06f -> ToneLabel.STRESSED
            score < 0.09f -> ToneLabel.NEUTRAL
            score < 0.24f -> ToneLabel.POSITIVE
            else -> ToneLabel.CALM
        }

        return ToneAnalysis(label = label, sentimentScore = ((score * 100f).roundToInt() / 100f))
    }
}

