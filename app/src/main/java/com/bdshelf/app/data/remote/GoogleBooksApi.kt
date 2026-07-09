package com.bdshelf.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** Titre et sous-titre bruts d'une fiche Google Books, avant toute mise en forme. */
data class GoogleBooksVolume(val title: String, val subtitle: String?)

/**
 * Enrichissement optionnel (§6.4 SPEC) : recherche les métadonnées d'un album
 * via son EAN/ISBN sur Google Books, pour pré-remplir la fiche d'un nouvel
 * album créé depuis un verdict "Inconnu", et pour suggérer une série/tome
 * (§6.4, suggestion de scan). Dégradation silencieuse si hors-ligne ou sans
 * résultat : retourne `null`, jamais d'exception.
 */
class GoogleBooksApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookupVolume(isbn: String): GoogleBooksVolume? = withContext(Dispatchers.IO) {
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
                if (title.isEmpty()) null else GoogleBooksVolume(title = title, subtitle = volumeInfo?.subtitle?.trim())
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    /** Titre affichable "Titre - Sous-titre", pour pré-remplir le champ titre d'un nouvel album. */
    suspend fun lookupTitleByIsbn(isbn: String): String? {
        val volume = lookupVolume(isbn) ?: return null
        return if (volume.subtitle.isNullOrEmpty()) volume.title else "${volume.title} - ${volume.subtitle}"
    }
}
