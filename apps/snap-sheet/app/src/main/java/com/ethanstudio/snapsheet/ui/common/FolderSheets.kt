package com.ethanstudio.snapsheet.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.FOLDER_NAME_MAX
import com.ethanstudio.snapsheet.data.Folder
import com.ethanstudio.snapsheet.data.FolderNameCheck
import com.ethanstudio.snapsheet.data.checkFolderName
import com.ethanstudio.snapsheet.ui.theme.Accent

/**
 * Bảng chọn thư mục để chuyển tài liệu vào: "Không thuộc thư mục nào", các thư mục, rồi "Thư mục mới".
 * [currentFolderId]: thư mục hiện tại (có dấu tích); từ màn chọn nhiều thì truyền null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(
    folders: List<Folder>,
    currentFolderId: Long?,
    onPick: (Long?) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
    ) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(bottom = 20.dp)) {
            item {
                Text(
                    stringResource(R.string.doc_move),
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
            }
            item(key = "none") { FolderOption(R.drawable.ic_doc, stringResource(R.string.move_none), false) { onPick(null) } }
            items(folders, key = { it.id }) { folder ->
                FolderOption(R.drawable.ic_folder, folder.name, folder.id == currentFolderId) { onPick(folder.id) }
            }
            item(key = "new") { FolderOption(R.drawable.ic_plus, stringResource(R.string.folder_new), false, onNewFolder) }
        }
    }
}

@Composable
private fun FolderOption(@DrawableRes icon: Int, label: String, current: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = Accent)
        Text(label, Modifier.weight(1f), fontSize = 16.sp, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (current) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(22.dp), tint = Accent)
    }
}

/**
 * Hộp thoại nhập tên thư mục (tạo mới hoặc đổi tên). Nút xác nhận chỉ bật khi tên hợp lệ và chưa trùng.
 * [editingId]: id thư mục đang đổi tên (được phép giữ nguyên tên của chính nó); tạo mới thì null.
 */
@Composable
fun FolderNameDialog(
    title: String,
    confirmLabel: String,
    initial: String,
    folders: List<Folder>,
    editingId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    val check = checkFolderName(value, folders, editingId)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value,
                { value = it.take(FOLDER_NAME_MAX) },
                label = { Text(stringResource(R.string.folder_name_label)) },
                singleLine = true,
                isError = check == FolderNameCheck.Taken,
                supportingText = if (check == FolderNameCheck.Taken) {
                    { Text(stringResource(R.string.folder_name_taken)) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton({ (check as? FolderNameCheck.Ok)?.let { onConfirm(it.name) } }, enabled = check is FolderNameCheck.Ok) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
