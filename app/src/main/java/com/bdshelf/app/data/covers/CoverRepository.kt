package com.bdshelf.app.data.covers

import android.content.Context
import com.bdshelf.app.data.prefs.UserPreferencesRepository
import com.bdshelf.app.data.remote.httpGetBytes
import com.bdshelf.app.domain.isValidEan13
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

// NOTE : désactiver le réglage arrête seulement les téléchargements futurs ;
// les couvertures déjà présentes restent (locales, quelques dizaines de Ko).

/**
 * Couvertures d'albums (§6.4) : téléchargées UNE FOIS, servies ensuite depuis
 * le stockage local, jamais depuis le réseau à l'affichage.
 *
 * Compromis avec le principe « 100 % hors ligne » du projet : le
 * téléchargement est opt-in (réglage « Couvertures », désactivé par défaut,
 * [UserPreferencesRepository.downloadCovers]) et ne se déclenche qu'au moment
 * d'une identification ISBN — jamais en parcourant la collection. Source :
 * l'API couvertures d'Open Library, adressable directement par ISBN, sans clé.
 *
 * Les fichiers vivent dans `filesDir/covers/{ean}.jpg`, nommés par l'EAN-13
 * canonique ([com.bdshelf.app.domain.canonicalEan]) — la même clé que le champ
 * `ean` des albums et le cache ISBN.
 */
class CoverRepository(
    context: Context,
    private val prefs: UserPreferencesRepository,
) {
    private val dir = File(context.filesDir, "covers")

    /** Couverture déjà téléchargée pour cet EAN, ou `null`. Lecture seule, aucun réseau. */
    fun coverFile(ean: String?): File? {
        if (ean == null) return null
        return File(dir, "$ean.jpg").takeIf { it.exists() }
    }

    /**
     * Couverture pour [ean] : le fichier local s'il existe, sinon tentative de
     * téléchargement si le réglage est activé. Dégradation silencieuse (`null`
     * si hors-ligne, couverture inconnue, ou réglage désactivé) : une
     * couverture est un bonus, jamais un blocage.
     */
    suspend fun ensureCover(ean: String?): File? {
        if (ean == null) return null
        coverFile(ean)?.let { return it }
        if (!prefs.downloadCovers.first()) return null
        // Seuls les EAN-13 « bookland » (978/979) sont des ISBN adressables.
        if (!isValidEan13(ean) || !(ean.startsWith("978") || ean.startsWith("979"))) return null

        return withContext(Dispatchers.IO) {
            // default=false : Open Library renvoie 404 au lieu d'une image
            // « pas de couverture », que l'on ne veut pas stocker.
            val bytes = httpGetBytes("https://covers.openlibrary.org/b/isbn/$ean-L.jpg?default=false")
                ?: return@withContext null
            // Les très petits corps sont des pixels de remplissage, pas des couvertures.
            if (bytes.size < MIN_COVER_BYTES) return@withContext null
            runCatching {
                dir.mkdirs()
                // Écriture atomique : jamais de .jpg tronqué si l'app est tuée en plein vol.
                val tmp = File(dir, "$ean.jpg.tmp")
                tmp.writeBytes(bytes)
                val final = File(dir, "$ean.jpg")
                if (tmp.renameTo(final)) final else null
            }.getOrNull()
        }
    }

    private companion object {
        const val MIN_COVER_BYTES = 1_000
    }
}
