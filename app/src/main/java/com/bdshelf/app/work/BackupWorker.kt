package com.bdshelf.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bdshelf.app.BdShelfApplication
import java.util.concurrent.TimeUnit

/**
 * Sauvegarde automatique quotidienne de la collection (§6.9).
 *
 * Entièrement locale (aucun réseau requis) : voir
 * [com.bdshelf.app.data.backup.BackupManager] pour la rotation et les
 * garde-fous. Toujours `success` — une sauvegarde manquée un jour se rattrape
 * le lendemain, inutile de solliciter les retries de WorkManager.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BdShelfApplication
        app.backupManager.backupNow()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "collection_backup"

        /** Planifie la sauvegarde 1x/jour, batterie non faible. */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
