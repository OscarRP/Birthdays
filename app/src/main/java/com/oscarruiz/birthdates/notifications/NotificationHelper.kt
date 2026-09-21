package com.oscarruiz.birthdates.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
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
import com.oscarruiz.birthdates.MainActivity
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.domain.Reminder
import com.oscarruiz.birthdates.util.DateTexts

object NotificationHelper {

    const val CHANNEL_ID = "birthday_reminders"
    private const val TODAY_GROUP_ID = 1000

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Muestra los avisos del día. Si varios cumplen hoy, se agrupan en una sola notificación. */
    @SuppressLint("MissingPermission") // Comprobado en canPostNotifications().
    fun showReminders(context: Context, reminders: List<Reminder>) {
        if (reminders.isEmpty() || !canPostNotifications(context)) return
        val manager = NotificationManagerCompat.from(context)
        val res = context.resources
        val (today, upcoming) = reminders.partition { it.daysUntil == 0 }

        when {
            today.size == 1 -> {
                val r = today.first()
                val title = res.getString(R.string.notification_today_title, r.birthday.name)
                val text = r.turningAge?.let { res.getString(R.string.notification_today_age, it) }
                    ?: res.getString(R.string.notification_today_no_age)
                manager.notify(notificationId(r), build(context, title, text))
            }
            today.size > 1 -> {
                val title = res.getQuantityString(R.plurals.notification_today_many_title, today.size, today.size)
                val text = today.joinToString(", ") { it.birthday.name }
                manager.notify(TODAY_GROUP_ID, build(context, title, text))
            }
        }

        upcoming.forEach { r ->
            val title = res.getQuantityString(
                R.plurals.notification_upcoming_title, r.daysUntil, r.birthday.name, r.daysUntil,
            )
            val date = DateTexts.long(r.date)
            val text = r.turningAge?.let { res.getString(R.string.notification_upcoming_age, it, date) }
                ?: res.getString(R.string.notification_upcoming_no_age, date)
            manager.notify(notificationId(r), build(context, title, text))
        }
    }

    private fun build(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_cake)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(context))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notificationId(r: Reminder): Int = (r.birthday.id + ":" + r.daysUntil).hashCode()
}
