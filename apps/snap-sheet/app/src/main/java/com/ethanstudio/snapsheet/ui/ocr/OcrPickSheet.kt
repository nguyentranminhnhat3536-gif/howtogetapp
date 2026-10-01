package com.ethanstudio.snapsheet.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.ui.common.DocRow
import com.ethanstudio.snapsheet.ui.common.docDate
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import java.io.File

/** Bảng chọn tài liệu để lấy chữ (mở từ Tools › Extract text), kèm lối quét trang mới. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrPickSheet(
    docs: List<Doc>,
    pageFile: (Long) -> File,
    onScanNew: () -> Unit,
    onPick: (Long) -> Unit,
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
            item { PickHeader(Modifier.padding(horizontal = 20.dp)) }
            item { ScanNewCard(onScanNew, Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) }
            item {
                Text(
                    stringResource(R.string.ocr_pick_doc),
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.04.em,
                    color = colors.onSurfaceVariant,
                )
            }
            if (docs.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.ocr_no_docs),
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        fontSize = 15.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            items(docs, key = { it.id }) { doc ->
                val pages = pluralStringResource(R.plurals.pages, doc.pageCount, doc.pageCount)
                DocRow(doc, pageFile(doc.id), stringResource(R.string.join_dot, docDate(doc), pages), onClick = { onPick(doc.id) })
            }
            item { OnDeviceChip(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) }
        }
    }
}

@Composable
private fun PickHeader(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(52.dp).background(Gradients.Primary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_text), null, Modifier.size(26.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.doc_extract), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(stringResource(R.string.ocr_pick_sub), fontSize = 14.sp, lineHeight = 19.sp, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScanNewCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(Color(0xFFF7FAFF))
            .border(1.5.dp, Color(0xFF9EBDF2), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_scan), null, Modifier.size(24.dp), tint = Accent)
        Text(stringResource(R.string.ocr_scan_new), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Accent)
    }
}

/** Dòng nhỏ "chạy trên điện thoại, không tải gì lên", dùng ở bảng chọn và màn đang đọc. */
@Composable
internal fun OnDeviceChip(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Gradients.soft()).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_shield), null, Modifier.size(16.dp), tint = Accent)
        Text(stringResource(R.string.ocr_on_device), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
