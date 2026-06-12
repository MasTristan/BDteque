package com.bdshelf.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.BuildConfig
import com.bdshelf.app.MainActivity
import com.bdshelf.app.R
import com.bdshelf.app.data.remote.ReleaseItem
import com.bdshelf.app.domain.ReleaseWithOwnership
import com.bdshelf.app.domain.crossReferenceReleases
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Synchronisation périodique des sorties + notifications locales (§7 SPEC).
 *
 * 1x/jour : fetch `releases.json`, croise avec la collection (séries suivies,
 * tomes non possédés), et notifie uniquement les sorties jamais notifiées
 * auparavant. Jamais de notification pour un tome déjà possédé.
 */
class ReleasesSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BdShelfApplication
        val prefs = app.userPreferencesRepository

        if (!prefs.notificationsEnabled.first()) return Result.success()

        val url = prefs.releasesUrlOverride.first()?.takeIf { it.isNotBlank() } ?: BuildConfig.RELEASES_URL
        app.releasesRepository.refresh(url)

        val document = app.releasesRepository.loadCached()
        val tracked = app.collectionRepository.trackedSeriesIds()
        val owned = app.collectionRepository.ownedTomeRefs()
        val crossed = crossReferenceReleases(document.releases, tracked, owned)

        val alreadyNotified = prefs.notifiedReleaseKeys.first()
        val newOnes = crossed.filter { !it.owned && releaseKey(it.release) !in alreadyNotified }

        if (newOnes.isNotEmpty()) {
            sendNotification(newOnes)
            prefs.addNotifiedReleaseKeys(newOnes.map { releaseKey(it.release) }.toSet())
        }

        return Result.success()
    }

    private fun sendNotification(newOnes: List<ReleaseWithOwnership>) {
        val context = applicationContext
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_RELEASES, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = if (newOnes.size == 1) {
            val item = newOnes.first().release
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notif_new_release_title))
                .setContentText(context.getString(R.string.notif_new_release_body, item.seriesTitle, item.tomeNumber))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
        } else {
            val body = context.resources.getQuantityString(R.plurals.notif_new_releases_body, newOnes.size, newOnes.size)
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notif_new_releases_title))
                .setContentText(body)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "releases"
        private const val NOTIFICATION_ID = 1
        private const val WORK_NAME = "releases_sync"

        private fun releaseKey(release: ReleaseItem) = "${release.seriesId}-${release.tomeNumber}"

        /** Planifie la vérification 1x/jour, réseau dispo + batterie non faible (§7). */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<ReleasesSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_description)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
