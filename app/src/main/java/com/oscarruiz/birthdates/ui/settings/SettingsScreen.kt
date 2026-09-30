package com.oscarruiz.birthdates.ui.settings

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscarruiz.birthdates.AppContainer
import com.oscarruiz.birthdates.BuildConfig
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.data.SettingsRepository
import com.oscarruiz.birthdates.domain.model.AccentColor
import com.oscarruiz.birthdates.domain.model.AppSettings
import com.oscarruiz.birthdates.domain.model.LeapDayPolicy
import com.oscarruiz.birthdates.domain.model.ReminderOffsets
import com.oscarruiz.birthdates.domain.model.ThemeMode
import com.oscarruiz.birthdates.notifications.NotificationHelper
import com.oscarruiz.birthdates.ui.common.appViewModel
import com.oscarruiz.birthdates.ui.common.findActivity
import com.oscarruiz.birthdates.ui.theme.swatch
import com.oscarruiz.birthdates.util.DateTexts
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://oscarrp.github.io/Birthdays/site/privacidad.html"

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val reschedule: suspend () -> Unit,
) : ViewModel() {

    fun toggleOffset(current: Set<Int>, offset: Int) = viewModelScope.launch {
        val updated = if (offset in current) current - offset else current + offset
        settings.setReminderOffsets(updated)
        reschedule()
    }

    fun setTime(hour: Int, minute: Int) = viewModelScope.launch {
        settings.setReminderTime(hour, minute)
        reschedule()
    }

    fun setLeapPolicy(policy: LeapDayPolicy) = viewModelScope.launch { settings.setLeapDayPolicy(policy) }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setAccentColor(color: AccentColor) = viewModelScope.launch { settings.setAccentColor(color) }
}

@Composable
fun SettingsRoute(
    container: AppContainer,
    settings: AppSettings,
    onBack: () -> Unit,
    onPro: () -> Unit,
    onBackup: () -> Unit,
) {
    val vm = appViewModel { SettingsViewModel(container.settingsRepository, container::rescheduleReminders) }
    val context = LocalContext.current
    SettingsScreen(
        settings = settings,
        canPostNotifications = NotificationHelper.canPostNotifications(context),
        showPrivacyOptions = !settings.isPro && container.consentManager.isPrivacyOptionsRequired,
        onBack = onBack,
        onPro = onPro,
        onBackup = onBackup,
        onToggleOffset = { vm.toggleOffset(settings.reminderOffsets, it) },
        onTimeChange = { h, m -> vm.setTime(h, m) },
        onLeapPolicy = { vm.setLeapPolicy(it) },
        onTheme = { vm.setTheme(it) },
        onAccentColor = { if (settings.isPro) vm.setAccentColor(it) else onPro() },
        onPrivacyOptions = { context.findActivity()?.let(container.consentManager::showPrivacyOptions) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    canPostNotifications: Boolean,
    showPrivacyOptions: Boolean,
    onBack: () -> Unit,
    onPro: () -> Unit,
    onBackup: () -> Unit,
    onToggleOffset: (Int) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onLeapPolicy: (LeapDayPolicy) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onAccentColor: (AccentColor) -> Unit,
    onPrivacyOptions: () -> Unit,
) {
    val context = LocalContext.current
    var showHelp by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            ProCard(isPro = settings.isPro, onClick = onPro)

            // ---- Avisos ----
            SectionHeader(stringResource(R.string.settings_reminders))
            Text(
                text = stringResource(R.string.settings_notify_me),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderOffsets.ALL.forEach { offset ->
                    val selected = offset in settings.reminderOffsets
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleOffset(offset) },
                        label = { Text(offsetLabel(offset)) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
            }
            SettingRow(
                icon = Icons.Default.DateRange,
                title = stringResource(R.string.settings_time),
                subtitle = stringResource(R.string.settings_time_hint),
                trailing = {
                    Text(
                        DateTexts.time(settings.reminderHour, settings.reminderMinute),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onTimeChange(h, m) },
                        settings.reminderHour,
                        settings.reminderMinute,
                        DateFormat.is24HourFormat(context),
                    ).show()
                },
            )
            SettingRow(
                icon = if (canPostNotifications) Icons.Default.Notifications else Icons.Default.Warning,
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(
                    if (canPostNotifications) R.string.settings_notifications_on else R.string.settings_notifications_off,
                ),
                onClick = { openNotificationSettings(context) },
            )

            // ---- Fechas ----
            SectionHeader(stringResource(R.string.settings_dates))
            Text(
                text = stringResource(R.string.settings_leap_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RadioRow(
                text = stringResource(R.string.settings_leap_feb28),
                selected = settings.leapDayPolicy == LeapDayPolicy.FEB_28,
                onClick = { onLeapPolicy(LeapDayPolicy.FEB_28) },
            )
            RadioRow(
                text = stringResource(R.string.settings_leap_mar1),
                selected = settings.leapDayPolicy == LeapDayPolicy.MAR_1,
                onClick = { onLeapPolicy(LeapDayPolicy.MAR_1) },
            )

            // ---- Apariencia ----
            SectionHeader(stringResource(R.string.settings_appearance))
            val modes = listOf(
                ThemeMode.LIGHT to R.string.settings_theme_light,
                ThemeMode.DARK to R.string.settings_theme_dark,
                ThemeMode.SYSTEM to R.string.settings_theme_system,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    ) { Text(stringResource(label)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_colors),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AccentColor.entries.forEach { accent ->
                    AccentSwatch(
                        accent = accent,
                        selected = settings.isPro && settings.accentColor == accent,
                        locked = !settings.isPro,
                        onClick = { onAccentColor(accent) },
                    )
                }
            }
            if (!settings.isPro) {
                Text(
                    text = stringResource(R.string.settings_colors_pro_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // ---- Datos ----
            SectionHeader(stringResource(R.string.settings_data))
            SettingRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.backup_title),
                subtitle = when {
                    !settings.driveConnected -> stringResource(R.string.settings_backup_off)
                    settings.lastBackupAt != null -> stringResource(
                        R.string.settings_backup_last, backupTimeText(settings.lastBackupAt),
                    )
                    else -> stringResource(R.string.settings_backup_connected)
                },
                chevron = true,
                onClick = onBackup,
            )

            // ---- Privacidad ----
            SectionHeader(stringResource(R.string.settings_privacy))
            if (showPrivacyOptions) {
                SettingRow(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_ads_consent),
                    subtitle = stringResource(R.string.settings_ads_consent_hint),
                    chevron = true,
                    onClick = onPrivacyOptions,
                )
            }
            SettingRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_privacy_policy),
                subtitle = stringResource(R.string.settings_privacy_policy_hint),
                onClick = { openUrl(context, PRIVACY_POLICY_URL) },
            )

            // ---- Ayuda ----
            SectionHeader(stringResource(R.string.settings_help))
            SettingRow(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.settings_help_reminders),
                subtitle = stringResource(R.string.settings_help_reminders_hint),
                chevron = true,
                onClick = { showHelp = true },
            )

            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.settings_help_reminders)) },
            text = { Text(stringResource(R.string.help_battery_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showHelp = false
                    openBatterySettings(context)
                }) { Text(stringResource(R.string.help_battery_open)) }
            },
            dismissButton = {
                TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}

