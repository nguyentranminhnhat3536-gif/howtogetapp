package com.ethanstudio.snapsheet.ui.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.safeWebUrl
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.util.openUrl
import com.ethanstudio.snapsheet.util.shareText

/**
 * Bảng kết quả quét mã: hiện nguyên nội dung mã (chọn được để sao chép), nút sao chép và chia sẻ.
 * Chỉ link http/https mới có nút "Mở liên kết", và địa chỉ luôn hiện ra trước khi mở. Kết quả không được lưu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrResultSheet(raw: String, onScanAgain: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val url = remember(raw) { safeWebUrl(raw) }
    val chooser = stringResource(R.string.share_chooser)

    fun toast(@StringRes res: Int) {
        Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QrHeader()
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                SelectionContainer(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()).padding(14.dp)) {
                    Text(raw, fontSize = 16.sp, lineHeight = 22.sp, color = colors.onSurface)
                }
            }
            if (url != null) {
                Text(stringResource(R.string.qr_link_hint), fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
                GradientButton(
                    stringResource(R.string.qr_open_link),
                    onClick = { if (!context.openUrl(url)) toast(R.string.error_no_app) },
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 52.dp,
                    fontSize = 16.sp,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedPillButton(R.drawable.ic_copy, stringResource(R.string.copy), Modifier.weight(1f)) {
                    val ok = try {
                        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: error("no clipboard")
                        clipboard.setPrimaryClip(ClipData.newPlainText("text", raw))
                        true
                    } catch (e: RuntimeException) {
                        false
                    }
                    // Android 13+ tự hiện thông báo đã sao chép.
                    if (!ok) toast(R.string.err_unknown) else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(R.string.copied)
                }
                OutlinedPillButton(R.drawable.ic_share, stringResource(R.string.share_text), Modifier.weight(1f)) {
                    val ok = try {
                        context.shareText(raw, chooser)
                    } catch (e: RuntimeException) {
                        false
                    }
                    if (!ok) toast(R.string.error_no_app)
                }
            }
            TextButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.ocr_scan_again), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Accent)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.close), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QrHeader() {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(52.dp).background(Gradients.Primary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_qr), null, Modifier.size(26.dp), tint = Color.White)
        }
        Text(
            stringResource(R.string.qr_title),
            Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Nút viền bo tròn: icon xanh và chữ. Dùng ở bảng kết quả quét mã và màn Ký tên. */
@Composable
internal fun OutlinedPillButton(@DrawableRes icon: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = Accent)
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent, textAlign = TextAlign.Center)
    }
}
