package com.alvin.neuromind.domain

import com.alvin.neuromind.data.Task

/** How a spoken/typed task name lines up with the task list. */
sealed interface TaskMatch {
    /** One clear match: safe to act on. */
    data class Exact(val task: Task) : TaskMatch
    /** Close but not certain ("crypto asignment"): ask before acting. */
    data class Likely(val task: Task) : TaskMatch
    /** Several equally good matches: ask which. */
    data class Several(val tasks: List<Task>) : TaskMatch
    data object None : TaskMatch
}

/**
 * Finds the task someone means from a few words, offline. Every word of what
 * they said is scored against the title's words: exact (1), prefix (0.9,
 * "assign" ~ "assignment"), or a typo within edit distance 1-2 (0.75).
 */
object TaskMatcher {

    private val stopWords = setOf("the", "a", "an", "my", "to", "for", "of", "on", "task", "it", "that", "this", "as", "and")

    fun match(target: String, tasks: List<Task>): TaskMatch {
        val open = tasks.filter { !it.isCompleted }
        val t = target.trim().lowercase()
        if (t.isBlank() || open.isEmpty()) return TaskMatch.None

        // Whole phrase inside the title: the old rule, still the strongest signal.
        val contains = open.filter { it.title.lowercase().contains(t) }
        if (contains.size == 1) return TaskMatch.Exact(contains.single())
        contains.firstOrNull { it.title.equals(t, ignoreCase = true) }?.let { return TaskMatch.Exact(it) }
        if (contains.size > 1) return TaskMatch.Several(contains)

        val words = words(t)
        if (words.isEmpty()) return TaskMatch.None
        val scored = open.map { it to score(words, words(it.title)) }
            .filter { it.second >= 0.6 }
            .sortedByDescending { it.second }
        if (scored.isEmpty()) return TaskMatch.None
        val best = scored.first()
        val tied = scored.filter { best.second - it.second < 0.1 }
        return when {
            tied.size > 1 -> TaskMatch.Several(tied.map { it.first })
            best.second >= 0.95 -> TaskMatch.Exact(best.first)
            else -> TaskMatch.Likely(best.first)
        }
    }

    private fun words(s: String): List<String> =
        s.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() && it !in stopWords }

    /** Average over the spoken words of their best match in the title. */
    private fun score(spoken: List<String>, title: List<String>): Double {
        if (title.isEmpty()) return 0.0
        return spoken.sumOf { w -> title.maxOf { wordScore(w, it) } } / spoken.size
    }

    private fun wordScore(a: String, b: String): Double = when {
        a == b -> 1.0
        a.length >= 3 && b.startsWith(a) || b.length >= 3 && a.startsWith(b) -> 0.9
        a.length >= 4 && distance(a, b) <= (if (a.length >= 7) 2 else 1) -> 0.75
        else -> 0.0
    }

    /** Levenshtein distance. */
    private fun distance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
