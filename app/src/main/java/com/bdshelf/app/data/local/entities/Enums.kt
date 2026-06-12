package com.bdshelf.app.data.local.entities

/** Statut d'une série dans la collection. */
enum class SeriesStatus {
    ONGOING,
    FINISHED,
    UNKNOWN,
}

/** Statut de lecture / prêt d'un album. */
enum class ReadStatus {
    UNREAD,
    READ,
    LENT,
}
