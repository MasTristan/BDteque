package com.bdshelf.app.data.remote

import com.bdshelf.app.data.local.dao.IsbnLookupCacheDao
import com.bdshelf.app.data.local.entities.CachedIsbnLookup
import com.bdshelf.app.domain.IsbnBook
import com.bdshelf.app.domain.canonicalEan
import com.bdshelf.app.domain.isbn13To10
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Une source d'identification par ISBN, interchangeable pour les tests (§6.4). */
fun interface IsbnSource {
    suspend fun lookup(isbn: String): IsbnBook?
}

/**
 * Identification d'un livre à partir de son ISBN (§6.4).
 *
 * Fiabilité avant tout :
 * - cache local d'abord ([IsbnLookupCacheDao]) : un code déjà identifié ne
 *   repart jamais sur le réseau (re-scan instantané, y compris hors-ligne) ;
 * - sinon les deux sources sont interrogées en parallèle. La BnF (structurée,
 *   série + tome) reste prioritaire pour la BD franco-belge ; Open Library ne
 *   sert que de repli (titre + auteurs) ou de complément (auteurs manquants
 *   sur la notice BnF), sans allonger le pire cas ;
 * - chaque source est réinterrogée sous forme ISBN-10 si l'EAN-13 (978) ne
 *   donne rien : les notices d'avant 2007 ne sont parfois indexées que sous
 *   l'ancienne forme.
 *
 * Retourne `null` si aucune source ne répond (hors-ligne, ISBN inconnu) —
 * l'échec n'est pas mémorisé, pour qu'une panne passagère reste retentable.
 */
class IsbnLookupService(
    private val bnf: IsbnSource = BnfCatalogApi(),
    private val openLibrary: IsbnSource = OpenLibraryApi(),
    private val cache: IsbnLookupCacheDao? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun lookup(rawEan: String): IsbnBook? {
        val isbn = canonicalEan(rawEan) ?: rawEan.trim().ifEmpty { return null }

        runCatching { cache?.byIsbn(isbn) }.getOrNull()?.let { return it.toIsbnBook() }

        val (book, source) = fetch(isbn) ?: return null
        // Un échec d'écriture du cache (disque plein…) ne doit pas faire
        // perdre l'identification déjà obtenue.
        runCatching { cache?.insert(CachedIsbnLookup.from(isbn, book, source, clock())) }
        return book
    }

    private suspend fun fetch(isbn: String): Pair<IsbnBook, String>? = coroutineScope {
        val isbn10 = isbn13To10(isbn)
        val bnfDeferred = async {
            bnf.lookup(isbn) ?: isbn10?.let { bnf.lookup(it) }
        }
        val openLibraryDeferred = async {
            openLibrary.lookup(isbn) ?: isbn10?.let { openLibrary.lookup(it) }
        }

        val bnfBook = bnfDeferred.await()
        when {
            bnfBook == null -> openLibraryDeferred.await()?.let { it to SOURCE_OPEN_LIBRARY }
            // Notice BnF sans auteurs : Open Library peut compléter.
            bnfBook.authors.isEmpty() -> {
                val openLibraryBook = openLibraryDeferred.await()
                val merged = if (openLibraryBook != null && openLibraryBook.authors.isNotEmpty()) {
                    bnfBook.copy(authors = openLibraryBook.authors)
                } else {
                    bnfBook
                }
                merged to SOURCE_BNF
            }
            else -> {
                openLibraryDeferred.cancel()
                bnfBook to SOURCE_BNF
            }
        }
    }

    private companion object {
        const val SOURCE_BNF = "bnf"
        const val SOURCE_OPEN_LIBRARY = "openlibrary"
    }
}
