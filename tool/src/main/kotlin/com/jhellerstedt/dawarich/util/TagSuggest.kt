package com.jhellerstedt.dawarich.util

import com.jhellerstedt.dawarich.api.models.DawarichTag

object TagSuggest {
    fun uniqueSorted(tags: Iterable<DawarichTag>): List<DawarichTag> =
        tags
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

    fun filterNames(tags: Iterable<DawarichTag>, query: String): List<String> {
        val unique = uniqueSorted(tags)
        val q = query.trim()
        if (q.isEmpty()) return unique.map { it.name }
        val lower = q.lowercase()
        val prefix = unique.filter { it.name.lowercase().startsWith(lower) }
        val contains = unique.filter {
            val name = it.name.lowercase()
            !name.startsWith(lower) && name.contains(lower)
        }
        return (prefix + contains).map { it.name }
    }

    fun resolveId(tags: Iterable<DawarichTag>, name: String): Int? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return uniqueSorted(tags).firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id
    }

    /** Newest-first recent list, capped at [limit] (default 2 for filter chips). */
    fun rememberRecent(
        name: String,
        existing: List<String>,
        limit: Int = 2,
    ): List<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return existing.take(limit)
        val without = existing.filterNot { it.equals(trimmed, ignoreCase = true) }
        return (listOf(trimmed) + without).take(limit)
    }

    /**
     * Filter/upload suggestions.
     *
     * - Blank [query]: only the most recent known tags (capped at [recentLimit],
     *   default 2) — never the full tag catalog.
     * - Non-blank [query]: autocomplete matches via [filterNames], capped at [limit].
     */
    fun filterSuggestions(
        tags: Iterable<DawarichTag>,
        query: String,
        recent: List<String> = emptyList(),
        limit: Int = 8,
        recentLimit: Int = 2,
    ): List<String> {
        val unique = uniqueSorted(tags)
        val knownNames = unique.map { it.name }
        val knownLower = knownNames.map { it.lowercase() }.toSet()
        val recentKnown = recent
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() in knownLower }
            .distinctBy { it.lowercase() }
            .map { recentName ->
                knownNames.first { it.equals(recentName, ignoreCase = true) }
            }
            .take(recentLimit)

        val q = query.trim()
        if (q.isEmpty()) {
            return recentKnown
        }

        return filterNames(unique, q).take(limit)
    }
}
