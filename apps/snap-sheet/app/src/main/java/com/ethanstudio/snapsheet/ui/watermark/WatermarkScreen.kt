package com.ethanstudio.snapsheet.ui.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.WatermarkColor
import com.ethanstudio.snapsheet.data.WatermarkSpec
import com.ethanstudio.snapsheet.data.WatermarkStrength
import com.ethanstudio.snapsheet.data.drawWatermark
import com.ethanstudio.snapsheet.data.fitInside
import com.ethanstudio.snapsheet.ui.common.BusyOverlay
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.PrimaryTint
import com.ethanstudio.snapsheet.util.decodeSampledCached
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Cạnh dài (điểm ảnh) của ảnh xem trước. */
private const val PREVIEW_PX = 900

/** Chờ người dùng gõ xong một chút rồi mới vẽ lại ảnh xem trước. */
private const val PREVIEW_DELAY_MS = 150L

/** Cao/rộng khổ A4 dọc, dùng khi chưa đọc được ảnh. */
private const val PREVIEW_DEFAULT_ASPECT = 1.414f

/** Màn Chèn chữ mờ: xem trước trang đầu, nhập chữ, chọn màu và độ đậm, rồi lưu thành bản sao. */
@Composable
fun WatermarkScreen(viewModel: WatermarkViewModel, onBack: () -> Unit, onDone: (Long) -> Unit, onNeedPro: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme
    val nameTemplate = stringResource(R.string.wm_copy_name)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WatermarkEvent.Done -> {
                    Toast.makeText(context, context.getString(R.string.wm_done), Toast.LENGTH_SHORT).show()
                    onDone(event.id)
                }
                WatermarkEvent.NeedPro -> {
                    Toast.makeText(context, context.getString(R.string.watermark_pro_only), Toast.LENGTH_LONG).show()
                    onNeedPro()
                }
                is WatermarkEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
            }
        }
    }

    // Tài liệu không còn (đã bị xóa ở nơi khác): quay lại.
    LaunchedEffect(state.loaded, state.doc == null) {
        if (state.loaded && state.doc == null) onBack()
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { WatermarkTopBar(onBack) },
            bottomBar = {
                Box(Modifier.fillMaxWidth().background(colors.background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    GradientButton(
                        stringResource(R.string.wm_save),
                        onClick = { viewModel.save(nameTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSave,
                        busy = state.saving,
                    )
                }
            },
        ) { padding ->
            val doc = state.doc
            if (doc == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WatermarkPreview(viewModel.firstPage(doc), state.spec)
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = viewModel::setText,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.wm_text_label)) },
                        singleLine = true,
                    )
                    Suggestions(onPick = viewModel::setText)
                    Text(stringResource(R.string.wm_color), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WatermarkColor.entries.forEach { color ->
                            ColorSwatch(color, selected = color == state.color, label = stringResource(colorLabel(color))) { viewModel.setColor(color) }
                        }
                    }
                    Text(stringResource(R.string.wm_strength), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WatermarkStrength.entries.forEach { strength ->
                            FilterChip(
                                selected = strength == state.strength,
                                onClick = { viewModel.setStrength(strength) },
                                label = { Text(stringResource(strengthLabel(strength))) },
                                modifier = Modifier.heightIn(min = 40.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = colors.surface,
                                    selectedContainerColor = PrimaryTint,
                                    selectedLabelColor = Accent,
                                ),
                            )
                        }
                    }
                    Text(stringResource(R.string.wm_note), fontSize = 13.5.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
                }
            }
        }
        if (state.saving) BusyOverlay(stringResource(R.string.saving))
    }
}

/** Thanh trên: nút quay lại và tiêu đề "Chèn chữ mờ". */
@Composable
private fun WatermarkTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(colors.background).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.doc_back), tint = colors.onSurface)
        }
        Text(
            stringResource(R.string.tool_watermark_title),
            Modifier.weight(1f).padding(horizontal = 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Ảnh xem trang đầu kèm chữ mờ, vẽ bằng đúng hàm của bản lưu ([drawWatermark]).
 * Vẽ lỗi (ví dụ hết bộ nhớ) thì hiện trang không có chữ mờ.
 */
@Composable
private fun WatermarkPreview(file: File, spec: WatermarkSpec?) {
    val colors = MaterialTheme.colorScheme
    val base by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            try {
                decodeSampledCached(file, PREVIEW_PX)
            } catch (e: OutOfMemoryError) {
                null
            }
        }
    }
    val shown by produceState<ImageBitmap?>(null, base, spec) {
        val source = base ?: return@produceState
        if (spec == null) {
            value = source.asImageBitmap()
            return@produceState
        }
        delay(PREVIEW_DELAY_MS)
        value = try {
            withContext(Dispatchers.Default) {
                // Ảnh trong bộ nhớ đệm dùng chung nên vẽ lên bản chép, không vẽ lên ảnh gốc.
                val copy = source.copy(Bitmap.Config.ARGB_8888, true)
                if (copy == null) {
                    source.asImageBitmap()
                } else {
                    drawWatermark(Canvas(copy), copy.width, copy.height, spec)
                    copy.asImageBitmap()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            source.asImageBitmap()
        }
    }
    val aspect = base?.let { it.height.toFloat() / it.width } ?: PREVIEW_DEFAULT_ASPECT
    val description = stringResource(R.string.wm_preview)
    BoxWithConstraints(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
        val (w, h) = fitInside(maxWidth.value, maxHeight.value, aspect)
        val shape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .size(w.dp, h.dp)
                .clip(shape)
                .background(Color.White)
                .border(1.dp, colors.outlineVariant, shape)
                .semantics { contentDescription = description },
        ) {
            shown?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
        }
    }
}

/** Hàng chữ gợi ý (MẬT, BẢN SAO, BẢN NHÁP…), cuộn ngang. */
@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    val suggestions = listOf(
        stringResource(R.string.wm_suggest_confidential),
        stringResource(R.string.wm_suggest_copy),
        stringResource(R.string.wm_suggest_draft),
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { text ->
            SuggestionChip(onClick = { onPick(text) }, label = { Text(text) }, modifier = Modifier.heightIn(min = 40.dp))
        }
    }
}

/** Ô màu tròn; ô đang chọn có viền xanh và dấu tích. */
@Composable
private fun ColorSwatch(color: WatermarkColor, selected: Boolean, label: String, onClick: () -> Unit) {
    val fill = Color(0xFF000000.toInt() or color.rgb)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val ring = if (selected) Modifier.border(3.dp, Accent, CircleShape).padding(5.dp) else Modifier
        Box(Modifier.size(40.dp).then(ring).background(fill, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp), tint = Color.White)
        }
    }
}

@StringRes
private fun colorLabel(color: WatermarkColor): Int = when (color) {
    WatermarkColor.GRAY -> R.string.wm_color_gray
    WatermarkColor.RED -> R.string.wm_color_red
    WatermarkColor.BLUE -> R.string.wm_color_blue
}

@StringRes
private fun strengthLabel(strength: WatermarkStrength): Int = when (strength) {
    WatermarkStrength.LIGHT -> R.string.wm_light
    WatermarkStrength.MEDIUM -> R.string.wm_medium
    WatermarkStrength.STRONG -> R.string.wm_strong
}
