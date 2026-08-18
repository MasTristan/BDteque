package com.bdshelf.app.domain.catalog

/**
 * Une ligne décodée du corps JSON Lines d'un bundle catalogue (§E1 §4.4).
 * Volontairement distinct de toute entité Room : le domaine ne connaît rien
 * de la persistance (§ADR-002), la couche données fera la conversion vers
 * `catalog_series` / `catalog_album` / `catalog_alias` le moment venu.
 */
sealed interface CatalogBundleEntity {
    data class SeriesEntity(
        val id: String,
        val title: String,
        val publisher: String?,
        val status: String,
        val firstYear: Int?,
        val tomeCount: Int?,
    ) : CatalogBundleEntity

    data class AlbumEntity(
        val ean: String,
        val seriesId: String,
        val tomeNumber: Int?,
        val title: String,
        val published: String?,
        val pages: Int?,
    ) : CatalogBundleEntity

    /** `seriesId` n'est volontairement pas une clé étrangère (§ADR-003) : un bundle partiel reste valide. */
    data class AliasEntity(
        val alias: String,
        val seriesId: String,
    ) : CatalogBundleEntity
}
