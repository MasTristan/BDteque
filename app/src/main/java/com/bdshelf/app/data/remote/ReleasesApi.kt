package com.bdshelf.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Lecture du fichier `releases.json` distant. Aucune dépendance réseau lourde (§2 SPEC). */
class ReleasesApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchReleases(url: String): ReleasesDocument = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        try {
            connection.connect()
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $code")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ReleasesDocument>(text)
        } finally {
            connection.disconnect()
        }
    }
}
