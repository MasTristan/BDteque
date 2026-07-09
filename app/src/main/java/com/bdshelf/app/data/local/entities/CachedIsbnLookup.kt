package com.bdshelf.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bdshelf.app.domain.IsbnBook

/**
 * Identification réussie d'un ISBN, mémorisée localement (§6.4).
 *
 * Évite de réinterroger la BnF / Open Library pour un code déjà vu : le
 * verdict d'un re-scan est instantané et fonctionne hors-ligne. Seules les
 * identifications réussies sont mémorisées — un échec peut venir d'une panne
 * réseau passagère et doit pouvoir être retenté au scan suivant. Les données
 * bibliographiques étant stables, aucune expiration n'est nécessaire.
 */
@Entity(tableName = "isbn_lookup_cache")
data class CachedIsbnLookup(
    @PrimaryKey val isbn: String, // forme canonique (EAN-13/EAN-8, chiffres nus)
    val title: String,
    val seriesName: String?,
    val tomeNumber: Int?,
    val authors: String, // noms joints par AUTHORS_SEPARATOR
    val source: String, // "bnf" ou "openlibrary", pour diagnostic
    val fetchedAt: Long, // epoch millis
) {
    fun toIsbnBook(): IsbnBook = IsbnBook(
        title = title,
        seriesName = seriesName,
        tomeNumber = tomeNumber,
        authors = if (authors.isEmpty()) emptyList() else authors.split(AUTHORS_SEPARATOR),
    )

    companion object {
        // Séparateur de contrôle US (U+001F), référencé par point de code pour
        // garder le source en ASCII : ne peut pas apparaître dans un nom d'auteur.
        private val AUTHORS_SEPARATOR = Char(0x1F).toString()

        fun from(isbn: String, book: IsbnBook, source: String, fetchedAt: Long): CachedIsbnLookup =
            CachedIsbnLookup(
                isbn = isbn,
                title = book.title,
                seriesName = book.seriesName,
                tomeNumber = book.tomeNumber,
                authors = book.authors.joinToString(AUTHORS_SEPARATOR),
                source = source,
                fetchedAt = fetchedAt,
            )
    }
}
