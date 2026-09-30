package com.oscarruiz.birthdates.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oscarruiz.birthdates.domain.model.AccentColor
import com.oscarruiz.birthdates.domain.model.AppSettings
import com.oscarruiz.birthdates.domain.model.LeapDayPolicy
import com.oscarruiz.birthdates.domain.model.ReminderOffsets
import com.oscarruiz.birthdates.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val OFFSETS = stringSetPreferencesKey("reminder_offsets")
        val HOUR = intPreferencesKey("reminder_hour")
        val MINUTE = intPreferencesKey("reminder_minute")
        val LAST_REMINDER_DATE = stringPreferencesKey("last_reminder_date")
        val LEAP_POLICY = stringPreferencesKey("leap_day_policy")
        val THEME = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val ADS_INTRO_SHOWN = booleanPreferencesKey("ads_intro_shown")
        val PRO_PROMPT_SHOWN = booleanPreferencesKey("pro_prompt_shown")
        val NOTIFICATION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val DRIVE_CONNECTED = booleanPreferencesKey("drive_connected")
        val DRIVE_ACCOUNT = stringPreferencesKey("drive_account")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val BACKUP_PENDING = booleanPreferencesKey("backup_pending")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            AppSettings(
                reminderOffsets = p[Keys.OFFSETS]?.mapNotNull { it.toIntOrNull() }?.toSet()
                    ?: ReminderOffsets.DEFAULT,
                reminderHour = p[Keys.HOUR] ?: 9,
                reminderMinute = p[Keys.MINUTE] ?: 0,
                lastReminderDate = p[Keys.LAST_REMINDER_DATE],
                leapDayPolicy = p[Keys.LEAP_POLICY].toEnum(LeapDayPolicy.FEB_28),
                themeMode = p[Keys.THEME].toEnum(ThemeMode.SYSTEM),
                accentColor = p[Keys.ACCENT_COLOR].toEnum(AccentColor.BERRY),
                //isPro = p[Keys.IS_PRO] ?: false,
                isPro = true,
                adsIntroShown = p[Keys.ADS_INTRO_SHOWN] ?: false,
                proPromptShown = p[Keys.PRO_PROMPT_SHOWN] ?: false,
                notificationPermissionAsked = p[Keys.NOTIFICATION_ASKED] ?: false,
                driveConnected = p[Keys.DRIVE_CONNECTED] ?: false,
                driveAccount = p[Keys.DRIVE_ACCOUNT],
                autoBackup = p[Keys.AUTO_BACKUP] ?: true,
                lastBackupAt = p[Keys.LAST_BACKUP_AT],
                backupPending = p[Keys.BACKUP_PENDING] ?: false,
            )
        }
        .distinctUntilChanged()

    suspend fun setReminderOffsets(offsets: Set<Int>) {
        store.edit { it[Keys.OFFSETS] = offsets.map(Int::toString).toSet() }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        store.edit {
            it[Keys.HOUR] = hour
            it[Keys.MINUTE] = minute
        }
    }

    suspend fun setLastReminderDate(isoDate: String) {
        store.edit { it[Keys.LAST_REMINDER_DATE] = isoDate }
    }

    suspend fun setLeapDayPolicy(policy: LeapDayPolicy) {
        store.edit { it[Keys.LEAP_POLICY] = policy.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setAccentColor(color: AccentColor) {
        store.edit { it[Keys.ACCENT_COLOR] = color.name }
    }

    suspend fun setPro(isPro: Boolean) {
        store.edit { it[Keys.IS_PRO] = isPro }
    }

    suspend fun setAdsIntroShown() {
        store.edit { it[Keys.ADS_INTRO_SHOWN] = true }
    }

    suspend fun setProPromptShown() {
        store.edit { it[Keys.PRO_PROMPT_SHOWN] = true }
    }

    suspend fun setNotificationPermissionAsked() {
        store.edit { it[Keys.NOTIFICATION_ASKED] = true }
    }

    suspend fun setDriveConnected(connected: Boolean, account: String?) {
        store.edit {
            it[Keys.DRIVE_CONNECTED] = connected
            if (account != null) it[Keys.DRIVE_ACCOUNT] = account else it.remove(Keys.DRIVE_ACCOUNT)
            if (!connected) it.remove(Keys.LAST_BACKUP_AT)
        }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        store.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    suspend fun setBackupPending(pending: Boolean) {
        store.edit { it[Keys.BACKUP_PENDING] = pending }
    }

    suspend fun onBackupCompleted(timestamp: Long) {
        store.edit {
            it[Keys.LAST_BACKUP_AT] = timestamp
            it[Keys.BACKUP_PENDING] = false
        }
    }
}
