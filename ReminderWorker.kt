package com.learnenglishsimply.app

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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Personalized daily reminder: uses the student's name and chosen hour. */
class ReminderWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val p = Repo.prefs(ctx)
        if (!p.getBoolean("reminder_on", true)) return Result.success()
        show(p.getString("name", "") ?: "")
        schedule(ctx) // plan tomorrow's reminder
        return Result.success()
    }

    private fun show(name: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val first = name.trim().substringBefore(' ').ifBlank { "Student" }
        val lines = listOf(
            "Hi $first! 10 minutes of English today keeps your streak alive.",
            "$first, a new lesson is waiting for you.",
            "Small steps every day = big English. Ready, $first?",
            "Don't forget your 15 coins today, $first!"
        )
        val msg = lines[(System.currentTimeMillis() / 86_400_000L % lines.size).toInt()]
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Learn English Simply")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(1001, n)
    }

    companion object {
        private const val CHANNEL = "reminders"
        private const val WORK = "daily_reminder"

        fun schedule(ctx: Context) {
            val p = Repo.prefs(ctx)
            val wm = WorkManager.getInstance(ctx)
            if (!p.getBoolean("reminder_on", true)) { wm.cancelUniqueWork(WORK); return }
            val hour = p.getInt("reminder_hour", 18)
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis() + 60_000) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = c.timeInMillis - System.currentTimeMillis()
            wm.enqueueUniqueWork(
                WORK, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            )
        }
    }
}
