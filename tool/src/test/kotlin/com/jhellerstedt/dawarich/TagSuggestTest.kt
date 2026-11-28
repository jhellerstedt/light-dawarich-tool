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
}
