package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizationTest {

    @Test
    fun `accents are removed and lowercased`() {
        assertEquals("asterix", "Astérix".normalizedForSearch())
        assertEquals("corto maltese", "Cortö Maltèse".normalizedForSearch())
    }

    @Test
    fun `already plain text is unchanged apart from case`() {
        assertEquals("thorgal", "Thorgal".normalizedForSearch())
    }

    @Test
    fun `slug is stable and hyphenated`() {
        assertEquals("buck-danny-classic", "Buck Danny Classic".toSlug())
        assertEquals("blake-et-mortimer", "Blake et Mortimer".toSlug())
    }

    @Test
    fun `slug strips accents and trims separators`() {
        assertEquals("asterix", "  Astérix  ".toSlug())
        assertEquals("les-tuniques-bleues", "Les Tuniques Bleues !".toSlug())
    }
}
