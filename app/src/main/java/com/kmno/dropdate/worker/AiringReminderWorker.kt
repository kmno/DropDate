package com.kmno.dropdate.worker

import android.annotation.SuppressLint
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
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kmno.dropdate.MainActivity
import com.kmno.dropdate.R
import com.kmno.dropdate.data.local.dao.ReleaseDao
import com.kmno.dropdate.data.local.entity.ReleaseEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class AiringReminderWorker
    @AssistedInject
    constructor(
        @Assisted private val context: Context,
        @Assisted workerParams: WorkerParameters,
        private val releaseDao: ReleaseDao,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result =
            runCatching { check() }
                .getOrElse { if (runAttemptCount < 2) Result.retry() else Result.success() }

        private suspend fun check(): Result {
            val isTest = inputData.getBoolean(KEY_TEST_MODE, false)

            if (isTest) {
                // Test path: skip DAO + dedup, fire immediately with dummy data
                if (areNotificationsAllowed()) {
                    ensureChannel()
                    showNotification(dummyReleaseEntity(), isEvening = true)
                }
                return Result.success()
            }

            // Production path
            val type = inputData.getString(KEY_NOTIF_TYPE) ?: TYPE_EVENING
            val isEvening = type == TYPE_EVENING
            val today = LocalDate.now()
            val targetDate = if (isEvening) today.plusDays(1) else today
            val targetStr = targetDate.toString()
            val prefix = if (isEvening) "day_before" else "same_day"

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getStringSet(KEY_NOTIFIED, emptySet()) ?: emptySet()

            // Prune stale entries whose air date has already passed
            val live =
                raw.filterTo(mutableSetOf()) { entry ->
                    val date = entry.substringAfterLast('|', "")
                    date.isNotEmpty() && !LocalDate.parse(date).isBefore(today)
                }

            val toNotify = releaseDao.getTrackedAiringOn(targetStr)
            val filteredToNotify = toNotify.filter { "$prefix|${it.id}|$targetStr" !in live }

            if (filteredToNotify.isNotEmpty() && areNotificationsAllowed()) {
                ensureChannel()
                val summaryId = if (isEvening) SUMMARY_ID_EVENING else SUMMARY_ID_MORNING
                if (filteredToNotify.size > INDIVIDUAL_NOTIF_LIMIT) {
                    showSummaryNotification(filteredToNotify, isEvening, summaryId)
                } else {
                    filteredToNotify.forEach { entity ->
                        showNotification(entity, isEvening)
                    }
                }
                filteredToNotify.forEach { entity ->
                    live.add("$prefix|${entity.id}|$targetStr")
                }
            }

            // Persist only when set actually changed (avoids unnecessary disk write)
            if (live != raw) {
                prefs.edit { putStringSet(KEY_NOTIFIED, live) }
            }

            return Result.success()
        }

        private fun areNotificationsAllowed(): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
            return true
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH, // enables sound + heads-up
                    ).apply {
                        description = context.getString(R.string.notification_channel_desc)
                    }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }

        @SuppressLint("MissingPermission") // guarded by areNotificationsAllowed() at call site
        private fun showNotification(entity: ReleaseEntity, isEvening: Boolean) {
            val friendlyLabel = formatFriendlyLabel(entity.episodeLabel)
            val text =
                if (!friendlyLabel.isNullOrBlank()) {
                    val resId =
                        if (isEvening) {
                            R.string.notification_episode_drops
                        } else {
                            R.string.notification_episode_drops_today
                        }
                    context.getString(resId, friendlyLabel)
                } else {
                    val resId =
                        if (isEvening) {
                            R.string.notification_drops_tomorrow
                        } else {
                            R.string.notification_drops_today
                        }
                    context.getString(resId)
                }
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(entity.title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // required for sound on pre-Oreo
                    .setContentIntent(createPendingIntent())
                    .setAutoCancel(true)
                    .build()
            try {
                NotificationManagerCompat.from(context).notify(entity.id.hashCode(), notification)
            } catch (_: SecurityException) {
                // Permission revoked between check and notify — safe to ignore
            }
        }

        @SuppressLint("MissingPermission")
        private fun showSummaryNotification(
            entities: List<ReleaseEntity>,
            isEvening: Boolean,
            summaryId: Int,
        ) {
            val resId =
                if (isEvening) {
                    R.string.notification_summary_tomorrow
                } else {
                    R.string.notification_summary_today
                }
            val title = context.getString(resId, entities.size)

            val inboxStyle =
                NotificationCompat
                    .InboxStyle()
                    .setBigContentTitle(title)

            // Show up to the first few titles in the expanded view
            entities.take(SUMMARY_ITEM_LIMIT).forEach {
                val label = formatFriendlyLabel(it.episodeLabel)
                inboxStyle.addLine("${it.title}${if (label != null) " ($label)" else ""}")
            }
            if (entities.size > SUMMARY_ITEM_LIMIT) {
                inboxStyle.setSummaryText("+${entities.size - SUMMARY_ITEM_LIMIT} more")
            }

            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(
                        entities
                            .take(INDIVIDUAL_NOTIF_LIMIT)
                            .joinToString(", ") { it.title }
                            .let { if (entities.size > INDIVIDUAL_NOTIF_LIMIT) "$it…" else it },
                    ).setStyle(inboxStyle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(createPendingIntent())
                    .setAutoCancel(true)
                    .build()

            NotificationManagerCompat.from(context).notify(summaryId, notification)
        }

        private fun createPendingIntent(): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun formatFriendlyLabel(label: String?): String? {
            if (label == null) return null

            val tvMazeMatch = Regex("""S\d+ · E(\d+)""").find(label)
            val aniListMatch = Regex("""Ep (\d+)""").find(label)

            return when {
                tvMazeMatch != null -> "Episode ${tvMazeMatch.groupValues[1].toInt()}"
                aniListMatch != null -> "Episode ${aniListMatch.groupValues[1]}"
                label.contains("eps (all)") -> "All Episodes"
                else -> label
            }
        }

        private fun dummyReleaseEntity() =
            ReleaseEntity(
                id = "debug_test",
                seriesId = "debug_series",
                title = "Debug: Airing Reminder Test",
                posterUrl = null,
                backdropUrl = null,
                type = "SERIES",
                status = "UPCOMING",
                premiered = LocalDate.now().plusDays(1).toString(),
                airDate = LocalDate.now().plusDays(1).toString(),
                airTime = "20:00",
                platform = "Debug",
                episodeLabel = "S01 · E01",
                rating = null,
                synopsis = null,
                genres = null,
                syncedAt = 0L,
            )

        companion object {
            private const val WORK_NAME_EVENING = "airing_reminder_evening"
            private const val WORK_NAME_MORNING = "airing_reminder_morning"
            private const val KEY_TEST_MODE = "test_mode"
            private const val KEY_NOTIF_TYPE = "notif_type"
            private const val TYPE_EVENING = "evening"
            private const val TYPE_MORNING = "morning"

            private const val CHANNEL_ID = "dropdate_airing_reminders"
            private const val PREFS_NAME = "airing_reminder_prefs"
            private const val KEY_NOTIFIED = "notified_ids"
            private const val SUMMARY_ID_EVENING = 997
            private const val SUMMARY_ID_MORNING = 998

            private const val INDIVIDUAL_NOTIF_LIMIT = 3
            private const val SUMMARY_ITEM_LIMIT = 5
            private const val BACKOFF_MINUTES = 30L

            private const val NOTIFY_HOUR_EVENING = 20 // 8 PM
            private const val NOTIFY_HOUR_MORNING = 9 // 9 AM

            fun schedule(workManager: WorkManager) {
                // Cancel legacy single worker if it exists
                workManager.cancelUniqueWork("airing_reminder")

                scheduleInternal(workManager, NOTIFY_HOUR_EVENING, WORK_NAME_EVENING, TYPE_EVENING, flexHours = 2)
                scheduleInternal(workManager, NOTIFY_HOUR_MORNING, WORK_NAME_MORNING, TYPE_MORNING, flexHours = 1)
            }

            private fun scheduleInternal(
                workManager: WorkManager,
                hour: Int,
                uniqueName: String,
                type: String,
                flexHours: Long,
            ) {
                val now = LocalDateTime.now()
                val targetToday =
                    now
                        .withHour(hour)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0)
                val nextRun = if (now.isBefore(targetToday)) targetToday else targetToday.plusDays(1)
                val delayMs = ChronoUnit.MILLIS.between(now, nextRun)

                val request =
                    PeriodicWorkRequestBuilder<AiringReminderWorker>(
                        repeatInterval = 1,
                        repeatIntervalTimeUnit = TimeUnit.DAYS,
                        flexTimeInterval = flexHours,
                        flexTimeIntervalUnit = TimeUnit.HOURS,
                    ).setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(KEY_NOTIF_TYPE to type))
                        .setConstraints(
                            Constraints
                                .Builder()
                                .setRequiresBatteryNotLow(true)
                                .build(),
                        ).setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            BACKOFF_MINUTES,
                            TimeUnit.MINUTES,
                        ).build()

                workManager.enqueueUniquePeriodicWork(
                    uniqueName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }

            fun scheduleTest(workManager: WorkManager) {
                val request =
                    OneTimeWorkRequestBuilder<AiringReminderWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(workDataOf(KEY_TEST_MODE to true))
                        .build()
                workManager.enqueue(request)
            }
        }
    }
