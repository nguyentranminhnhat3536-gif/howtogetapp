package com.ethanstudio.snapsheet.ui.files

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocSort
import com.ethanstudio.snapsheet.data.splitToday
import com.ethanstudio.snapsheet.ui.common.DocRow
import com.ethanstudio.snapsheet.ui.common.EmptyState
import com.ethanstudio.snapsheet.ui.common.ScreenHeader
import com.ethanstudio.snapsheet.ui.main.MainUiState
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.NavMuted
import com.ethanstudio.snapsheet.ui.theme.PrimaryTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FilesScreen(
    state: MainUiState,
    onQuery: (String) -> Unit,
    onSort: (DocSort) -> Unit,
    pageFile: (Long) -> File,
    pdfFile: (Long) -> File,
    onOpenDoc: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        item { FilesHeader(state, onQuery, onSort) }
        when {
            state.allDocs.isEmpty() -> item { EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body)) }
            state.docs.isEmpty() -> item {
                Text(
                    stringResource(R.string.empty_search),
                    Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.sort == DocSort.NEWEST -> {
                val (today, earlier) = splitToday(state.docs, System.currentTimeMillis())
                docGroup(R.string.files_today, today, pageFile, pdfFile, onOpenDoc)
                docGroup(R.string.files_earlier, earlier, pageFile, pdfFile, onOpenDoc)
            }
            else -> docGroup(null, state.docs, pageFile, pdfFile, onOpenDoc)
        }
    }
}

/** Đầu trang: tiêu đề, ô tìm kiếm và hai nút sắp xếp. */
@Composable
private fun FilesHeader(state: MainUiState, onQuery: (String) -> Unit, onSort: (DocSort) -> Unit) {
    val colors = MaterialTheme.colorScheme
    ScreenHeader {
        Text(stringResource(R.string.files_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.files_search_hint)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, tint = NavMuted) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = Accent,
                unfocusedBorderColor = colors.outlineVariant,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(R.string.sort_newest, state.sort == DocSort.NEWEST) { onSort(DocSort.NEWEST) }
            SortChip(R.string.sort_name, state.sort == DocSort.NAME) { onSort(DocSort.NAME) }
        }
    }
}

@Composable
private fun SortChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(label)) },
        modifier = Modifier.heightIn(min = 40.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = PrimaryTint,
            selectedLabelColor = Accent,
        ),
    )
}

/** Một nhóm tài liệu kèm nhãn ("TODAY", "EARLIER"). Nhóm rỗng thì không hiện gì. */
private fun LazyListScope.docGroup(
    label: Int?,
    docs: List<Doc>,
    pageFile: (Long) -> File,
    pdfFile: (Long) -> File,
    onOpenDoc: (Long) -> Unit,
) {
    if (docs.isEmpty()) return
    if (label != null) {
        item(key = "label_$label") {
            Text(
                stringResource(label),
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04.em,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(docs, key = { it.id }) { doc ->
        DocRow(doc, pageFile(doc.id), docSubtitle(doc, pdfFile(doc.id)), onClick = { onOpenDoc(doc.id) })
    }
}

/** Dòng phụ: số trang và dung lượng PDF (đọc ngoài luồng chính). Không đọc được dung lượng thì chỉ hiện số trang. */
@Composable
private fun docSubtitle(doc: Doc, pdf: File): String {
    val context = LocalContext.current
    val pages = pluralStringResource(R.plurals.pages, doc.pageCount, doc.pageCount)
    val size by produceState(0L, pdf) {
        value = withContext(Dispatchers.IO) { runCatching { pdf.length() }.getOrDefault(0L) }
    }
    val sizeText = if (size > 0) Formatter.formatShortFileSize(context, size) else null
    return if (sizeText != null) stringResource(R.string.join_dot, pages, sizeText) else pages
}
