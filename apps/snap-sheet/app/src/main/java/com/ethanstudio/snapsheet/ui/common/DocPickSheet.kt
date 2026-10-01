package com.ethanstudio.snapsheet.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.ui.theme.Gradients
import java.io.File

/** Bảng chọn một tài liệu cho công cụ mở từ tab Công cụ (Ký tên, Chèn chữ mờ). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocPickSheet(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    docs: List<Doc>,
    pageFile: (Long) -> File,
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
            item { DocPickHeader(title, subtitle, icon, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)) }
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
        }
    }
}

@Composable
private fun DocPickHeader(title: String, subtitle: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(52.dp).background(Gradients.Primary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(26.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(subtitle, fontSize = 14.sp, lineHeight = 19.sp, color = colors.onSurfaceVariant)
        }
    }
}
