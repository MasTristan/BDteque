package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BnfUnimarcParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/bnf/$name")) { "fixture $name introuvable" }
            .bufferedReader().use { it.readText() }

    @Test
    fun `parses Asterix record with series and tome`() {
        val book = parseBnfUnimarc(fixture("asterix_9782012101333.xml"))
        assertNotNull(book)
        book!!
        assertEquals("Astérix le Gaulois", book.title)
        // La collection BnF pour Astérix est "Une aventure d'Astérix" (contient "Astérix").
        assertTrue(book.seriesName!!.contains("Astérix"))
        assertEquals(1, book.tomeNumber)
        assertTrue(book.authors.isNotEmpty())
    }

    @Test
    fun `parses Thorgal record picking the first record with clean series and tome`() {
        val book = parseBnfUnimarc(fixture("thorgal_aniel.xml"))
        assertNotNull(book)
        book!!
        assertEquals("Aniel", book.title)
        assertEquals("Thorgal", book.seriesName)
        assertEquals(36, book.tomeNumber)
    }

    @Test
    fun `series match works against a known collection via guessSeries`() {
        val book = parseBnfUnimarc(fixture("thorgal_aniel.xml"))!!
        val candidates = listOf("thorgal" to "Thorgal", "xiii" to "XIII")
        assertEquals("thorgal", guessSeries(book.seriesName!!, candidates))
    }

    @Test
    fun `returns null on malformed xml`() {
        assertNull(parseBnfUnimarc("<not><valid"))
    }

    @Test
    fun `returns null on empty result set`() {
        val emptySru = """
            <?xml version="1.0" encoding="UTF-8"?>
            <srw:searchRetrieveResponse xmlns:srw="http://www.loc.gov/zing/srw/">
              <srw:numberOfRecords>0</srw:numberOfRecords>
              <srw:records></srw:records>
            </srw:searchRetrieveResponse>
        """.trimIndent()
        assertNull(parseBnfUnimarc(emptySru))
    }
}
