package com.bdshelf.app

/** Routes de navigation (§NAVIGATION ROUTES). Profondeur maximale : 2 niveaux. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val VERDICT = "verdict/{ean}"
    const val SERIES_LIST = "series_list"
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val ALBUM_FORM = "album_form/{seriesId}?albumId={albumId}&tomeNumber={tomeNumber}&ean={ean}"
    const val SERIES_FORM = "series_form?seriesId={seriesId}"
    const val RELEASES = "releases"
    const val SETTINGS = "settings"

    const val ALBUM_ID_ARG = "albumId"
    const val TOME_NUMBER_ARG = "tomeNumber"
    const val EAN_ARG = "ean"

    fun verdict(ean: String) = "verdict/$ean"
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"

    /**
     * [tomeNumber] préremplit le numéro de tome lors de l'ajout depuis un vide de l'étagère (§6.6).
     * [ean] préremplit le code-barres lors de la création d'un album depuis un verdict inconnu (§6.4).
     */
    fun albumForm(seriesId: String, albumId: String? = null, tomeNumber: Int? = null, ean: String? = null): String {
        val params = buildList {
            if (albumId != null) add("albumId=$albumId")
            if (tomeNumber != null) add("tomeNumber=$tomeNumber")
            if (ean != null) add("ean=$ean")
        }
        return "album_form/$seriesId" + if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
    }

    fun seriesForm(seriesId: String? = null) =
        "series_form" + if (seriesId != null) "?seriesId=$seriesId" else ""
}
