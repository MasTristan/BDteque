package com.bdshelf.app.domain

import com.bdshelf.app.data.repo.CollectionSnapshot

/**
 * Pont entre [CollectionSnapshot] (Room/DTO) et [validateSnapshot] (§ADR-002) :
 * le domaine ne connaît que les identifiants nécessaires à la validation.
 */
fun CollectionSnapshot.validate(): SnapshotValidation = validateSnapshot(
    version = version,
    seriesIds = series.map { it.id },
    albums = albums.map { SnapshotAlbumRef(it.id, it.seriesId) },
)
