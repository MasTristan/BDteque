package com.bdshelf.app.data.remote

import java.net.HttpURLConnection
import java.net.URL

/**
 * GET simple retournant le corps texte, ou `null` en cas d'échec (réseau,
 * code HTTP non 200, timeout). Aucune exception propagée : les sources ISBN
 * dégradent silencieusement (§6.4).
 */
internal fun httpGetText(url: String, timeoutMs: Int = 8_000): String? {
    val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull() ?: return null
    return try {
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "*/*")
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
        connection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}
