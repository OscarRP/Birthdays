package com.oscarruiz.birthdates.ui.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oscarruiz.birthdates.AppContainer
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.ui.common.appViewModel
import com.oscarruiz.birthdates.ui.common.quantityString
import com.oscarruiz.birthdates.ui.settings.SettingRow
import com.oscarruiz.birthdates.ui.settings.backupTimeText
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRoute(container: AppContainer, onBack: () -> Unit) {
    val vm = appViewModel {
        BackupViewModel(
            container.driveBackup, container.fileBackup, container.birthdayRepository, container.settingsRepository,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        vm.onAuthorizationResult(if (result.resultCode == Activity.RESULT_OK) result.data else null)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFrom)
    }

    LaunchedEffect(Unit) {
        vm.authRequests.collect { sender -> authLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
    }
    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            val res = context.resources
            val text = when (message) {
                is BackupMessage.BackedUp -> res.getQuantityString(R.plurals.backup_msg_backed_up, message.count, message.count)
                is BackupMessage.Restored -> res.getQuantityString(R.plurals.backup_msg_restored, message.count, message.count)
                is BackupMessage.Exported -> res.getQuantityString(R.plurals.backup_msg_exported, message.count, message.count)
                is BackupMessage.Imported -> res.getQuantityString(R.plurals.backup_msg_imported, message.count, message.count)
                BackupMessage.NoBackupFound -> res.getString(R.string.backup_msg_none)
                BackupMessage.InvalidFile -> res.getString(R.string.backup_msg_invalid)
                BackupMessage.Error -> res.getString(R.string.backup_msg_error)
            }
            snackbar.showSnackbar(text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = { Text(stringResource(R.string.backup_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))

            if (state.connected) {
                ConnectedCard(state)
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = vm::backupNow,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(stringResource(R.string.backup_now)) }
                SettingRow(
                    icon = Icons.Default.DateRange,
                    title = stringResource(R.string.backup_auto),
                    subtitle = stringResource(R.string.backup_auto_hint),
                    onClick = { vm.setAutoBackup(!state.autoBackup) },
                    trailing = { Switch(checked = state.autoBackup, onCheckedChange = { vm.setAutoBackup(it) }) },
                )
                SettingRow(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.backup_restore),
                    subtitle = stringResource(R.string.backup_restore_hint),
                    onClick = if (state.busy) null else vm::checkRestore,
                )
            } else {
                DisconnectedCard(busy = state.busy, onConnect = vm::connect)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Row(Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.backup_privacy_note), style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                text = stringResource(R.string.backup_file_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
            )
            SettingRow(
                icon = Icons.Default.Share,
                title = stringResource(R.string.backup_export),
                subtitle = stringResource(R.string.backup_export_hint),
                onClick = { exportLauncher.launch("birthdays-${LocalDate.now()}.json") },
            )
            SettingRow(
                icon = Icons.Default.Done,
                title = stringResource(R.string.backup_import),
                subtitle = stringResource(R.string.backup_import_hint),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            )

            if (state.connected) {
                SettingRow(
                    icon = Icons.Default.Close,
                    title = stringResource(R.string.backup_disconnect),
                    subtitle = stringResource(R.string.backup_disconnect_hint),
                    onClick = { vm.disconnect() },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    state.pendingRestore?.let { backup ->
        val date = backup.modifiedAt?.let { backupTimeText(it.toEpochMilli()) }
        AlertDialog(
            onDismissRequest = vm::dismissRestore,
            icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            title = { Text(stringResource(R.string.restore_title)) },
            text = {
                Column {
                    Text(quantityString(R.plurals.restore_body_count, backup.birthdays.size, backup.birthdays.size))
                    if (date != null) Text(stringResource(R.string.restore_body_date, date))
                    if (state.localCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                quantityString(R.plurals.restore_replace_warning, state.localCount, state.localCount),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::confirmRestore) { Text(stringResource(R.string.restore_confirm)) } },
            dismissButton = { TextButton(onClick = vm::dismissRestore) { Text(stringResource(R.string.restore_dismiss)) } },
        )
    }
}

@Composable
private fun ConnectedCard(state: BackupUiState) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.backup_connected_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.lastBackupAt?.let { stringResource(R.string.backup_last, backupTimeText(it)) }
                    ?: stringResource(R.string.backup_never),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = quantityString(R.plurals.backup_local_count, state.localCount, state.localCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.account?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DisconnectedCard(busy: Boolean, onConnect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.backup_connect_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.backup_connect_body), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onConnect, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.backup_connect))
                }
            }
        }
    }
}
