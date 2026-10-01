package com.ethanstudio.snapsheet.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients

/**
 * Bảng chọn nguồn trang: quét bằng camera hoặc nhập ảnh. Dùng cho nút + (tài liệu mới)
 * và nút "Thêm trang" trong màn tài liệu (chỉ khác tiêu đề và dòng mô tả).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanChoiceSheet(
    title: String,
    cameraSub: String,
    photosSub: String,
    onCamera: () -> Unit,
    onPhotos: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            SheetCard(
                icon = R.drawable.ic_scan,
                title = stringResource(R.string.sheet_camera),
                sub = cameraSub,
                primary = true,
                onClick = onCamera,
            )
            SheetCard(
                icon = R.drawable.ic_photo,
                title = stringResource(R.string.sheet_photos),
                sub = photosSub,
                primary = false,
                onClick = onPhotos,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Một lựa chọn trong bảng. [primary]: thẻ nền gradient xanh chữ trắng; không thì thẻ trắng viền mảnh. */
@Composable
fun SheetCard(icon: Int, title: String, sub: String, primary: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val base = Modifier.fillMaxWidth().clip(shape)
    val background = if (primary) base.background(Gradients.Primary) else base.background(colors.surface).border(1.dp, colors.outlineVariant, shape)
    Row(
        background.clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val iconBox = Modifier.size(50.dp)
        Box(
            if (primary) iconBox.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)) else iconBox.background(Gradients.soft(), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, Modifier.size(26.dp), tint = if (primary) Color.White else Accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (primary) Color.White else colors.onSurface,
            )
            Text(
                sub,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = if (primary) Color.White.copy(alpha = 0.9f) else colors.onSurfaceVariant,
            )
        }
    }
}
