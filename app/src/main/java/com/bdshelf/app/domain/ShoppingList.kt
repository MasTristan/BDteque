package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series

/**
 * Un album à acheter (§6.10) : soit un trou connu de l'étagère (album en base,
 * non possédé), soit une sortie déjà parue d'une série suivie qui n'a pas
 * encore de fiche dans la collection.
 */
data class ShoppingItem(
    val seriesId: String,
    val seriesTitle: String,
    val tomeNumber: Int?,
    val title: String?,
    val albumId: String?, // null si l'item vient d'une sortie sans fiche en base
)

/** Les achats d'une série, pour l'affichage groupé (§6.10). */
data class ShoppingGroup(
    val seriesId: String,
    val seriesTitle: String,
    val items: List<ShoppingItem>,
)

/**
 * Construit la liste d'achats (§6.10) : calcul pur, à partir d'instantanés de
 * la collection (Moitié A) et des sorties (Moitié B) — sans jamais les
 * mélanger en base.
 *
 * - Chaque album non possédé de la collection est un achat potentiel.
 * - Chaque sortie DÉJÀ PARUE d'une série suivie, non possédée et sans fiche
 *   correspondante, s'y ajoute (les sorties à venir restent sur l'écran
 *   « À paraître » : on ne peut pas acheter ce qui n'existe pas encore).
 *
 * Groupée par série (alphabétique), tomes croissants, hors-série en fin.
 */
fun buildShoppingList(
    series: List<Series>,
    albums: List<Album>,
    releasedUnowned: List<ReleaseWithOwnership>,
): List<ShoppingGroup> {
    val seriesById = series.associateBy { it.id }
    val albumKeys = albums.mapNotNull { album -> album.tomeNumber?.let { album.seriesId to it } }.toSet()

    val gapItems = albums
        .filter { !it.owned }
        .mapNotNull { album ->
            val s = seriesById[album.seriesId] ?: return@mapNotNull null
            ShoppingItem(
                seriesId = album.seriesId,
                seriesTitle = s.title,
                tomeNumber = album.tomeNumber,
                title = album.title,
                albumId = album.id,
            )
        }

    val releaseItems = releasedUnowned
        .asSequence()
        .filter { it.release.status == "RELEASED" && !it.owned }
        .filter { (it.release.seriesId to it.release.tomeNumber) !in albumKeys }
        .map {
            ShoppingItem(
                seriesId = it.release.seriesId,
                seriesTitle = it.release.seriesTitle,
                tomeNumber = it.release.tomeNumber,
                title = it.release.title.ifBlank { null },
                albumId = null,
            )
        }
        .toList()

    return (gapItems + releaseItems)
        .groupBy { it.seriesId }
        .map { (seriesId, items) ->
            ShoppingGroup(
                seriesId = seriesId,
                seriesTitle = items.first().seriesTitle,
                items = items.sortedWith(compareBy(nullsLast()) { it.tomeNumber }),
            )
        }
        .sortedBy { it.seriesTitle.normalizedForSearch() }
}

/** Nombre total d'items, pour la carte d'accueil (§6.2). */
fun List<ShoppingGroup>.itemCount(): Int = sumOf { it.items.size }

/**
 * Texte partageable de la liste (§6.10) : à envoyer au libraire ou à la
 * famille avant un anniversaire. Format sobre, lisible dans n'importe quelle
 * messagerie.
 */
fun List<ShoppingGroup>.toShareText(header: String): String = buildString {
    append(header)
    for (group in this@toShareText) {
        append("\n\n")
        append(group.seriesTitle)
        for (item in group.items) {
            append("\n  - ")
            if (item.tomeNumber != null) {
                append("Tome ")
                append(item.tomeNumber)
                if (item.title != null) {
                    append(" : ")
                    append(item.title)
                }
            } else {
                append(item.title ?: "Hors-série")
            }
        }
    }
}
