package com.bdshelf.app.data.backup

import android.content.Context
import com.bdshelf.app.data.repo.CollectionRepository
import com.bdshelf.app.data.repo.CollectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sauvegardes automatiques locales de la collection (§6.9).
 *
 * Filet de sécurité contre les fausses manipulations (import d'un mauvais
 * fichier, suppression involontaire) : la collection est photographiée en JSON
 * dans `filesDir/backups/`, une fois par jour ([BackupWorker]) et avant chaque
 * import. Les [MAX_BACKUPS] plus récentes sont conservées.
 *
 * Ce n'est PAS une protection contre la perte du téléphone : pour cela,
 * l'export partageable des Réglages reste le bon outil. Les fichiers sont au
 * même format que l'export JSON ([CollectionSnapshot]) et se restaurent donc
 * par l'import habituel.
 */
class BackupManager(
    context: Context,
    private val collectionRepository: CollectionRepository,
) {
    private val dir = File(context.filesDir, "backups")
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Écrit une sauvegarde maintenant. Sans effet si la collection est vide
     * (ne pas évincer de bonnes sauvegardes par des instantanés vides) ou si
     * rien n'a changé depuis la dernière sauvegarde (sept quotidiennes
     * identiques finiraient par faire tourner l'état d'avant l'erreur hors de
     * la rotation). Dégradation silencieuse : `null` en cas d'échec d'écriture.
     */
    suspend fun backupNow(): File? = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = collectionRepository.exportSnapshot()
            if (snapshot.series.isEmpty() && snapshot.albums.isEmpty()) return@runCatching null

            val content = json.encodeToString(snapshot)
            val newest = listBackups().firstOrNull()
            if (newest != null && runCatching { newest.readText() }.getOrNull() == content) {
                return@runCatching newest
            }

            dir.mkdirs()
            // SimpleDateFormat n'est pas thread-safe : instance locale (worker + réglages).
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "bdshelf_backup_$timestamp.json"
            // Écriture atomique : jamais de sauvegarde tronquée dans la rotation.
            val tmp = File(dir, "$name.tmp")
            tmp.writeText(content)
            val file = File(dir, name)
            if (!tmp.renameTo(file)) return@runCatching null

            listBackups().drop(MAX_BACKUPS).forEach { it.delete() }
            file
        }.getOrNull()
    }

    /** Date (epoch millis) de la sauvegarde la plus récente, ou `null`. */
    suspend fun lastBackupAt(): Long? = withContext(Dispatchers.IO) {
        listBackups().firstOrNull()?.lastModified()
    }

    /** Sauvegardes présentes, la plus récente d'abord (le nom horodaté trie comme la date). */
    private fun listBackups(): List<File> =
        dir.listFiles { file -> file.name.startsWith("bdshelf_backup_") && file.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            .orEmpty()

    private companion object {
        const val MAX_BACKUPS = 7
    }
}
