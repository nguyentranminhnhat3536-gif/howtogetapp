package com.ethanstudio.lunartasks.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.ethanstudio.lunartasks.util.PickPhoneNumber
import com.ethanstudio.lunartasks.util.readPickedPhone
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.ethanstudio.lunartasks.data.AppSettings
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethanstudio.lunartasks.R
import com.ethanstudio.lunartasks.ui.common.AppViewModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    onClose: () -> Unit,
    viewModel: DisplaySettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_display)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextSizeRow(scale = state.textScale, onChange = viewModel::changeTextSize)
            ToggleRow(
                title = stringResource(R.string.setting_high_contrast),
                body = stringResource(R.string.setting_high_contrast_body),
                checked = state.highContrast,
                onCheckedChange = viewModel::setHighContrast,
            )
            ToggleRow(
                title = stringResource(R.string.setting_speak_reminders),
                body = stringResource(R.string.setting_speak_reminders_body),
                checked = state.speakReminders,
                onCheckedChange = viewModel::setSpeakReminders,
            )
            ToggleRow(
                title = stringResource(R.string.setting_haptics),
                body = stringResource(R.string.setting_haptics_body),
                checked = state.haptics,
                onCheckedChange = viewModel::setHaptics,
            )
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.preview_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.preview_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            ContactSection(
                name = state.contactName,
                phone = state.contactPhone,
                onSave = viewModel::setContact,
            )
            Text(
                text = stringResource(R.string.setting_talkback_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Cả hàng bấm được (vùng chạm lớn), TalkBack đọc như một công tắc. */
@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(body) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun ContactSection(name: String, phone: String, onSave: (String, String) -> Unit) {
    val context = LocalContext.current
    var showManual by rememberSaveable { mutableStateOf(false) }
    val pickLauncher = rememberLauncherForActivityResult(PickPhoneNumber()) { uri ->
        uri?.let { context.readPickedPhone(it) }?.let { (pickedName, pickedPhone) -> onSave(pickedName, pickedPhone) }
    }

    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.contact_section_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (phone.isBlank()) stringResource(R.string.contact_none) else stringResource(R.string.contact_current, name.ifBlank { phone }, phone),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                try {
                    pickLauncher.launch(Unit)
                } catch (e: ActivityNotFoundException) {
                    showManual = true
                }
            }) { Text(stringResource(R.string.contact_pick)) }
            OutlinedButton(onClick = { showManual = true }) { Text(stringResource(R.string.contact_manual)) }
        }
        if (phone.isNotBlank()) {
            TextButton(onClick = { onSave("", "") }) { Text(stringResource(R.string.contact_remove)) }
        }
    }

    if (showManual) {
        var nameText by rememberSaveable { mutableStateOf(name) }
        var phoneText by rememberSaveable { mutableStateOf(phone) }
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text(stringResource(R.string.contact_section_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text(stringResource(R.string.contact_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { value -> phoneText = value.filter { it.isDigit() || it in "+ " } },
                        label = { Text(stringResource(R.string.contact_phone)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = phoneText.any(Char::isDigit), onClick = {
                    onSave(nameText, phoneText)
                    showManual = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showManual = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Hai nút A− / A+ to, ở giữa là mức hiện tại (phần trăm). */
@Composable
private fun TextSizeRow(scale: Float, onChange: (Int) -> Unit) {
    val percent = (scale * 100).roundToInt()
    val smallerLabel = stringResource(R.string.cd_text_smaller)
    val biggerLabel = stringResource(R.string.cd_text_bigger)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.setting_text_size), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = { onChange(-1) },
                enabled = scale > AppSettings.TEXT_SCALES.first(),
                modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 56.dp).semantics { contentDescription = smallerLabel },
            ) {
                Text(stringResource(R.string.text_size_smaller_symbol), style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { })
            }
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            OutlinedButton(
                onClick = { onChange(+1) },
                enabled = scale < AppSettings.TEXT_SCALES.last(),
                modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 56.dp).semantics { contentDescription = biggerLabel },
            ) {
                Text(stringResource(R.string.text_size_bigger_symbol), style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }
}
