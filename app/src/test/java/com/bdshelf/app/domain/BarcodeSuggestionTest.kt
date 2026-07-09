package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeSuggestionTest {

    @Test
    fun `recognizes Tome N pattern`() {
        assertEquals(12, guessTomeNumber("Thorgal - Tome 12 : Le Pays Qâ"))
    }

    @Test
    fun `recognizes abbreviated T pattern`() {
        assertEquals(21, guessTomeNumber("Blake et Mortimer T21"))
    }

    @Test
    fun `recognizes T with dot pattern`() {
        assertEquals(7, guessTomeNumber("Astérix T. 7"))
    }

    @Test
    fun `recognizes hash pattern`() {
        assertEquals(3, guessTomeNumber("Largo Winch #3"))
    }

    @Test
    fun `returns null when no pattern matches`() {
        assertNull(guessTomeNumber("Le Pays Qâ"))
    }

    @Test
    fun `returns null on empty text`() {
        assertNull(guessTomeNumber(""))
    }

    @Test
    fun `matches series whose title is a prefix of the google title`() {
        val candidates = listOf("thorgal" to "Thorgal", "xiii" to "XIII")
        assertEquals("thorgal", guessSeries("Thorgal - Tome 12 : Le Pays Qâ", candidates))
    }

    @Test
    fun `matches accented series titles case-insensitively`() {
        val candidates = listOf("asterix" to "Astérix")
        assertEquals("asterix", guessSeries("ASTERIX, tome 1 : Astérix le Gaulois", candidates))
    }

    @Test
    fun `prefers the longer matching candidate to avoid short-name masking`() {
        val candidates = listOf("xiii" to "XIII", "xiii-mystery" to "XIII Mystery")
        assertEquals("xiii-mystery", guessSeries("XIII Mystery - Tome 1 : Le Wal Mora", candidates))
    }

    @Test
    fun `returns null when no known series matches`() {
        val candidates = listOf("thorgal" to "Thorgal")
        assertNull(guessSeries("Une série totalement inconnue", candidates))
    }

    @Test
    fun `returns null when candidate list is empty`() {
        assertNull(guessSeries("Thorgal - Tome 12", emptyList()))
    }
}
