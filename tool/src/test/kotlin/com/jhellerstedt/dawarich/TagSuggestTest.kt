package com.jhellerstedt.dawarich

import com.jhellerstedt.dawarich.api.models.DawarichTag
import com.jhellerstedt.dawarich.util.TagSuggest
import kotlin.test.Test
import kotlin.test.assertEquals

class TagSuggestTest {
    private val tags = listOf(
        DawarichTag(id = 1, name = "Cafe"),
        DawarichTag(id = 2, name = "Coffee"),
        DawarichTag(id = 3, name = "Park"),
        DawarichTag(id = 1, name = "Cafe"),
    )

    @Test
    fun dedupesAndSortsAllWhenQueryBlank() {
        assertEquals(
            listOf("Cafe", "Coffee", "Park"),
            TagSuggest.filterNames(tags, ""),
        )
    }

    @Test
    fun filtersCaseInsensitivePrefixThenContains() {
        assertEquals(listOf("Cafe", "Coffee"), TagSuggest.filterNames(tags, "c"))
        assertEquals(listOf("Park"), TagSuggest.filterNames(tags, "ar"))
    }

    @Test
    fun resolveByNameIgnoresCase() {
        assertEquals(1, TagSuggest.resolveId(tags, "cafe"))
        assertEquals(null, TagSuggest.resolveId(tags, "unknown"))
    }

    @Test
    fun rememberRecentPutsNewestFirstAndCapsAtTwo() {
        assertEquals(
            listOf("Park"),
            TagSuggest.rememberRecent("Park", emptyList()),
        )
        assertEquals(
            listOf("Cafe", "Park"),
            TagSuggest.rememberRecent("Cafe", listOf("Park")),
        )
        assertEquals(
            listOf("Coffee", "Cafe"),
            TagSuggest.rememberRecent("Coffee", listOf("Cafe", "Park")),
        )
        // Re-selecting moves to front without duplicating.
        assertEquals(
            listOf("Park", "Coffee"),
            TagSuggest.rememberRecent("Park", listOf("Coffee", "Park")),
        )
    }

    @Test
    fun filterSuggestionsBlankShowsOnlyRecentCappedAtTwo() {
        val manyTags = tags + listOf(
            DawarichTag(id = 4, name = "Library"),
            DawarichTag(id = 5, name = "Museum"),
            DawarichTag(id = 6, name = "Office"),
            DawarichTag(id = 7, name = "Shop"),
        )
        val recent = listOf("Park", "Cafe", "Shop")
        // Blank query: only the two most-recent known tags — not the full catalog.
        assertEquals(
            listOf("Park", "Cafe"),
            TagSuggest.filterSuggestions(manyTags, query = "", recent = recent, limit = 8),
        )
        assertEquals(
            emptyList(),
            TagSuggest.filterSuggestions(manyTags, query = "", recent = emptyList(), limit = 8),
        )
    }

    @Test
    fun filterSuggestionsTypedQueryIsAutocompleteOnly() {
        val recent = listOf("Park", "Cafe")
        assertEquals(
            listOf("Cafe", "Coffee"),
            TagSuggest.filterSuggestions(tags, query = "c", recent = recent, limit = 8),
        )
    }
}
