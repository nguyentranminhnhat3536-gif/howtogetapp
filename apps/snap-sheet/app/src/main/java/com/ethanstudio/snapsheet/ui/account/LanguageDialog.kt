package com.ethanstudio.snapsheet.ui.account

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.i18n.AppLanguages

/** Tên tự xưng của từng ngôn ngữ, đúng thứ tự [AppLanguages.TAGS]. */
private val LANGUAGE_NAMES: List<Int> = listOf(
    R.string.lang_en, R.string.lang_vi, R.string.lang_es, R.string.lang_pt_br, R.string.lang_fr, R.string.lang_de,
    R.string.lang_id, R.string.lang_ru, R.string.lang_tr, R.string.lang_ja, R.string.lang_ko, R.string.lang_hi,
)

/** Tên hiển thị của mã ngôn ngữ đã chuẩn hóa; "" (theo hệ thống) hoặc mã lạ → null. */
@StringRes
fun languageNameRes(tag: String): Int? {
    val index = AppLanguages.TAGS.indexOf(tag)
    return if (index >= 0) LANGUAGE_NAMES[index] else null
}

/** Hộp chọn ngôn ngữ của app: "System default" và 12 ngôn ngữ có bản dịch. */
@Composable
fun LanguageDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                LanguageRow(stringResource(R.string.language_system), current.isEmpty()) {
                    if (current.isEmpty()) onDismiss() else onPick("")
                }
                AppLanguages.TAGS.forEachIndexed { index, tag ->
                    LanguageRow(stringResource(LANGUAGE_NAMES[index]), current == tag) {
                        if (current == tag) onDismiss() else onPick(tag)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, fontSize = 16.sp)
    }
}
