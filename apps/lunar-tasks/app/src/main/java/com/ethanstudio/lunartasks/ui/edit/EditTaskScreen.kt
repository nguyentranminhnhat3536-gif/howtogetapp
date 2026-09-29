package com.ethanstudio.lunartasks.ui.edit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethanstudio.lunartasks.R
import com.ethanstudio.lunartasks.data.Repeat
import com.ethanstudio.lunartasks.ui.common.AppViewModels
import com.ethanstudio.lunartasks.ui.common.formatTime
import com.ethanstudio.lunartasks.ui.common.fullDate
import com.ethanstudio.lunartasks.ui.common.lunarLong
import com.ethanstudio.lunartasks.ui.common.repeatLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    onClose: () -> Unit,
    viewModel: EditTaskViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.finished) { if (state.finished) onClose() }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showLunarPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var notificationsBlocked by remember { mutableStateOf(!NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsBlocked = !granted
    }

    fun onRemindChange(remind: Boolean) {
        viewModel.setRemind(remind)
        if (remind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.title_new_task else R.string.title_edit_task)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text(stringResource(R.string.field_title)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.field_note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            val date = state.date
            SettingRow(
                icon = R.drawable.ic_calendar,
                headline = date?.let { fullDate(it) } ?: stringResource(R.string.field_no_date),
                supporting = date?.let { stringResource(R.string.lunar_prefix, lunarLong(it)) },
                onClick = { showDatePicker = true },
                onClear = if (date != null) viewModel::clearDate else null,
            )
            SettingRow(
                icon = R.drawable.ic_schedule,
                headline = state.minute?.let { formatTime(it) } ?: stringResource(R.string.field_no_time),
                supporting = null,
                onClick = { showTimePicker = true },
                onClear = if (state.minute != null) viewModel::clearTime else null,
            )
            OutlinedButton(onClick = { showLunarPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.ic_moon), contentDescription = null)
                Text(stringResource(R.string.action_pick_lunar_date), modifier = Modifier.padding(start = 8.dp))
            }

            RepeatRow(repeat = state.repeat, onSelect = viewModel::setRepeat)
            if (state.repeat == Repeat.LUNAR_MONTHLY) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 16.dp)) {
                    FilterChip(
                        selected = state.pickedLunarDay == 1,
                        onClick = { viewModel.pickLunarDayOfMonth(1) },
                        label = { Text(stringResource(R.string.chip_first_day)) },
                    )
                    FilterChip(
                        selected = state.pickedLunarDay == 15,
                        onClick = { viewModel.pickLunarDayOfMonth(15) },
                        label = { Text(stringResource(R.string.chip_full_moon)) },
                    )
                }
            }

            HorizontalDivider()
            SwitchRow(
                icon = R.drawable.ic_notification,
                headline = stringResource(R.string.field_remind),
                supporting = when {
                    state.remind && notificationsBlocked -> stringResource(R.string.remind_blocked)
                    state.remind -> stringResource(R.string.remind_at, formatTime(state.minute ?: 0))
                    else -> null
                },
                checked = state.remind,
                onCheckedChange = ::onRemindChange,
            )
            SwitchRow(
                icon = R.drawable.ic_star_border,
                headline = stringResource(R.string.field_important),
                supporting = null,
                checked = state.important,
                onCheckedChange = viewModel::setImportant,
            )

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = (state.date ?: LocalDate.now()).toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.setDate(it.utcMillisToDate()) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val initial = state.minute ?: (9 * 60)
        val pickerState = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(pickerState.hour * 60 + pickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = { TimePicker(state = pickerState) },
        )
    }

    if (showLunarPicker) {
        LunarDateDialog(
            onDismiss = { showLunarPicker = false },
            onConfirm = { day, month ->
                viewModel.pickLunarDate(day, month)
                showLunarPicker = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SettingRow(
    icon: Int,
    headline: String,
    supporting: String?,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = onClear?.let {
            {
                IconButton(onClick = it) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.cd_clear))
                }
            }
        },
    )
}

@Composable
private fun SwitchRow(
    icon: Int,
    headline: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        headlineContent = { Text(headline) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun RepeatRow(repeat: Repeat, onSelect: (Repeat) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ListItem(
            modifier = Modifier.clickable { open = true },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            leadingContent = { Icon(painterResource(R.drawable.ic_repeat), contentDescription = null) },
            headlineContent = { Text(repeatLabel(repeat)) },
            supportingContent = { Text(stringResource(R.string.field_repeat)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Repeat.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(repeatLabel(option)) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LunarDateDialog(onDismiss: () -> Unit, onConfirm: (day: Int, month: Int) -> Unit) {
    var dayText by rememberSaveable { mutableStateOf("") }
    var monthText by rememberSaveable { mutableStateOf("") }
    val day = dayText.toIntOrNull()
    val month = monthText.toIntOrNull()
    val valid = day != null && day in 1..30 && month != null && month in 1..12

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lunar_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.lunar_dialog_body), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.lunar_day)) },
                        singleLine = true,
                        isError = dayText.isNotEmpty() && (day == null || day !in 1..30),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = monthText,
                        onValueChange = { monthText = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.lunar_month)) },
                        singleLine = true,
                        isError = monthText.isNotEmpty() && (month == null || month !in 1..12),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(day!!, month!!) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
