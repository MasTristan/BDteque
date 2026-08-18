package com.bdshelf.app.crash

import android.content.Context
import android.os.Build
import com.bdshelf.app.BuildConfig
import com.bdshelf.app.domain.CrashReportInput
import com.bdshelf.app.domain.crashReportFileName
import com.bdshelf.app.domain.crashReportTimestampOrNull
import com.bdshelf.app.domain.formatCrashReport
import com.bdshelf.app.domain.isCrashReportExpired
import java.io.File

/** Un rapport de plantage lu depuis le disque : nom de fichier (pour l'acquittement) et contenu. */
data class CrashReportFile(val fileName: String, val content: String)

/**
 * Capture de plantage locale, sans télémétrie (§E6 5.4).
 *
 * [install] pose un gestionnaire d'exceptions non interceptées qui écrit un
 * rapport texte dans `filesDir/crashes/` puis laisse le plantage suivre son
 * cours normal — on n'essaie jamais de maintenir le process en vie après une
 * exception non rattrapée, c'est le rôle du gestionnaire précédent (celui du
 * système). Rien n'est envoyé automatiquement ; l'envoi passe toujours par
 * une action explicite de l'utilisateur (§E6 5.4).
 */
class CrashReporter(private val context: Context) {

    private val crashesDir: File
        get() = File(context.filesDir, "crashes").apply { mkdirs() }

    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashReport(throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashReport(throwable: Throwable) {
        val timestamp = System.currentTimeMillis()
        val input = CrashReportInput(
            timestampMillis = timestamp,
            stackTrace = throwable.stackTraceToString(),
            appVersion = BuildConfig.VERSION_NAME,
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
        File(crashesDir, crashReportFileName(timestamp)).writeText(formatCrashReport(input))
    }

    /** Supprime les rapports de plus de 30 jours (§E6 5.4). À appeler au démarrage. */
    fun purgeExpired() {
        val now = System.currentTimeMillis()
        crashesDir.listFiles()?.forEach { file ->
            val timestamp = crashReportTimestampOrNull(file.name) ?: return@forEach
            if (isCrashReportExpired(timestamp, now)) file.delete()
        }
    }

    /** Le rapport le plus récent, s'il en existe un — pour la bannière au démarrage suivant. */
    fun latestReport(): CrashReportFile? {
        val latest = crashesDir.listFiles()
            ?.mapNotNull { file -> crashReportTimestampOrNull(file.name)?.let { it to file } }
            ?.maxByOrNull { it.first }
            ?: return null
        return CrashReportFile(fileName = latest.second.name, content = latest.second.readText())
    }
}
