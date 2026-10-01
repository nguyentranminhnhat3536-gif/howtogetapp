package com.ethanstudio.lunartasks.ui.list

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethanstudio.lunartasks.LunarTasksApp
import com.ethanstudio.lunartasks.R
import com.ethanstudio.lunartasks.data.Repeat
import com.ethanstudio.lunartasks.data.Section
import com.ethanstudio.lunartasks.data.Task
import com.ethanstudio.lunartasks.ui.common.AppViewModels
import com.ethanstudio.lunartasks.ui.common.OverflowMenu
import com.ethanstudio.lunartasks.ui.common.formatTime
import com.ethanstudio.lunartasks.ui.common.fullDate
import com.ethanstudio.lunartasks.ui.common.lunarLong
import com.ethanstudio.lunartasks.ui.common.lunarShort
import com.ethanstudio.lunartasks.ui.common.relativeDate
import com.ethanstudio.lunartasks.ui.common.rememberHapticTap
import com.ethanstudio.lunartasks.util.dialNumber
import com.ethanstudio.lunartasks.voice.VoiceCommand
import com.ethanstudio.lunartasks.voice.VoiceCommandParser
import com.ethanstudio.lunartasks.voice.rememberVoiceInput
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onAddMedicine: () -> Unit,
    viewModel: TaskListViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.completions.collect { result ->
            val message = result.nextDue?.let { context.getString(R.string.snackbar_next_due, it.dayOfMonth, it.monthValue) }
                ?: context.getString(R.string.snackbar_done)
            val action = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (action == SnackbarResult.ActionPerformed) viewModel.undo(result)
        }
    }

    val noAppMessage = stringResource(R.string.error_no_app)
    val displaySettingsLabel = stringResource(R.string.title_display)
    val noTtsMessage = stringResource(R.string.error_no_tts)
    val app = context.applicationContext as LunarTasksApp
    val hapticTap = rememberHapticTap()
    val summary = todaySummary(state)
    val helpText = stringResource(R.string.voice_help_body)
    var showVoiceHelp by rememberSaveable { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Trả lời lệnh nói bằng cả giọng đọc lẫn dòng chữ ở cuối màn hình. */
    fun respond(message: String) {
        scope.launch {
            app.speaker.speak(message)
            snackbarHostState.showSnackbar(message)
        }
    }

    fun setting(on: Boolean, onRes: Int, offRes: Int, apply: (Boolean) -> Unit) {
        apply(on)
        respond(context.getString(if (on) onRes else offRes))
    }

    fun handleVoice(spoken: String) {
        hapticTap()
        when (val command = VoiceCommandParser.parse(spoken, LocalDateTime.now())) {
            is VoiceCommand.AddTask -> {
                viewModel.addQuickTask(command.title, command.date, command.minute)
                if (command.minute != null) requestNotificationsIfNeeded()
                respond(context.getString(R.string.voice_added, command.title, whenText(context, command.date, command.minute, state.today)))
            }
            VoiceCommand.OpenAddTask -> onAddTask()
            VoiceCommand.ReadToday -> scope.launch { if (!app.speaker.speak(summary)) snackbarHostState.showSnackbar(noTtsMessage) }
            is VoiceCommand.SetHighContrast ->
                setting(command.on ?: !state.display.highContrast, R.string.voice_contrast_on, R.string.voice_contrast_off, viewModel::setHighContrast)
            is VoiceCommand.SetSpeakReminders ->
                setting(command.on ?: !state.display.speakReminders, R.string.voice_speak_on, R.string.voice_speak_off, viewModel::setSpeakReminders)
            is VoiceCommand.SetHaptics ->
                setting(command.on ?: !state.display.haptics, R.string.voice_haptics_on, R.string.voice_haptics_off, viewModel::setHaptics)
            is VoiceCommand.ChangeTextSize -> {
                viewModel.changeTextSize(command.step)
                respond(context.getString(if (command.step > 0) R.string.voice_text_bigger else R.string.voice_text_smaller))
            }
            VoiceCommand.CallContact ->
                if (state.contactPhone.isBlank()) respond(context.getString(R.string.voice_no_contact))
                else if (!context.dialNumber(state.contactPhone)) scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
            VoiceCommand.AddMedicine -> onAddMedicine()
            VoiceCommand.OpenSettings -> onOpenDisplaySettings()
            VoiceCommand.Help -> {
                showVoiceHelp = true
                scope.launch { app.speaker.speak(helpText) }
            }
            VoiceCommand.Unknown -> respond(context.getString(R.string.voice_unknown, spoken))
        }
    }

    val voiceUnavailable = stringResource(R.string.error_no_voice)
    val listen = rememberVoiceInput(
        onResult = ::handleVoice,
        onUnavailable = { scope.launch { snackbarHostState.showSnackbar(voiceUnavailable) } },
    )
    val startListening = {
        app.speaker.stop()
        listen()
    }

    if (showVoiceHelp) {
        AlertDialog(
            onDismissRequest = { showVoiceHelp = false },
            title = { Text(stringResource(R.string.voice_help_title)) },
            text = { Text(helpText, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    showVoiceHelp = false
                    startListening()
                }) { Text(stringResource(R.string.action_voice)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVoiceHelp = false
                    app.speaker.stop()
                }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = startListening) {
                        Icon(painterResource(R.drawable.ic_mic), contentDescription = stringResource(R.string.action_voice))
                    }
                    TextButton(onClick = onOpenDisplaySettings) {
                        Text(
                            text = stringResource(R.string.action_text_size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { contentDescription = displaySettingsLabel },
                        )
                    }
                    OverflowMenu(onNoApp = { scope.launch { snackbarHostState.showSnackbar(noAppMessage) } })
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = onAddMedicine,
                    icon = { Icon(painterResource(R.drawable.ic_medication), contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_medicine)) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                ExtendedFloatingActionButton(
                    onClick = onAddTask,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.action_add_task), fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "header") {
                TodayCard(
                    state = state,
                    onReadAloud = { scope.launch { if (!app.speaker.speak(summary)) snackbarHostState.showSnackbar(noTtsMessage) } },
                    onVoice = startListening,
                    onVoiceHelp = { showVoiceHelp = true },
                )
            }
            if (state.contactPhone.isNotBlank()) {
                item(key = "call") {
                    Button(
                        onClick = { if (!context.dialNumber(state.contactPhone)) scope.launch { snackbarHostState.showSnackbar(noAppMessage) } },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_call), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_call_contact, state.contactName.ifBlank { state.contactPhone }),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            if (!state.loading && state.groups.open.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }
            state.groups.open.forEach { (section, tasks) ->
                item(key = "section-$section") { SectionHeader(sectionTitle(section), overdue = section == Section.OVERDUE) }
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        today = state.today,
                        onToggleDone = {
                            hapticTap()
                            viewModel.toggleDone(task)
                        },
                        onToggleImportant = { viewModel.toggleImportant(task) },
                        onClick = { onOpenTask(task.id) },
                    )
                }
            }
            if (state.groups.done.isNotEmpty()) {
                item(key = "done-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = viewModel::toggleShowDone)
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.section_done, state.groups.done.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painterResource(if (state.showDone) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.showDone) {
                    items(state.groups.done, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            today = state.today,
                            onToggleDone = { viewModel.toggleDone(task) },
                            onToggleImportant = { viewModel.toggleImportant(task) },
                            onClick = { onOpenTask(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(
    state: TaskListUiState,
    onReadAloud: () -> Unit,
    onVoice: () -> Unit,
    onVoiceHelp: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = fullDate(state.today),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    painterResource(R.drawable.ic_moon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.today_lunar, lunarLong(state.today)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.next_first_day, relativeDate(state.nextFirstDay, state.today)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.next_full_moon, relativeDate(state.nextFullMoon, state.today)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            // Nút nói lệnh to, nổi bật: micro chỉ bật khi người dùng bấm, ngay trong app.
            Button(
                onClick = onVoice,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(painterResource(R.drawable.ic_mic), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_voice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onReadAloud,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                ) {
                    Icon(painterResource(R.drawable.ic_volume), contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_read_today_short))
                }
                OutlinedButton(
                    onClick = onVoiceHelp,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                ) {
                    Text(stringResource(R.string.action_voice_help))
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun sectionTitle(section: Section): String = stringResource(
    when (section) {
        Section.OVERDUE -> R.string.section_overdue
        Section.TODAY -> R.string.section_today
        Section.TOMORROW -> R.string.section_tomorrow
        Section.UPCOMING -> R.string.section_upcoming
        Section.NO_DATE -> R.string.section_no_date
    },
)

@Composable
private fun SectionHeader(title: String, overdue: Boolean) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggleDone: () -> Unit,
    onToggleImportant: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val doneLabel = stringResource(R.string.cd_mark_done, task.title)
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggleDone() },
                modifier = Modifier.semantics { contentDescription = doneLabel },
            )
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TaskMeta(task, today)
            }
            IconButton(onClick = onToggleImportant) {
                Icon(
                    painterResource(if (task.important) R.drawable.ic_star else R.drawable.ic_star_border),
                    contentDescription = stringResource(
                        if (task.important) R.string.cd_unmark_important else R.string.cd_mark_important,
                    ),
                    tint = if (task.important) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TaskMeta(task: Task, today: LocalDate) {
    val due = task.dueDate ?: return
    val parts = buildList {
        add(relativeDate(due, today))
        task.dueMinute?.let { add(formatTime(it)) }
        add(lunarShort(due))
    }
    val overdue = !task.done && due.isBefore(today)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (task.isMedicine) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painterResource(R.drawable.ic_medication),
                contentDescription = stringResource(R.string.cd_medicine),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (task.repeat != Repeat.NONE) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painterResource(if (task.repeat.isLunar) R.drawable.ic_moon else R.drawable.ic_repeat),
                contentDescription = stringResource(R.string.cd_repeats),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (task.remind || task.remindDayBefore) {
            Spacer(Modifier.width(4.dp))
            Icon(
                painterResource(R.drawable.ic_notification),
                contentDescription = stringResource(R.string.cd_has_reminder),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "hôm nay lúc 08:00", "ngày mai", "05/10"... dùng trong câu xác nhận lệnh nói. */
private fun whenText(context: Context, date: LocalDate?, minute: Int?, today: LocalDate): String {
    val day = when {
        date == null -> ""
        date == today -> context.getString(R.string.date_today)
        date == today.plusDays(1) -> context.getString(R.string.date_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault()))
    }
    val time = minute?.let { context.getString(R.string.voice_at_time, formatTime(it)) }.orEmpty()
    return listOf(day, time).filter { it.isNotEmpty() }.joinToString(" ")
}

/** Câu đọc to cho thẻ Hôm nay: ngày, ngày âm, việc hôm nay và số việc quá hạn. */
@Composable
private fun todaySummary(state: TaskListUiState): String {
    val sections = state.groups.open.toMap()
    val today = sections[Section.TODAY].orEmpty()
    val overdue = sections[Section.OVERDUE].orEmpty()
    val parts = mutableListOf(stringResource(R.string.speak_today, fullDate(state.today), lunarLong(state.today)))
    if (today.isEmpty()) {
        parts += stringResource(R.string.speak_no_tasks_today)
    } else {
        val items = today.map { task ->
            task.dueMinute?.let { stringResource(R.string.speak_task_at, task.title, formatTime(it)) } ?: task.title
        }
        parts += stringResource(R.string.speak_tasks_today, today.size, items.joinToString(", "))
    }
    if (overdue.isNotEmpty()) parts += stringResource(R.string.speak_overdue, overdue.size)
    return parts.joinToString(" ")
}
