package com.bdshelf.app.data.remote

import com.bdshelf.app.data.local.dao.IsbnLookupCacheDao
import com.bdshelf.app.data.local.entities.CachedIsbnLookup
import com.bdshelf.app.domain.IsbnBook
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsbnLookupServiceTest {

    private val isbn = "9782803672172"
    private val bnfBook = IsbnBook("Aniel", "Thorgal", 36, listOf("Grzegorz Rosinski"))
    private val openLibraryBook = IsbnBook("Aniel", null, null, listOf("Rosinski"))

    private class FakeCacheDao : IsbnLookupCacheDao {
        val entries = mutableMapOf<String, CachedIsbnLookup>()
        override suspend fun byIsbn(isbn: String): CachedIsbnLookup? = entries[isbn]
        override suspend fun insert(entry: CachedIsbnLookup) {
            entries[entry.isbn] = entry
        }
        override suspend fun deleteAll() = entries.clear()
    }

    /** Source comptant ses appels, répondant [book] pour les ISBN de [answers]. */
    private class CountingSource(private val answers: Map<String, IsbnBook>) : IsbnSource {
        val calls = mutableListOf<String>()
        override suspend fun lookup(isbn: String): IsbnBook? {
            calls += isbn
            return answers[isbn]
        }
    }

    @Test
    fun `prefers BnF result over Open Library`() = runBlocking {
        val service = IsbnLookupService(
            bnf = { bnfBook },
            openLibrary = { openLibraryBook },
        )
        assertEquals(bnfBook, service.lookup(isbn))
    }

    @Test
    fun `falls back to Open Library when BnF has nothing`() = runBlocking {
        val service = IsbnLookupService(
            bnf = { null },
            openLibrary = { openLibraryBook },
        )
        assertEquals(openLibraryBook, service.lookup(isbn))
    }

    @Test
    fun `returns null when no source answers`() = runBlocking {
        val service = IsbnLookupService(bnf = { null }, openLibrary = { null })
        assertNull(service.lookup(isbn))
    }

    @Test
    fun `completes missing BnF authors from Open Library`() = runBlocking {
        val service = IsbnLookupService(
            bnf = { bnfBook.copy(authors = emptyList()) },
            openLibrary = { openLibraryBook },
        )
        val book = service.lookup(isbn)!!
        assertEquals("Thorgal", book.seriesName) // structure BnF conservée
        assertEquals(listOf("Rosinski"), book.authors) // auteurs complétés
    }

    @Test
    fun `retries sources with the ISBN-10 form on a miss`() = runBlocking {
        // Notice d'avant 2007 : indexée uniquement sous l'ancienne forme ISBN-10.
        val asterix13 = "9782012101333"
        val asterix10 = "201210133X"
        val bnf = CountingSource(mapOf(asterix10 to bnfBook))
        val service = IsbnLookupService(bnf = bnf, openLibrary = { null })
        assertEquals(bnfBook, service.lookup(asterix13))
        assertEquals(listOf(asterix13, asterix10), bnf.calls)
    }

    @Test
    fun `caches a successful lookup and serves the second scan from cache`() = runBlocking {
        val cache = FakeCacheDao()
        val bnf = CountingSource(mapOf(isbn to bnfBook))
        val service = IsbnLookupService(bnf = bnf, openLibrary = { null }, cache = cache, clock = { 42L })

        assertEquals(bnfBook, service.lookup(isbn))
        assertEquals(1, bnf.calls.size)
        assertEquals(42L, cache.entries.getValue(isbn).fetchedAt)

        // Deuxième scan : servi par le cache, aucune nouvelle requête réseau.
        assertEquals(bnfBook, service.lookup(isbn))
        assertEquals(1, bnf.calls.size)
    }

    @Test
    fun `does not cache a failed lookup`() = runBlocking {
        val cache = FakeCacheDao()
        val service = IsbnLookupService(bnf = { null }, openLibrary = { null }, cache = cache)
        assertNull(service.lookup(isbn))
        assertEquals(0, cache.entries.size)
    }

    @Test
    fun `normalizes the scanned code before lookup and caching`() = runBlocking {
        val cache = FakeCacheDao()
        val bnf = CountingSource(mapOf(isbn to bnfBook))
        val service = IsbnLookupService(bnf = bnf, openLibrary = { null }, cache = cache)

        assertEquals(bnfBook, service.lookup("978-2-8036-7217-2"))
        assertEquals(listOf(isbn), bnf.calls)
        assertEquals(setOf(isbn), cache.entries.keys)
    }

    @Test
    fun `cached entry round-trips authors and structure`() {
        val entry = CachedIsbnLookup.from(isbn, bnfBook.copy(authors = listOf("Rosinski", "Yann")), "bnf", 0L)
        val book = entry.toIsbnBook()
        assertEquals(listOf("Rosinski", "Yann"), book.authors)
        assertEquals("Thorgal", book.seriesName)
        assertEquals(36, book.tomeNumber)
    }

    @Test
    fun `cached entry without authors round-trips to empty list`() {
        val entry = CachedIsbnLookup.from(isbn, bnfBook.copy(authors = emptyList()), "bnf", 0L)
        assertEquals(emptyList<String>(), entry.toIsbnBook().authors)
    }
}
