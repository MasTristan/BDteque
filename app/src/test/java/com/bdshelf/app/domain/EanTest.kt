package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EanTest {

    // ISBN réels tirés des fixtures BnF : Astérix le Gaulois et Thorgal Aniel.
    private val asterix13 = "9782012101333"
    private val thorgal13 = "9782803672172"

    @Test
    fun `accepts valid EAN-13`() {
        assertTrue(isValidEan13(asterix13))
        assertTrue(isValidEan13(thorgal13))
    }

    @Test
    fun `rejects EAN-13 with a misread digit`() {
        // Chaque altération d'un chiffre doit faire échouer la somme de contrôle.
        for (i in asterix13.indices) {
            val altered = asterix13.substring(0, i) +
                ((asterix13[i].digitToInt() + 1) % 10) +
                asterix13.substring(i + 1)
            assertFalse("position $i", isValidEan13(altered))
        }
    }

    @Test
    fun `rejects wrong lengths and non digits`() {
        assertFalse(isValidEan13(""))
        assertFalse(isValidEan13("978201210133"))
        assertFalse(isValidEan13("97820121013331"))
        assertFalse(isValidEan13("97820121O1333"))
    }

    @Test
    fun `accepts valid EAN-8 and rejects altered one`() {
        assertTrue(isValidEan8("96385074"))
        assertFalse(isValidEan8("96385075"))
        assertFalse(isValidEan8("9638507"))
    }

    @Test
    fun `validates ISBN-10 including X check digit`() {
        assertTrue(isValidIsbn10("0306406152"))
        assertFalse(isValidIsbn10("0306406153"))
        // 201210133X = forme ISBN-10 d'Astérix le Gaulois (clé X).
        assertTrue(isValidIsbn10("201210133X"))
    }

    @Test
    fun `converts ISBN-10 to EAN-13 and back`() {
        assertEquals("9780306406157", isbn10To13("0306406152"))
        assertEquals(asterix13, isbn10To13("201210133X"))
        assertEquals("201210133X", isbn13To10(asterix13))
        assertEquals("0306406152", isbn13To10("9780306406157"))
    }

    @Test
    fun `isbn13To10 returns null for 979 and non-ISBN EAN`() {
        // Préfixe 979 (pas d'équivalent ISBN-10) : EAN-13 valide construit pour le test.
        assertNull(isbn13To10("9791030702576"))
        assertNull(isbn13To10("invalid"))
    }

    @Test
    fun `canonicalEan normalizes hyphens and spaces`() {
        assertEquals(thorgal13, canonicalEan("978-2-8036-7217-2"))
        assertEquals(thorgal13, canonicalEan(" 978 2803 672172 "))
    }

    @Test
    fun `canonicalEan converts ISBN-10 input to EAN-13`() {
        assertEquals(asterix13, canonicalEan("2-01-210133-X"))
        assertEquals(asterix13, canonicalEan("201210133x"))
    }

    @Test
    fun `canonicalEan accepts valid EAN-8`() {
        assertEquals("96385074", canonicalEan("96385074"))
    }

    @Test
    fun `canonicalEan rejects invalid codes`() {
        assertNull(canonicalEan(""))
        assertNull(canonicalEan("9782012101334")) // somme de contrôle fausse
        assertNull(canonicalEan("hello"))
        assertNull(canonicalEan("1234"))
    }
}
