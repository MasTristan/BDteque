package com.bdshelf.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Enrichissement optionnel (§6.4 SPEC) : recherche le titre d'un album via son
 * EAN/ISBN sur Google Books, pour pré-remplir la fiche d'un nouvel album créé
 * depuis un verdict "Inconnu". Dégradation silencieuse si hors-ligne ou sans
 * résultat : retourne `null`, jamais d'exception.
 */
class GoogleBooksApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookupTitleByIsbn(isbn: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn").openConnection() as HttpURLConnection
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.requestMethod = "GET"
            try {
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val volumeInfo = json.decodeFromString<GoogleBooksResponse>(text).items.firstOrNull()?.volumeInfo
                val title = volumeInfo?.title?.trim().orEmpty()
                val subtitle = volumeInfo?.subtitle?.trim()
                when {
                    title.isEmpty() -> null
                    subtitle.isNullOrEmpty() -> title
                    else -> "$title - $subtitle"
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
