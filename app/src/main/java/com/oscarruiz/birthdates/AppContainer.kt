package com.oscarruiz.birthdates

import android.app.Application
import android.content.Context
import com.oscarruiz.birthdates.backup.BackupWorker
import com.oscarruiz.birthdates.backup.DriveBackupRepository
import com.oscarruiz.birthdates.backup.FileBackupRepository
import com.oscarruiz.birthdates.data.BirthdayRepository
import com.oscarruiz.birthdates.data.SettingsRepository
import com.oscarruiz.birthdates.data.local.AppDatabase
import com.oscarruiz.birthdates.monetization.AdsController
import com.oscarruiz.birthdates.monetization.BillingRepository
import com.oscarruiz.birthdates.monetization.ConsentManager
import com.oscarruiz.birthdates.notifications.NotificationHelper
import com.oscarruiz.birthdates.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

/** Inyección de dependencias manual: con pocas pantallas no hace falta Hilt. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(appContext)

    private val database = AppDatabase.build(appContext)

    val birthdayRepository = BirthdayRepository(database.birthdayDao()) {
        settingsRepository.setBackupPending(true)
    }

    val fileBackup = FileBackupRepository(appContext, birthdayRepository)
    val driveBackup = DriveBackupRepository(appContext, birthdayRepository, settingsRepository)

    val billingRepository = BillingRepository(appContext, settingsRepository, appScope)
    val consentManager = ConsentManager(appContext)
    val adsController = AdsController(appContext, consentManager, appScope)

    suspend fun rescheduleReminders() {
        ReminderScheduler.schedule(appContext, settingsRepository.settings.first())
    }
}

class CandelioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        BackupWorker.schedule(this)
        // Los avisos se reprograman al abrir la app (MainActivity) y tras reiniciar (BootReceiver),
        // no aquí: este onCreate también se ejecuta cuando arranca el propio worker.
    }
}
