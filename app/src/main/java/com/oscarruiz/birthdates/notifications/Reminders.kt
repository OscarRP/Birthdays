package com.oscarruiz.birthdates.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oscarruiz.birthdates.BirthdaysApp
import com.oscarruiz.birthdates.domain.BirthdayCalculator
import com.oscarruiz.birthdates.domain.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Un único trabajo diario revisa qué cumpleaños hay que avisar y se reprograma a sí mismo.
 * No se usa AlarmManager exacto: no hace falta el permiso SCHEDULE_EXACT_ALARM y
 * unos minutos de retraso no importan para un cumpleaños.
 */
object ReminderScheduler {

    private const val WORK_NAME = "daily_birthday_reminder"

    fun schedule(context: Context, settings: AppSettings, fromWorker: Boolean = false) {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayAt = today.atTime(settings.reminderHour, settings.reminderMinute)
        val alreadyNotifiedToday = settings.lastReminderDate == today.toString()

        val next = when {
            now.isBefore(todayAt) -> todayAt
            !alreadyNotifiedToday -> now // Hoy aún no se avisó (móvil apagado, hora cambiada...).
            else -> todayAt.plusDays(1)
        }
        val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(0)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        // Desde el propio worker se encadena para no cancelarse a sí mismo.
        val policy = if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as BirthdaysApp).container
        val settingsRepo = container.settingsRepository
        val settings = settingsRepo.settings.first()
        val today = LocalDate.now()
        val reminderTime = today.atTime(settings.reminderHour, settings.reminderMinute)

        if (settings.lastReminderDate != today.toString() && !LocalDateTime.now().isBefore(reminderTime)) {
            val reminders = BirthdayCalculator.remindersFor(
                birthdays = container.birthdayRepository.getAll(),
                today = today,
                offsets = settings.reminderOffsets,
                policy = settings.leapDayPolicy,
            )
            NotificationHelper.showReminders(applicationContext, reminders)
            settingsRepo.setLastReminderDate(today.toString())
        }

        ReminderScheduler.schedule(applicationContext, settingsRepo.settings.first(), fromWorker = true)
        return Result.success()
    }
}

/** Red de seguridad: tras reiniciar el móvil se recalcula el siguiente aviso. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val container = (context.applicationContext as BirthdaysApp).container
        container.appScope.launch {
            try {
                ReminderScheduler.schedule(context.applicationContext, container.settingsRepository.settings.first())
            } finally {
                pending.finish()
            }
        }
    }
}
