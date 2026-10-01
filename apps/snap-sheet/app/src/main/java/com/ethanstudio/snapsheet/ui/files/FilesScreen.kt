package com.ethanstudio.snapsheet.ui.files

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocSort
import com.ethanstudio.snapsheet.data.MergeCheck
import com.ethanstudio.snapsheet.data.checkMerge
import com.ethanstudio.snapsheet.data.orderedSelection
import com.ethanstudio.snapsheet.data.splitToday
import com.ethanstudio.snapsheet.ui.common.DocRow
import com.ethanstudio.snapsheet.ui.common.EmptyState
import com.ethanstudio.snapsheet.ui.common.FolderNameDialog
import com.ethanstudio.snapsheet.ui.common.MoveToFolderSheet
import com.ethanstudio.snapsheet.ui.common.ProTag
import com.ethanstudio.snapsheet.ui.common.ScreenHeader
import com.ethanstudio.snapsheet.ui.main.MainUiState
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
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
    onFolder: (Long?) -> Unit,
    onToggle: (Long) -> Unit,
    onStartSelect: () -> Unit,
    onClearSelect: () -> Unit,
    onShareSel: () -> Unit,
    onDeleteSel: () -> Unit,
    onMoveSel: (Long?) -> Unit,
    onCreateFolderAndMove: (String) -> Unit,
    onMergeSel: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (Long, String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newFolderOpen by rememberSaveable { mutableStateOf(false) }
    var renameFolderOpen by rememberSaveable { mutableStateOf(false) }
    var deleteFolderOpen by rememberSaveable { mutableStateOf(false) }
    var moveOpen by rememberSaveable { mutableStateOf(false) }
    var moveNewFolderOpen by rememberSaveable { mutableStateOf(false) }
    var mergeOpen by rememberSaveable { mutableStateOf(false) }
    var deleteOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = state.selecting) { onClearSelect() }

    val rows = RowActions(
        selecting = state.selecting,
        selection = state.selection,
        onClick = { id -> if (state.selecting) onToggle(id) else onOpenDoc(id) },
        onLongClick = { id ->
            onStartSelect()
            onToggle(id)
        },
    )

    Box(modifier.fillMaxSize()) {
        // Đang chọn: chừa chỗ cho thanh hành động (cao khoảng 180 dp) để hàng cuối cuộn lên được.
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = if (state.selecting) 190.dp else 0.dp)) {
            item {
                FilesHeader(
                    state = state,
                    onQuery = onQuery,
                    onSort = onSort,
                    onFolder = onFolder,
                    onSelect = onStartSelect,
                    onNewFolder = { newFolderOpen = true },
                    onRenameFolder = { renameFolderOpen = true },
                    onDeleteFolder = { deleteFolderOpen = true },
                )
            }
            when {
                state.allDocs.isEmpty() -> item { EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body)) }
                state.docs.isEmpty() -> item {
                    val empty = if (state.folderFilter != null && state.query.isBlank()) R.string.folder_empty else R.string.empty_search
                    Text(
                        stringResource(empty),
                        Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.sort == DocSort.NEWEST -> {
                    val (today, earlier) = splitToday(state.docs, System.currentTimeMillis())
                    docGroup(R.string.files_today, today, pageFile, pdfFile, rows)
                    docGroup(R.string.files_earlier, earlier, pageFile, pdfFile, rows)
                }
                else -> docGroup(null, state.docs, pageFile, pdfFile, rows)
            }
        }
        if (state.selecting) {
            SelectionBar(
                count = state.selection.size,
                isPro = state.pro.isPro,
                onClose = onClearSelect,
                onShare = onShareSel,
                onMove = { moveOpen = true },
                onMerge = {
                    // Bản miễn phí hoặc chưa đủ điều kiện gộp: để ViewModel mở màn mua hoặc báo lý do.
                    val ok = state.pro.isPro && checkMerge(orderedSelection(state.selection, state.allDocs)) is MergeCheck.Ok
                    if (ok) mergeOpen = true else onMergeSel()
                },
                onDelete = { deleteOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    val currentFolder = state.folders.firstOrNull { it.id == state.folderFilter }
    if (newFolderOpen) {
        FolderNameDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.folder_create),
            initial = "",
            folders = state.folders,
            editingId = null,
            onDismiss = { newFolderOpen = false },
            onConfirm = {
                newFolderOpen = false
                onCreateFolder(it)
            },
        )
    }
    if (renameFolderOpen && currentFolder != null) {
        FolderNameDialog(
            title = stringResource(R.string.folder_rename_title),
            confirmLabel = stringResource(R.string.save),
            initial = currentFolder.name,
            folders = state.folders,
            editingId = currentFolder.id,
            onDismiss = { renameFolderOpen = false },
            onConfirm = {
                renameFolderOpen = false
                onRenameFolder(currentFolder.id, it)
            },
        )
    }
    if (deleteFolderOpen && currentFolder != null) {
        AlertDialog(
            onDismissRequest = { deleteFolderOpen = false },
            title = { Text(stringResource(R.string.folder_delete_title)) },
            text = { Text(stringResource(R.string.folder_delete_body)) },
            confirmButton = {
                TextButton({
                    deleteFolderOpen = false
                    onDeleteFolder(currentFolder.id)
                }) {
                    Text(stringResource(R.string.doc_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton({ deleteFolderOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (moveOpen && state.selecting) {
        MoveToFolderSheet(
            folders = state.folders,
            currentFolderId = null,
            onPick = {
                moveOpen = false
                onMoveSel(it)
            },
            onNewFolder = {
                moveOpen = false
                moveNewFolderOpen = true
            },
            onDismiss = { moveOpen = false },
        )
    }
    if (moveNewFolderOpen && state.selecting) {
        FolderNameDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.folder_create),
            initial = "",
            folders = state.folders,
            editingId = null,
            onDismiss = { moveNewFolderOpen = false },
            onConfirm = {
                moveNewFolderOpen = false
                onCreateFolderAndMove(it)
            },
        )
    }
    if (mergeOpen && state.selecting) {
        val selected = orderedSelection(state.selection, state.allDocs)
        val check = checkMerge(selected)
        if (check is MergeCheck.Ok) {
            MergeDialog(
                docCount = selected.size,
                pageCount = check.total,
                onDismiss = { mergeOpen = false },
                onConfirm = {
                    mergeOpen = false
                    onMergeSel()
                },
            )
        }
    }
    if (deleteOpen && state.selecting) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text(stringResource(R.string.delete_many_title)) },
            text = { Text(stringResource(R.string.delete_many_body)) },
            confirmButton = {
                TextButton({
                    deleteOpen = false
                    onDeleteSel()
                }) {
                    Text(stringResource(R.string.doc_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton({ deleteOpen = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Cách mỗi hàng tài liệu phản ứng: mở tài liệu, hoặc chọn/bỏ chọn khi đang ở chế độ chọn. */
private class RowActions(
    val selecting: Boolean,
    val selection: List<Long>,
    val onClick: (Long) -> Unit,
    val onLongClick: (Long) -> Unit,
)

/** Đầu trang: tiêu đề, ô tìm kiếm, hai nút sắp xếp + nút Chọn, hàng chip thư mục. */
@Composable
private fun FilesHeader(
    state: MainUiState,
    onQuery: (String) -> Unit,
    onSort: (DocSort) -> Unit,
    onFolder: (Long?) -> Unit,
    onSelect: () -> Unit,
    onNewFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SortChip(R.string.sort_newest, state.sort == DocSort.NEWEST) { onSort(DocSort.NEWEST) }
            SortChip(R.string.sort_name, state.sort == DocSort.NAME) { onSort(DocSort.NAME) }
            Spacer(Modifier.weight(1f))
            if (state.docs.isNotEmpty() && !state.selecting) {
                TextButton(onClick = onSelect, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.select), color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderChip(stringResource(R.string.folder_all), state.folderFilter == null, showIcon = false) { onFolder(null) }
            state.folders.forEach { folder ->
                FolderChip(folder.name, state.folderFilter == folder.id, showIcon = true) { onFolder(folder.id) }
            }
            AssistChip(
                onClick = onNewFolder,
                label = { Text(stringResource(R.string.folder_new)) },
                modifier = Modifier.heightIn(min = 40.dp),
                leadingIcon = { Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(18.dp), tint = Accent) },
                colors = AssistChipDefaults.assistChipColors(containerColor = colors.surface),
            )
        }
        if (state.folderFilter != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRenameFolder, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.doc_rename), color = Accent)
                }
                TextButton(onClick = onDeleteFolder, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.doc_delete), color = colors.error)
                }
            }
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
        colors = chipColors(),
    )
}

/** Chip lọc theo thư mục ("All" hoặc một thư mục); tên dài thì cắt bằng dấu "…". */
@Composable
private fun FolderChip(label: String, selected: Boolean, showIcon: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.heightIn(min = 40.dp).widthIn(max = 160.dp),
        leadingIcon = if (showIcon) {
            { Icon(painterResource(R.drawable.ic_folder), null, Modifier.size(18.dp), tint = if (selected) Accent else NavMuted) }
        } else {
            null
        },
        colors = chipColors(),
    )
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    selectedContainerColor = PrimaryTint,
    selectedLabelColor = Accent,
)

/** Một nhóm tài liệu kèm nhãn ("TODAY", "EARLIER"). Nhóm rỗng thì không hiện gì. */
private fun LazyListScope.docGroup(
    label: Int?,
    docs: List<Doc>,
    pageFile: (Long) -> File,
    pdfFile: (Long) -> File,
    rows: RowActions,
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
        val order = rows.selection.indexOf(doc.id)
        DocRow(
            doc,
            pageFile(doc.id),
            docSubtitle(doc, pdfFile(doc.id)),
            onClick = { rows.onClick(doc.id) },
            onLongClick = { rows.onLongClick(doc.id) },
            selectMode = rows.selecting,
            selectedOrder = if (order >= 0) order + 1 else null,
        )
    }
}

/** Dòng phụ: số trang và dung lượng PDF (đọc ngoài luồng chính). Không đọc được dung lượng thì chỉ hiện số trang. */
@Composable
private fun docSubtitle(doc: Doc, pdf: File): String {
    val context = LocalContext.current
    val pages = pluralStringResource(R.plurals.pages, doc.pageCount, doc.pageCount)
    val size by produceState(0L, pdf, doc.pageCount) {
        value = withContext(Dispatchers.IO) { runCatching { pdf.length() }.getOrDefault(0L) }
    }
    val sizeText = if (size > 0) Formatter.formatShortFileSize(context, size) else null
    return if (sizeText != null) stringResource(R.string.join_dot, pages, sizeText) else pages
}

/** Thanh hành động khi đang chọn nhiều: số đã chọn, nút đóng, rồi Chia sẻ / Chuyển / Gộp / Xóa. */
@Composable
private fun SelectionBar(
    count: Int,
    isPro: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val enabled = count > 0
    Surface(
        modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.selected_count, count, count),
                    Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close), tint = colors.onSurface)
                }
            }
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BarAction(R.drawable.ic_share, stringResource(R.string.share_chooser), enabled, false, onShare, Modifier.weight(1f))
                BarAction(R.drawable.ic_folder, stringResource(R.string.sel_move), enabled, false, onMove, Modifier.weight(1f))
                BarAction(R.drawable.ic_merge, stringResource(R.string.sel_merge), enabled, !isPro, onMerge, Modifier.weight(1f))
                BarAction(R.drawable.ic_trash, stringResource(R.string.doc_delete), enabled, false, onDelete, Modifier.weight(1f))
            }
        }
    }
}

/** Nút tròn 48 dp kèm nhãn (tối đa 2 dòng) trong thanh chọn nhiều; tắt thì mờ và không bấm được. */
@Composable
private fun BarAction(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean,
    pro: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(48.dp).background(Gradients.soft(), CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(24.dp), tint = Accent)
            }
            Text(
                label,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (pro) ProTag(Modifier.align(Alignment.TopEnd))
    }
}

/** Hỏi lại trước khi gộp: nói rõ thứ tự gộp, số tài liệu và tổng số trang. */
@Composable
private fun MergeDialog(docCount: Int, pageCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val docs = pluralStringResource(R.plurals.docs_count, docCount, docCount)
    val pages = pluralStringResource(R.plurals.pages, pageCount, pageCount)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.merge_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.merge_body))
                Text(stringResource(R.string.join_dot, docs, pages), fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { TextButton(onConfirm) { Text(stringResource(R.string.sel_merge)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