@Composable
private fun offsetLabel(offset: Int): String = when (offset) {
    0 -> stringResource(R.string.offset_same_day)
    1 -> stringResource(R.string.offset_one_day)
    7 -> stringResource(R.string.offset_one_week)
    else -> stringResource(R.string.offset_days, offset)
}

@Composable
private fun ProCard(isPro: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !isPro,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = if (isPro) Icons.Default.Check else Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (isPro) R.string.settings_pro_active else R.string.pro_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(if (isPro) R.string.settings_pro_thanks else R.string.settings_pro_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!isPro) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun accentName(accent: AccentColor): String = when (accent) {
    AccentColor.BERRY -> stringResource(R.string.accent_berry)
    AccentColor.OCEAN -> stringResource(R.string.accent_ocean)
    AccentColor.FOREST -> stringResource(R.string.accent_forest)
    AccentColor.SUNSET -> stringResource(R.string.accent_sunset)
    AccentColor.LAVENDER -> stringResource(R.string.accent_lavender)
}

@Composable
private fun AccentSwatch(accent: AccentColor, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val color = accent.swatch(dark = androidx.compose.foundation.isSystemInDarkTheme())
    val description = accentName(accent)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(44.dp),
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = description,
                    tint = Color.White,
                    modifier = Modifier.padding(11.dp),
                )
            } else if (locked) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = description,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

fun backupTimeText(timestamp: Long): String {
    val zoned = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
    return DateTexts.dayMonthTime(zoned.toLocalDate(), zoned.toLocalTime())
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    safeStart(context, intent)
}

private fun openBatterySettings(context: Context) {
    safeStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun openUrl(context: Context, url: String) {
    safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun safeStart(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Algunos fabricantes no tienen esta pantalla; no hacemos nada.
    }
}
