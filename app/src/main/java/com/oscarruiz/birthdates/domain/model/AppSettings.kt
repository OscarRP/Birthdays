package com.oscarruiz.birthdates.domain.model

/** Qué día se celebra a quien nació el 29 de febrero en años no bisiestos. */
enum class LeapDayPolicy { FEB_28, MAR_1 }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Paletas de color de la app. BERRY es la de siempre; el resto son función Pro. */
enum class AccentColor { BERRY, OCEAN, FOREST, SUNSET, LAVENDER }

object ReminderOffsets {
    /** Días de antelación que se pueden elegir en Ajustes. */
    val ALL = listOf(0, 1, 3, 7)
    val DEFAULT = setOf(0, 3)
}

data class AppSettings(
    // Avisos
    val reminderOffsets: Set<Int> = ReminderOffsets.DEFAULT,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastReminderDate: String? = null,
    // Fechas y apariencia
    val leapDayPolicy: LeapDayPolicy = LeapDayPolicy.FEB_28,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.BERRY,
    // Monetización
    val isPro: Boolean = false,
    val adsIntroShown: Boolean = false,
    val proPromptShown: Boolean = false,
    // Permisos
    val notificationPermissionAsked: Boolean = false,
    // Copia en Google Drive
    val driveConnected: Boolean = false,
    val driveAccount: String? = null,
    val autoBackup: Boolean = true,
    val lastBackupAt: Long? = null,
    val backupPending: Boolean = false,
)
