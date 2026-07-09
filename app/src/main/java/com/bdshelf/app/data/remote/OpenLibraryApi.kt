package com.bdshelf.app.data.remote

import com.bdshelf.app.domain.IsbnBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Identification d'un album par ISBN via Open Library (§6.4, source de repli).
 *
 * Couverture inégale pour la BD franco-belge, mais utile quand la BnF ne
 * renvoie rien (éditions étrangères notamment). Ne fournit ni série ni tome
 * de façon fiable : seulement titre et auteurs. Dégradation silencieuse.
 */
class OpenLibraryApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(isbn: String): IsbnBook? = withContext(Dispatchers.IO) {
        val url = "https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data"
        val body = httpGetText(url) ?: return@withContext null
        runCatching {
            // Réponse = objet indexé par "ISBN:xxx" ; on prend la seule notice.
            val book = json.decodeFromString<Map<String, OpenLibraryBook>>(body).values.firstOrNull()
            val title = book?.title?.trim()?.takeIf { it.isNotBlank() } ?: return@runCatching null
            IsbnBook(
                title = title,
                seriesName = null,
                tomeNumber = null,
                authors = book.authors.mapNotNull { it.name?.trim() }.filter { it.isNotBlank() },
            )
        }.getOrNull()
    }
}
