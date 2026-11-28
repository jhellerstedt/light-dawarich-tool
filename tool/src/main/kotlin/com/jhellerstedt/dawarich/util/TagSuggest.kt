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
}
