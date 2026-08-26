package com.bdshelf.tools.bnfharvester

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Étape 0, purement diagnostique : confirmer que le point d'accès SRU du
 * catalogue BnF (§E1, ADR-004) répond bien à la forme de requête attendue par
 * [com.bdshelf.app.domain.parseBnfUnimarc] (notices `mxc:record` /
 * `mxc:datafield`), avant d'écrire quoi que ce soit qui ressemble à un
 * moissonnage réel. Ne construit aucun bundle, n'écrit aucun fichier — imprime
 * la réponse brute pour vérification humaine dans les logs CI.
 *
 * Exécuté uniquement en CI (accès réseau réel) via
 * .github/workflows/harvest-bnf.yml, déclenché manuellement.
 */
private val client: HttpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(15))
    .build()

private fun sruSearch(cql: String, recordSchema: String): String {
    val encodedQuery = URLEncoder.encode(cql, StandardCharsets.UTF_8)
    val url = "https://catalogue.bnf.fr/api/SRU" +
        "?version=1.2&operation=searchRetrieve" +
        "&query=$encodedQuery" +
        "&recordSchema=$recordSchema" +
        "&maximumRecords=3"
    val request = HttpRequest.newBuilder(URI.create(url))
        .header("Accept", "application/xml")
        .timeout(Duration.ofSeconds(20))
        .GET()
        .build()
    return try {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        buildString {
            appendLine("requête CQL : $cql")
            appendLine("schéma      : $recordSchema")
            appendLine("URL         : $url")
            appendLine("HTTP        : ${response.statusCode()}")
            appendLine("--- corps (4000 premiers caractères) ---")
            appendLine(response.body().take(4000))
        }
    } catch (e: Exception) {
        "requête CQL : $cql\nURL : $url\nÉCHEC : ${e::class.simpleName} ${e.message}"
    }
}

fun main() {
    // Deux formes de requête et deux valeurs de recordSchema plausibles : si
    // l'une des quatre combinaisons renvoie des notices exploitables, on sait
    // laquelle garder pour la suite. Choix de séries très connues pour ne
    // dépendre d'aucun ISBN deviné à l'avance — la BnF nous rendra les vrais.
    val queries = listOf(
        """bib.title all "Astérix" and bib.author all "Goscinny"""",
        """bib.title all "Thorgal"""",
    )
    val schemas = listOf("unimarcxchange", "unimarc")

    for (schema in schemas) {
        for (cql in queries) {
            println(sruSearch(cql, schema))
            println("=".repeat(80))
        }
    }
}
