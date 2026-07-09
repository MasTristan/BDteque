package com.bdshelf.app.data.remote

import java.net.HttpURLConnection
import java.net.URL

/**
 * GET simple retournant le corps texte, ou `null` en cas d'échec. Aucune
 * exception propagée : les sources ISBN dégradent silencieusement (§6.4).
 *
 * Les échecs transitoires (coupure réseau, timeout, HTTP 5xx/429) sont
 * retentés une fois après une courte pause : un scan en magasin se joue
 * souvent sur une connexion mobile hachée, et une seule requête perdue ne
 * doit pas faire passer un album pour inconnu. Les autres codes HTTP (404,
 * 400…) sont définitifs et rendent `null` immédiatement.
 */
internal fun httpGetText(url: String, timeoutMs: Int = 8_000, attempts: Int = 2): String? {
    for (attempt in 1..attempts) {
        when (val result = httpGetOnce(url, timeoutMs)) {
            is HttpResult.Success -> return result.body
            is HttpResult.Fatal -> return null
            is HttpResult.Transient -> if (attempt < attempts) Thread.sleep(300L * attempt)
        }
    }
    return null
}

private sealed interface HttpResult {
    data class Success(val body: String) : HttpResult
    data object Transient : HttpResult
    data object Fatal : HttpResult
}

private fun httpGetOnce(url: String, timeoutMs: Int): HttpResult {
    val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull()
        ?: return HttpResult.Fatal
    return try {
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "*/*")
        // Les API publiques (Open Library notamment) demandent un client identifiable.
        connection.setRequestProperty("User-Agent", "BDShelf/1.0 (https://github.com/MasTristan/BDteque)")
        connection.connect()
        when (connection.responseCode) {
            HttpURLConnection.HTTP_OK -> HttpResult.Success(connection.inputStream.bufferedReader().use { it.readText() })
            429, in 500..599 -> HttpResult.Transient
            else -> HttpResult.Fatal
        }
    } catch (e: Exception) {
        HttpResult.Transient
    } finally {
        connection.disconnect()
    }
}
