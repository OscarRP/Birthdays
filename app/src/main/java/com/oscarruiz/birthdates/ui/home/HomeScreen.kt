package com.oscarruiz.birthdates.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oscarruiz.birthdates.AppContainer
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.domain.Section
import com.oscarruiz.birthdates.domain.UpcomingBirthday
import com.oscarruiz.birthdates.domain.model.AppSettings
import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.monetization.AdaptiveBanner
import com.oscarruiz.birthdates.notifications.NotificationHelper
import com.oscarruiz.birthdates.ui.common.Avatar
import com.oscarruiz.birthdates.ui.common.Countdown
import com.oscarruiz.birthdates.ui.common.RelationTag
import com.oscarruiz.birthdates.ui.common.SectionTitle
import com.oscarruiz.birthdates.ui.common.appViewModel
import com.oscarruiz.birthdates.ui.common.quantityString
import com.oscarruiz.birthdates.util.DateTexts
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    container: AppContainer,
    settings: AppSettings,
    showAds: Boolean,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onSettings: () -> Unit,
    onPro: () -> Unit,
    onRestore: () -> Unit,
    onStartAds: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(container.birthdayRepository, container.settingsRepository) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }

    HomeScreen(
        state = state,
        settings = settings,
        showAds = showAds,
        onQueryChange = vm::onQueryChange,
        onAdd = onAdd,
        onEdit = onEdit,
        onDelete = vm::delete,
        onUndo = vm::undoDelete,
        onSettings = onSettings,
        onPro = onPro,
        onRestore = onRestore,
        onAdsIntroContinue = {
            vm.onAdsIntroShown()
            onStartAds()
        },
        onAdsIntroPro = {
            vm.onAdsIntroShown()
            onPro()
        },
        onNotificationAsked = { vm.onNotificationPermissionAsked() },
        onProPromptShown = { vm.onProPromptShown() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    settings: AppSettings,
    showAds: Boolean,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (Birthday) -> Unit,
    onUndo: () -> Unit,
    onSettings: () -> Unit,
    onPro: () -> Unit,
    onRestore: () -> Unit,
    onAdsIntroContinue: () -> Unit,
    onAdsIntroPro: () -> Unit,
    onNotificationAsked: () -> Unit,
    onProPromptShown: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searching by rememberSaveable { mutableStateOf(false) }

    val deleteWithUndo: (Birthday) -> Unit = { birthday ->
        onDelete(birthday)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.home_deleted, birthday.name),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    // Sugerencia de Pro, una sola vez, cuando ya hay 5 cumpleaños guardados.
    val currentSettings by rememberUpdatedState(settings)
    LaunchedEffect(state.total >= 5) {
        if (state.total >= 5 && !currentSettings.isPro && !currentSettings.proPromptShown) {
            onProPromptShown()
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.home_pro_prompt),
                actionLabel = context.getString(R.string.home_pro_prompt_action),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onPro()
        }
    }

    Scaffold(
        topBar = {
            if (searching) {
                SearchTopBar(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    onClose = {
                        onQueryChange("")
                        searching = false
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    },
                    actions = {
                        if (state.total > 0) {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                            }
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (state.total > 0 && !searching) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // Sin anuncios en el estado vacío.
            if (showAds && state.total > 0) {
                AdaptiveBanner(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding))
            state.total == 0 -> EmptyState(
                modifier = Modifier.padding(padding),
                onAdd = onAdd,
                onRestore = onRestore,
            )
            else -> BirthdayList(
                state = state,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                onEdit = onEdit,
                onDelete = deleteWithUndo,
            )
        }
    }

    if (!settings.isPro && !settings.adsIntroShown) {
        AdsIntroSheet(onContinue = onAdsIntroContinue, onPro = onAdsIntroPro)
    }

    NotificationPermissionPrompt(
        hasBirthdays = state.total > 0,
        alreadyAsked = settings.notificationPermissionAsked,
        canPost = NotificationHelper.canPostNotifications(context),
        onAsked = onNotificationAsked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.home_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp).focusRequester(focus),
            )
        },
    )
}

