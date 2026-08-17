package com.bdshelf.app.domain.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Résultat de l'analyse d'un corps de bundle (§E1 §4.4). Les lignes
 * ignorées ne font pas échouer l'import à elles seules — c'est
 * [com.bdshelf.app.domain.catalog.CatalogPlausibility] qui décide, sur leur
 * proportion, si le bundle dans son ensemble reste digne de confiance.
 */
data class BundleParseResult(
    val entities: List<CatalogBundleEntity>,
    /** Ligne non vide mais illisible : JSON invalide, ou `t` connu avec des champs requis absents/du mauvais type. */
    val malformedLines: Int,
    /** JSON valide, mais `t` inconnu — extension future du format, pas une erreur (§CONTRATS-DE-DONNEES §5.3). */
    val unknownTypeLines: Int,
)

/**
 * Lecture du corps JSON Lines d'un bundle catalogue (§E1 §4.4, format exact
 * dans `docs/specs/CONTRATS-DE-DONNEES.md` §5.3). Une entité par ligne,
 * traitée indépendamment des autres : jamais de structure imbriquée à
 * charger en mémoire d'un bloc pour 35 000 albums.
 */
object CatalogBundleParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(lines: Sequence<String>): BundleParseResult {
        val entities = mutableListOf<CatalogBundleEntity>()
        var malformed = 0
        var unknownType = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val element = runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull()
            if (element == null) {
                malformed++
                continue
            }

            when (element["t"]?.jsonPrimitive?.content) {
                "s" -> decode<SeriesLine>(element)?.let { entities += it.toEntity() } ?: malformed++
                "a" -> decode<AlbumLine>(element)?.let { entities += it.toEntity() } ?: malformed++
                "x" -> decode<AliasLine>(element)?.let { entities += it.toEntity() } ?: malformed++
                null -> malformed++
                else -> unknownType++
            }
        }

        return BundleParseResult(entities, malformed, unknownType)
    }

    private inline fun <reified T> decode(element: JsonObject): T? =
        runCatching { json.decodeFromJsonElement<T>(element) }.getOrNull()

    @Serializable
    private data class SeriesLine(
        val id: String,
        val title: String,
        val publisher: String? = null,
        val status: String,
        val firstYear: Int? = null,
        val tomeCount: Int? = null,
    ) {
        fun toEntity() = CatalogBundleEntity.SeriesEntity(id, title, publisher, status, firstYear, tomeCount)
    }

    @Serializable
    private data class AlbumLine(
        val ean: String,
        val seriesId: String,
        val tome: Int? = null,
        val title: String,
        val published: String? = null,
        val pages: Int? = null,
    ) {
        fun toEntity() = CatalogBundleEntity.AlbumEntity(ean, seriesId, tome, title, published, pages)
    }

    @Serializable
    private data class AliasLine(val alias: String, val seriesId: String) {
        fun toEntity() = CatalogBundleEntity.AliasEntity(alias, seriesId)
    }
}
