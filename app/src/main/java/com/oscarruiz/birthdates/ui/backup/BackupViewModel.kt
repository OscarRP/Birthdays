package com.oscarruiz.birthdates.ui.backup

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscarruiz.birthdates.backup.DriveBackupRepository
import com.oscarruiz.birthdates.backup.FileBackupRepository
import com.oscarruiz.birthdates.backup.InvalidBackupException
import com.oscarruiz.birthdates.backup.RemoteBackup
import com.oscarruiz.birthdates.data.BirthdayRepository
import com.oscarruiz.birthdates.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BackupMessage {
    data class BackedUp(val count: Int) : BackupMessage
    data class Restored(val count: Int) : BackupMessage
    data object NoBackupFound : BackupMessage
    data class Exported(val count: Int) : BackupMessage
    data class Imported(val count: Int) : BackupMessage
    data object InvalidFile : BackupMessage
    data object Error : BackupMessage
}

data class BackupUiState(
    val connected: Boolean = false,
    val account: String? = null,
    val lastBackupAt: Long? = null,
    val autoBackup: Boolean = true,
    val localCount: Int = 0,
    val busy: Boolean = false,
    val pendingRestore: RemoteBackup? = null,
)

private data class Transient(
    val busy: Boolean = false,
    val pendingRestore: RemoteBackup? = null,
)

class BackupViewModel(
    private val drive: DriveBackupRepository,
    private val files: FileBackupRepository,
    private val birthdays: BirthdayRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private enum class Action { CONNECT, BACKUP, RESTORE }

    private val transient = MutableStateFlow(Transient())
    private var pendingAction: Action? = null

    private val _authRequests = Channel<IntentSender>(Channel.BUFFERED)
    /** La UI lanza estos IntentSender para que el usuario elija cuenta y acepte el permiso de Drive. */
    val authRequests: Flow<IntentSender> = _authRequests.receiveAsFlow()

    private val _messages = Channel<BackupMessage>(Channel.BUFFERED)
    val messages: Flow<BackupMessage> = _messages.receiveAsFlow()

    val state: StateFlow<BackupUiState> = combine(
        settings.settings,
        birthdays.observeAll().map { it.size },
        transient,
    ) { s, count, t ->
        BackupUiState(
            connected = s.driveConnected,
            account = s.driveAccount,
            lastBackupAt = s.lastBackupAt,
            autoBackup = s.autoBackup,
            localCount = count,
            busy = t.busy,
            pendingRestore = t.pendingRestore,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    fun connect() = runWithToken(Action.CONNECT)
    fun backupNow() = runWithToken(Action.BACKUP)
    fun checkRestore() = runWithToken(Action.RESTORE)

    fun onAuthorizationResult(data: Intent?) {
        val action = pendingAction ?: return
        pendingAction = null
        val token = drive.tokenFromIntent(data) ?: return
        viewModelScope.launch { perform(action, token) }
    }

    fun confirmRestore() {
        val backup = transient.value.pendingRestore ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                drive.restore(backup)
                transient.update { it.copy(pendingRestore = null) }
                _messages.send(BackupMessage.Restored(backup.birthdays.size))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.send(BackupMessage.Error)
            } finally {
                setBusy(false)
            }
        }
    }

    fun dismissRestore() = transient.update { it.copy(pendingRestore = null) }

    fun disconnect() = viewModelScope.launch { settings.setDriveConnected(false, null) }

    fun setAutoBackup(enabled: Boolean) = viewModelScope.launch { settings.setAutoBackup(enabled) }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        try {
            _messages.send(BackupMessage.Exported(files.exportTo(uri)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.send(BackupMessage.Error)
        }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        try {
            _messages.send(BackupMessage.Imported(files.importFrom(uri)))
        } catch (e: InvalidBackupException) {
            _messages.send(BackupMessage.InvalidFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.send(BackupMessage.Error)
        }
    }

    private fun runWithToken(action: Action) {
        viewModelScope.launch {
            setBusy(true)
            try {
                val result = drive.authorize()
                val intent = result.pendingIntent
                if (result.hasResolution() && intent != null) {
                    pendingAction = action
                    _authRequests.send(intent.intentSender)
                    setBusy(false)
                } else {
                    val token = result.accessToken
                    if (token == null) {
                        _messages.send(BackupMessage.Error)
                        setBusy(false)
                    } else {
                        perform(action, token)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.send(BackupMessage.Error)
                setBusy(false)
            }
        }
    }

    private suspend fun perform(action: Action, token: String) {
        setBusy(true)
        try {
            when (action) {
                Action.CONNECT -> {
                    settings.setDriveConnected(true, drive.accountEmail(token))
                    val remote = drive.fetchBackup(token)
                    val localCount = birthdays.getAll().size
                    when {
                        // Móvil nuevo o reinstalación: ofrecer recuperar la copia.
                        remote != null && localCount == 0 -> transient.update { it.copy(pendingRestore = remote) }
                        // Primera vez: subir lo que haya.
                        remote == null && localCount > 0 ->
                            _messages.send(BackupMessage.BackedUp(drive.backupNow(token)))
                        else -> Unit
                    }
                }
                Action.BACKUP -> _messages.send(BackupMessage.BackedUp(drive.backupNow(token)))
                Action.RESTORE -> {
                    val remote = drive.fetchBackup(token)
                    if (remote == null) {
                        _messages.send(BackupMessage.NoBackupFound)
                    } else {
                        transient.update { it.copy(pendingRestore = remote) }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.send(BackupMessage.Error)
        } finally {
            setBusy(false)
        }
    }

    private fun setBusy(value: Boolean) = transient.update { it.copy(busy = value) }
}
