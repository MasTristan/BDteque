package com.bdshelf.app.domain

import com.bdshelf.app.data.repo.CollectionSnapshot

/** Version de format de sauvegarde prise en charge par l'import (§6.9). */
const val SUPPORTED_SNAPSHOT_VERSION = 1

/** Résultat de la validation d'une sauvegarde avant écrasement de la collection. */
sealed interface SnapshotValidation {
    data object Valid : SnapshotValidation
    data class Invalid(val reason: String) : SnapshotValidation
}

/**
 * Valide un instantané de collection avant de remplacer la collection existante.
 *
 * L'import efface tout puis réinsère : un fichier incohérent doit être rejeté
 * AVANT toute suppression, sinon une sauvegarde corrompue détruit la collection.
 * On contrôle donc la version, l'unicité des identifiants et l'intégrité
 * référentielle (tout album pointe vers une série présente).
 */
fun CollectionSnapshot.validate(): SnapshotValidation {
    if (version != SUPPORTED_SNAPSHOT_VERSION) {
        return SnapshotValidation.Invalid("Version de sauvegarde non prise en charge ($version).")
    }
    val seriesIds = series.map { it.id }
    if (seriesIds.any { it.isBlank() }) {
        return SnapshotValidation.Invalid("Identifiant de série vide.")
    }
    if (seriesIds.size != seriesIds.toHashSet().size) {
        return SnapshotValidation.Invalid("Identifiants de série en double.")
    }
    val albumIds = albums.map { it.id }
    if (albumIds.any { it.isBlank() }) {
        return SnapshotValidation.Invalid("Identifiant d'album vide.")
    }
    if (albumIds.size != albumIds.toHashSet().size) {
        return SnapshotValidation.Invalid("Identifiants d'album en double.")
    }
    val knownSeries = seriesIds.toHashSet()
    val orphan = albums.firstOrNull { it.seriesId !in knownSeries }
    if (orphan != null) {
        return SnapshotValidation.Invalid("Album rattaché à une série absente (${orphan.seriesId}).")
    }
    return SnapshotValidation.Valid
}
