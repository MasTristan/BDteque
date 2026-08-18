package com.bdshelf.app.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.bdshelf.app.data.prefs.UserPreferencesRepository
import com.bdshelf.app.data.repo.CollectionRepository
import com.bdshelf.app.data.repo.CollectionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sauvegardes automatiques de la collection (§E4 "Le Coffre").
 *
 * Filet de sécurité contre les fausses manipulations (import d'un mauvais
 * fichier, suppression involontaire) : la collection est photographiée en
 * JSON, une fois par jour ([BackupWorker]) et avant chaque import. Deux
 * emplacements, indépendants l'un de l'autre :
 *
 * - `filesDir/backups/` : rapide, toujours actif, mais effacé avec
 *   l'application à la désinstallation. [MAX_INTERNAL_BACKUPS] copies gardées.
 * - le dossier choisi par l'utilisateur ([UserPreferencesRepository.backupFolderUri],
 *   accès SAF) : **survit à la désinstallation**. C'est lui qui corrige la
 *   dette la plus grave du projet — un filet de sécurité qui ne protégeait pas
 *   du cas de perte le plus fréquent. [MAX_EXTERNAL_BACKUPS] copies gardées.
 *   Absent tant que l'utilisateur n'a rien choisi ; son échec (dossier
 *   supprimé, carte SD retirée) n'empêche jamais la sauvegarde interne.
 *
 * Ce n'est PAS une protection contre la perte du téléphone lui-même : pour
 * cela, l'export partageable des Réglages reste le bon outil. Les fichiers
 * sont au même format que l'export JSON ([CollectionSnapshot]) et se
 * restaurent donc par l'import habituel.
 */
class BackupManager(
    private val context: Context,
    private val collectionRepository: CollectionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val dir = File(context.filesDir, "backups")
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Écrit une sauvegarde maintenant, dans les deux emplacements. Sans effet
     * si la collection est vide (ne pas évincer de bonnes sauvegardes par des
     * instantanés vides) ou si rien n'a changé depuis la dernière sauvegarde
     * (des copies quotidiennes identiques finiraient par faire sortir l'état
     * d'avant l'erreur de la rotation). Dégradation silencieuse : l'échec d'un
     * emplacement n'empêche jamais l'autre.
     */
    suspend fun backupNow(): File? = withContext(Dispatchers.IO) {
        val snapshot = runCatching { collectionRepository.exportSnapshot() }.getOrNull() ?: return@withContext null
        if (snapshot.series.isEmpty() && snapshot.albums.isEmpty()) return@withContext null
        val content = json.encodeToString(snapshot)

        val internal = backupInternal(content)
        backupToFolder(content)
        internal
    }

    private fun backupInternal(content: String): File? = runCatching {
        val newest = listInternalBackups().firstOrNull()
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

        listInternalBackups().drop(MAX_INTERNAL_BACKUPS).forEach { it.delete() }
        file
    }.getOrNull()

    private suspend fun backupToFolder(content: String) {
        val uriString = userPreferencesRepository.backupFolderUri.first() ?: return
        val root = accessibleFolder(uriString) ?: return

        val newest = listFolderBackups(root).firstOrNull()
        if (newest != null && readFolderFile(newest) == content) return

        val wrote = runCatching {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "bdshelf_backup_$timestamp.json"
            // Même discipline d'écriture atomique que la sauvegarde interne :
            // fichier temporaire, puis renommage. Un renommage refusé par le
            // fournisseur de documents laisse le .tmp de côté (ignoré par la
            // rotation, purgé au prochain succès) plutôt qu'une sauvegarde tronquée.
            val tmp = root.createFile("application/json", "$name.tmp") ?: error("Écriture du dossier refusée")
            writeFolderFile(tmp, content)
            val renamed = DocumentsContract.renameDocument(context.contentResolver, tmp.uri, name) != null
            if (!renamed) {
                runCatching { tmp.delete() }
                error("Renommage refusé par le fournisseur de documents")
            }
        }
        if (wrote.isSuccess) {
            listFolderBackups(root).drop(MAX_EXTERNAL_BACKUPS).forEach { it.delete() }
        }
    }

    /** Date (epoch millis) de la sauvegarde interne la plus récente, ou `null`. */
    suspend fun lastBackupAt(): Long? = withContext(Dispatchers.IO) {
        listInternalBackups().firstOrNull()?.lastModified()
    }

    /**
     * Le dossier choisi survit-il vraiment (existe encore, accessible en
     * écriture) ? Utilisé par les Réglages pour ne jamais afficher une
     * protection qui n'est plus réelle — dossier supprimé, carte SD retirée,
     * permission révoquée par le système.
     */
    suspend fun isBackupFolderAccessible(): Boolean = withContext(Dispatchers.IO) {
        val uriString = userPreferencesRepository.backupFolderUri.first() ?: return@withContext false
        accessibleFolder(uriString) != null
    }

    private fun accessibleFolder(uriString: String): DocumentFile? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val root = runCatching { DocumentFile.fromTreeUri(context, uri) }.getOrNull() ?: return null
        return root.takeIf { it.exists() && it.isDirectory && it.canWrite() }
    }

    private fun writeFolderFile(file: DocumentFile, content: String) {
        context.contentResolver.openOutputStream(file.uri)?.use { it.write(content.toByteArray()) }
            ?: error("Impossible d'écrire dans le dossier de sauvegarde")
    }

    private fun readFolderFile(file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    /** Sauvegardes présentes, la plus récente d'abord (le nom horodaté trie comme la date). */
    private fun listInternalBackups(): List<File> =
        dir.listFiles { file -> file.name.startsWith(BACKUP_PREFIX) && file.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            .orEmpty()

    private fun listFolderBackups(root: DocumentFile): List<DocumentFile> =
        root.listFiles()
            .filter { it.name?.startsWith(BACKUP_PREFIX) == true && it.name?.endsWith(".json") == true }
            .sortedByDescending { it.name }

    private companion object {
        const val BACKUP_PREFIX = "bdshelf_backup_"
        const val MAX_INTERNAL_BACKUPS = 7
        const val MAX_EXTERNAL_BACKUPS = 14
    }
}
