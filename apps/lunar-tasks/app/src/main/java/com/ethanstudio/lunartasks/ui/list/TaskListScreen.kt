package com.ethanstudio.lunartasks.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenDisplaySettings: () -> Unit,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
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
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_task)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "header") { TodayCard(state) }
            if (!state.loading && state.groups.open.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }
            state.groups.open.forEach { (section, tasks) ->
                item(key = "section-$section") { SectionHeader(sectionTitle(section), overdue = section == Section.OVERDUE) }
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        today = state.today,
                        onToggleDone = { viewModel.toggleDone(task) },
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
private fun TodayCard(state: TaskListUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = fullDate(state.today),
                style = MaterialTheme.typography.titleMedium,
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
                    tint = if (task.important) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
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
        if (task.repeat != Repeat.NONE) {
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
