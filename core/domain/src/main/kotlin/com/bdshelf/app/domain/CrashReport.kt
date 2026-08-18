package com.bdshelf.app.domain

/**
 * Contexte d'un plantage, sans rien qui identifie l'utilisateur ou sa
 * collection (§E6 5.4 « Incidents sans télémétrie ») : pile d'appels,
 * version de l'app, version d'Android, modèle d'appareil. Jamais
 * d'identifiant, de contenu de collection ni de chemin de fichier.
 */
data class CrashReportInput(
    val timestampMillis: Long,
    val stackTrace: String,
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
)

/** Nom de fichier stable, horodaté, pour retrouver et purger les rapports (§E6 5.4). */
fun crashReportFileName(timestampMillis: Long): String = "crash_$timestampMillis.txt"

/** Extrait l'horodatage d'un nom de fichier produit par [crashReportFileName], sinon `null`. */
fun crashReportTimestampOrNull(fileName: String): Long? {
    if (!fileName.startsWith("crash_") || !fileName.endsWith(".txt")) return null
    return fileName.removePrefix("crash_").removeSuffix(".txt").toLongOrNull()
}

/** Contenu texte intégral du rapport, montré avant tout envoi (§E6 5.4). */
fun formatCrashReport(input: CrashReportInput): String = buildString {
    appendLine("Application : BDthèque ${input.appVersion}")
    appendLine("Android : ${input.androidVersion}")
    appendLine("Appareil : ${input.deviceModel}")
    appendLine("Horodatage : ${input.timestampMillis}")
    appendLine()
    append(input.stackTrace)
}

private const val RETENTION_DAYS = 30
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** Un rapport plus vieux que 30 jours est purgé automatiquement, jamais envoyé (§E6 5.4). */
fun isCrashReportExpired(fileTimestampMillis: Long, nowMillis: Long, retentionDays: Int = RETENTION_DAYS): Boolean =
    nowMillis - fileTimestampMillis > retentionDays.toLong() * MILLIS_PER_DAY
