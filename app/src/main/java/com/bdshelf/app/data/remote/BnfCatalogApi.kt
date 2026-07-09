package com.bdshelf.app.data.remote

import com.bdshelf.app.domain.IsbnBook
import com.bdshelf.app.domain.parseBnfUnimarc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Identification d'un album par ISBN via le catalogue de la BnF (§6.4).
 *
 * Source primaire pour la BD franco-belge : dépôt légal français, donc
 * couverture large, sans clé ni quota strict. Interroge l'API SRU en UNIMARC,
 * qui expose la série et le numéro de tome de façon structurée
 * ([parseBnfUnimarc]). Dégradation silencieuse : `null` si rien trouvé.
 */
class BnfCatalogApi : IsbnSource {

    override suspend fun lookup(isbn: String): IsbnBook? = withContext(Dispatchers.IO) {
        // %20 plutôt que le "+" produit par URLEncoder : la requête CQL est un
        // composant d'URL, certains serveurs SRU ne décodent pas "+" en espace.
        val query = URLEncoder.encode("bib.isbn adj \"$isbn\"", "UTF-8").replace("+", "%20")
        // Plusieurs notices possibles pour un même ISBN (rééditions, tirages) :
        // on en demande quelques-unes et le parseur retient la plus complète.
        val url = "https://catalogue.bnf.fr/api/SRU?version=1.2&operation=searchRetrieve" +
            "&query=$query&recordSchema=unimarcxchange&maximumRecords=5"
        val xml = httpGetText(url) ?: return@withContext null
        parseBnfUnimarc(xml)
    }
}
