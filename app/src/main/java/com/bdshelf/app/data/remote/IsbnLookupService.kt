package com.bdshelf.app.data.remote

import com.bdshelf.app.domain.IsbnBook

/**
 * Identification d'un livre à partir de son ISBN (§6.4).
 *
 * Chaîne de sources par ordre de fiabilité pour la BD franco-belge :
 * BnF (structurée, série + tome), puis Open Library en repli (titre + auteurs).
 * Retourne la première identification obtenue, ou `null` si aucune source ne
 * répond (hors-ligne, ISBN inconnu).
 */
class IsbnLookupService(
    private val bnf: BnfCatalogApi = BnfCatalogApi(),
    private val openLibrary: OpenLibraryApi = OpenLibraryApi(),
) {
    suspend fun lookup(isbn: String): IsbnBook? =
        bnf.lookup(isbn) ?: openLibrary.lookup(isbn)
}
