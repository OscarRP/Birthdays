package com.oscarruiz.birthdates.ui.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oscarruiz.birthdates.AppContainer
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.domain.model.Relation
import com.oscarruiz.birthdates.ui.common.appViewModel
import com.oscarruiz.birthdates.ui.common.labelRes
import com.oscarruiz.birthdates.util.DateTexts

@Composable
fun EditRoute(
    container: AppContainer,
    id: String?,
    isPro: Boolean,
    onDone: () -> Unit,
    onPro: () -> Unit,
) {
    val vm = appViewModel(key = "edit_$id") { EditViewModel(id, container.birthdayRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) { if (state.finished) onDone() }

    EditScreen(
        state = state,
        isPro = isPro,
        onBack = onDone,
        onName = vm::onName,
        onDay = vm::onDay,
        onMonth = vm::onMonth,
        onYear = vm::onYear,
        onUnknownYear = vm::onUnknownYear,
        onRelation = vm::onRelation,
        onNotes = vm::onNotes,
        onSave = vm::save,
        onDelete = vm::delete,
        onPro = onPro,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    state: EditUiState,
    isPro: Boolean,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onDay: (Int) -> Unit,
    onMonth: (Int) -> Unit,
    onYear: (String) -> Unit,
    onUnknownYear: (Boolean) -> Unit,
    onRelation: (Relation?) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPro: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(state.isNew, state.loading) {
        if (state.isNew && !state.loading) nameFocus.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = {
                    Text(stringResource(if (state.isNew) R.string.edit_title_new else R.string.edit_title_edit))
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(50.dp),
            ) {
                Text(stringResource(if (state.isNew) R.string.action_save else R.string.action_save_changes))
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onName,
                label = { Text(stringResource(R.string.edit_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.edit_name_error)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            )

            FieldLabel(stringResource(R.string.edit_date))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = stringResource(R.string.edit_day),
                    value = state.day.toString(),
                    options = (1..state.maxDay).map { it to it.toString() },
                    onSelect = onDay,
                    isError = state.dateError,
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = stringResource(R.string.edit_month),
                    value = DateTexts.monthName(state.month),
                    options = (1..12).map { it to DateTexts.monthName(it) },
                    onSelect = onMonth,
                    isError = state.dateError,
                    modifier = Modifier.weight(1.9f),
                )
                OutlinedTextField(
                    value = if (state.unknownYear) "" else state.yearText,
                    onValueChange = onYear,
                    label = { Text(stringResource(R.string.edit_year)) },
                    enabled = !state.unknownYear,
                    isError = state.yearError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.4f),
                )
            }
            if (state.dateError || state.yearError) {
                Text(
                    text = stringResource(if (state.yearError) R.string.edit_year_error else R.string.edit_date_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUnknownYear(!state.unknownYear) }
                    .padding(vertical = 2.dp),
            ) {
                Checkbox(checked = state.unknownYear, onCheckedChange = onUnknownYear)
                Text(stringResource(R.string.edit_unknown_year))
            }

            FieldLabel(stringResource(R.string.edit_relation))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Relation.entries.forEach { relation ->
                    val selected = state.relation == relation
                    FilterChip(
                        selected = selected,
                        onClick = { onRelation(if (selected) null else relation) },
                        label = { Text(stringResource(relation.labelRes())) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.width(FilterChipDefaults.IconSize)) }
                        } else null,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotes,
                label = { Text(stringResource(R.string.edit_notes)) },
                placeholder = { Text(stringResource(R.string.edit_notes_hint)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            // Función Pro (v2): aviso distinto para esta persona. Visible pero discreta.
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit_custom_reminder)) },
                supportingContent = { Text(stringResource(R.string.edit_custom_reminder_default)) },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                trailingContent = {
                    if (!isPro) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.width(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pro_badge), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable(enabled = !isPro, onClick = onPro),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.edit_delete_title, state.name)) },
            text = { Text(stringResource(R.string.edit_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

/** Campo de solo lectura que abre un menú. Más robusto que ExposedDropdownMenuBox entre versiones. */
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa transparente encima para abrir el menú al tocar el campo.
        Box(
            Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(key)
                    },
                )
            }
        }
    }
}
