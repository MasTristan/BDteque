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

    /** Enrobe des notices UNIMARC dans une réponse SRU minimale. */
    private fun sru(vararg records: String): String =
        """<?xml version="1.0" encoding="UTF-8"?>
        <srw:searchRetrieveResponse xmlns:srw="http://www.loc.gov/zing/srw/">
          <srw:records>${records.joinToString("") { "<srw:recordData>$it</srw:recordData>" }}</srw:records>
        </srw:searchRetrieveResponse>"""

    private fun record(vararg datafields: String): String =
        """<mxc:record xmlns:mxc="info:lc/xmlns/marcxchange-v2" format="UNIMARC" type="Bibliographic">
          ${datafields.joinToString("")}
        </mxc:record>"""

    private fun datafield(tag: String, vararg subfields: Pair<String, String>): String =
        """<mxc:datafield tag="$tag" ind1=" " ind2=" ">
          ${subfields.joinToString("") { (code, value) -> """<mxc:subfield code="$code">$value</mxc:subfield>""" }}
        </mxc:datafield>"""

    @Test
    fun `falls back to zone 461 when 225 is absent`() {
        val xml = sru(
            record(
                datafield("200", "a" to "Aniel"),
                datafield("461", "t" to "Thorgal", "v" to "36"),
            ),
        )
        val book = parseBnfUnimarc(xml)!!
        assertEquals("Aniel", book.title)
        assertEquals("Thorgal", book.seriesName)
        assertEquals(36, book.tomeNumber)
    }

    @Test
    fun `picks the most complete record among several`() {
        val xml = sru(
            record(datafield("200", "a" to "Aniel")), // réédition sans collection
            record(
                datafield("200", "a" to "Aniel"),
                datafield("225", "a" to "Thorgal", "v" to "36"),
            ),
        )
        val book = parseBnfUnimarc(xml)!!
        assertEquals("Thorgal", book.seriesName)
        assertEquals(36, book.tomeNumber)
    }

    @Test
    fun `reads multi-volume records with part title and part number`() {
        // Notice "Série. N, Titre" : 200$a=série, 200$h=tome, 200$i=titre de partie.
        val xml = sru(
            record(datafield("200", "a" to "Thorgal", "h" to "36", "i" to "Aniel")),
        )
        val book = parseBnfUnimarc(xml)!!
        assertEquals("Aniel", book.title)
        assertEquals("Thorgal", book.seriesName)
        assertEquals(36, book.tomeNumber)
    }

    @Test
    fun `strips trailing ISBD punctuation from title and series`() {
        val xml = sru(
            record(
                datafield("200", "a" to "Aniel :"),
                datafield("225", "a" to "Thorgal /", "v" to "36"),
            ),
        )
        val book = parseBnfUnimarc(xml)!!
        assertEquals("Aniel", book.title)
        assertEquals("Thorgal", book.seriesName)
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
