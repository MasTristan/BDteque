package com.bdshelf.app

/** Routes de navigation (§NAVIGATION ROUTES). Profondeur maximale : 2 niveaux. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val VERDICT = "verdict/{ean}"
    const val SERIES_LIST = "series_list"
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val ALBUM_FORM = "album_form/{seriesId}?albumId={albumId}"
    const val SERIES_FORM = "series_form?seriesId={seriesId}"
    const val RELEASES = "releases"
    const val SETTINGS = "settings"

    const val ALBUM_ID_ARG = "albumId"

    fun verdict(ean: String) = "verdict/$ean"
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"
    fun albumForm(seriesId: String, albumId: String? = null) =
        "album_form/$seriesId" + if (albumId != null) "?albumId=$albumId" else ""
    fun seriesForm(seriesId: String? = null) =
        "series_form" + if (seriesId != null) "?seriesId=$seriesId" else ""
}