@Composable
private fun BirthdayList(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onEdit: (String) -> Unit,
    onDelete: (Birthday) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "count") {
            Text(
                text = if (state.query.isBlank()) {
                    quantityString(R.plurals.home_count, state.total, state.total)
                } else {
                    quantityString(R.plurals.home_results, state.sections.sumOf { it.items.size }, state.sections.sumOf { it.items.size })
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        if (state.sections.isEmpty()) {
            item(key = "no_results") {
                Text(
                    text = stringResource(R.string.home_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        state.sections.forEach { section ->
            item(key = "header_${section.section}") {
                SectionTitle(
                    text = stringResource(section.section.titleRes()),
                    trailing = if (section.section == Section.TODAY) null else section.items.size.toString(),
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                )
            }
            if (section.section == Section.TODAY) {
                items(section.items, key = { it.birthday.id }) { item ->
                    TodayCard(item, onClick = { onEdit(item.birthday.id) })
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                itemsIndexed(section.items, key = { _, item -> item.birthday.id }) { index, item ->
                    val shape = groupShape(index, section.items.size)
                    SwipeableBirthdayRow(
                        item = item,
                        shape = shape,
                        showDivider = index > 0,
                        onEdit = { onEdit(item.birthday.id) },
                        onDelete = { onDelete(item.birthday) },
                    )
                }
            }
        }
    }
}

private fun Section.titleRes(): Int = when (this) {
    Section.TODAY -> R.string.section_today
    Section.THIS_WEEK -> R.string.section_this_week
    Section.NEXT_WEEKS -> R.string.section_next_weeks
    Section.LATER -> R.string.section_later
}

private fun groupShape(index: Int, size: Int): Shape {
    val r = 20.dp
    return when {
        size == 1 -> RoundedCornerShape(r)
        index == 0 -> RoundedCornerShape(topStart = r, topEnd = r)
        index == size - 1 -> RoundedCornerShape(bottomStart = r, bottomEnd = r)
        else -> RoundedCornerShape(0.dp)
    }
}

@Composable
private fun TodayCard(item: UpcomingBirthday, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = colors.tertiary,
        contentColor = colors.onTertiary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(colors.onTertiary, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.birthday.name.trim().take(1).uppercase(),
                    color = colors.tertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.birthday.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.turningAge?.let { stringResource(R.string.home_turns, it) }
                        ?: stringResource(R.string.home_today_no_age),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🎂", fontSize = 28.sp)
                Text(
                    text = stringResource(R.string.home_today_badge),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBirthdayRow(
    item: UpcomingBirthday,
    shape: Shape,
    showDivider: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var handled by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!handled) {
                        handled = true
                        onDelete()
                    }
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
            ) {
                BirthdayRow(item = item, onClick = onEdit)
            }
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val deleting = direction == SwipeToDismissBoxValue.EndToStart
    val editing = direction == SwipeToDismissBoxValue.StartToEnd
    val color = when {
        deleting -> MaterialTheme.colorScheme.error
        editing -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 24.dp),
        contentAlignment = if (deleting) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (deleting) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color.White)
        } else if (editing) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit), tint = Color.White)
        }
    }
}

@Composable
private fun BirthdayRow(item: UpcomingBirthday, onClick: () -> Unit) {
    val b = item.birthday
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = b.name, seed = b.id)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = b.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                b.relation?.let {
                    Spacer(Modifier.width(6.dp))
                    RelationTag(it)
                }
            }
            val date = DateTexts.short(item.nextDate)
            Text(
                text = item.turningAge?.let { stringResource(R.string.home_row_date_age, date, it) } ?: date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Countdown(days = item.daysUntil)
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onAdd: () -> Unit, onRestore: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🎂", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_empty_add))
        }
        TextButton(onClick = onRestore) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_empty_restore))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdsIntroSheet(onContinue: () -> Unit, onPro: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onContinue, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.ads_intro_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.ads_intro_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.action_continue))
            }
            TextButton(onClick = onPro, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ads_intro_pro))
            }
        }
    }
}

/** Pide el permiso de notificaciones en contexto: tras guardar el primer cumpleaños. */
@Composable
private fun NotificationPermissionPrompt(
    hasBirthdays: Boolean,
    alreadyAsked: Boolean,
    canPost: Boolean,
    onAsked: () -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    var launched by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAsked() }
    if (!hasBirthdays || alreadyAsked || canPost || launched) return

    AlertDialog(
        onDismissRequest = onAsked,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text(stringResource(R.string.notif_permission_title)) },
        text = { Text(stringResource(R.string.notif_permission_body)) },
        confirmButton = {
            TextButton(onClick = {
                launched = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(stringResource(R.string.notif_permission_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onAsked) { Text(stringResource(R.string.action_not_now)) }
        },
    )
}
