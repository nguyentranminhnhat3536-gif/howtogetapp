package com.ethanstudio.snapsheet.ui.doc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.data.cleanDocName
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.PageThumbnail
import com.ethanstudio.snapsheet.ui.common.ProTag
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.util.shareFiles
import com.ethanstudio.snapsheet.util.shareText
import com.ethanstudio.snapsheet.util.viewPdf
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun DocScreen(
    viewModel: DocViewModel,
    onBack: () -> Unit,
    onNeedPro: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    val errorOpen = stringResource(R.string.error_open)
    val chooser = stringResource(R.string.share_chooser)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DocEvent.Export -> {
                    val doc = state.doc ?: return@collect
                    val ok = when (event.kind) {
                        ExportKind.OPEN_PDF -> context.viewPdf(viewModel.pdfFile())
                        ExportKind.SHARE_PDF -> context.shareFiles(listOf(viewModel.pdfFile()), "application/pdf", chooser)
                        ExportKind.SHARE_IMAGES -> context.shareFiles(viewModel.pageFiles(doc), "image/jpeg", chooser)
                    }
                    if (!ok) snackbar.showSnackbar(errorOpen)
                }
                is DocEvent.NeedPro -> {
                    Toast.makeText(context, context.getString(event.reason, FreeLimits.DAILY_EXPORTS), Toast.LENGTH_LONG).show()
                    onNeedPro()
                }
                is DocEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
                DocEvent.Deleted -> onBack()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val doc = state.doc
        when {
            !state.loaded -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            doc == null -> MissingDoc(onBack, Modifier.padding(padding))
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                DocHeader(doc.name, onBack, onRename = { renaming = true }, onDelete = { deleting = true })
                PagePager(doc, viewModel.pageFiles(doc))
                ActionRow(
                    isPro = state.pro.isPro,
                    onOpen = { viewModel.export(ExportKind.OPEN_PDF) },
                    onSharePdf = { viewModel.export(ExportKind.SHARE_PDF) },
                    onShareImages = { viewModel.export(ExportKind.SHARE_IMAGES) },
                    onExtract = viewModel::recognizeText,
                )
                if (!state.pro.isPro) ExportsBar(state.exportsLeft, onNeedPro)
            }
        }
    }

    if (renaming && state.doc != null) {
        RenameDialog(
            initial = state.doc?.name.orEmpty(),
            onDismiss = { renaming = false },
            onSave = { viewModel.rename(it); renaming = false },
        )
    }
    if (deleting && state.doc != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton({ deleting = false; viewModel.delete() }) {
                    Text(stringResource(R.string.doc_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton({ deleting = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (state.ocrBusy) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.ocr_busy))
                }
            },
        )
    }
    state.ocrText?.let { text ->
        OcrDialog(text, onDismiss = viewModel::dismissText)
    }
}

/** Đầu trang: nút quay lại, tên tài liệu, nút đổi tên và nút xóa. */
@Composable
private fun DocHeader(name: String, onBack: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(Gradients.header()).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.doc_back), tint = colors.onSurface)
        }
        Text(
            name,
            Modifier.weight(1f).padding(horizontal = 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRename) {
            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.doc_rename), tint = colors.onSurface)
        }
        IconButton(onClick = onDelete) {
            Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.doc_delete), tint = colors.error)
        }
    }
}

/** Xem từng trang bằng cách vuốt ngang, kèm dòng "Page x of y · ngày tạo". */
@Composable
private fun PagePager(doc: Doc, pages: List<File>) {
    val pager = rememberPagerState { pages.size }
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(doc.createdAt, locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(doc.createdAt)) }
    val shape = RoundedCornerShape(14.dp)
    HorizontalPager(
        pager,
        Modifier.fillMaxWidth().padding(top = 12.dp).height(380.dp),
        contentPadding = PaddingValues(horizontal = 48.dp),
        pageSpacing = 16.dp,
    ) { index ->
        PageThumbnail(
            pages[index],
            Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
                .shadow(8.dp, shape)
                .clip(shape)
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            target = 1200,
            contentScale = ContentScale.Fit,
        )
    }
    Text(
        stringResource(R.string.doc_page_of, minOf(pager.currentPage + 1, pages.size), pages.size, date),
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 18.dp),
        textAlign = TextAlign.Center,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Bốn nút tròn: mở PDF, chia sẻ PDF, chia sẻ ảnh, lấy chữ (Pro). */
@Composable
private fun ActionRow(
    isPro: Boolean,
    onOpen: () -> Unit,
    onSharePdf: () -> Unit,
    onShareImages: () -> Unit,
    onExtract: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionCircle(R.drawable.ic_open, stringResource(R.string.doc_open), false, onOpen, Modifier.weight(1f))
        ActionCircle(R.drawable.ic_share, stringResource(R.string.doc_share_pdf), false, onSharePdf, Modifier.weight(1f))
        ActionCircle(R.drawable.ic_photo, stringResource(R.string.doc_share_images), false, onShareImages, Modifier.weight(1f))
        ActionCircle(R.drawable.ic_text, stringResource(R.string.doc_extract), !isPro, onExtract, Modifier.weight(1f))
    }
}

@Composable
private fun ActionCircle(@DrawableRes icon: Int, label: String, pro: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(56.dp).background(Gradients.soft(), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(24.dp), tint = Accent)
            }
            Text(
                label,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (pro) ProTag(Modifier.align(Alignment.TopEnd).padding(end = 4.dp))
    }
}

/** Thanh lượt xuất miễn phí còn lại hôm nay, kèm link nâng cấp. */
@Composable
private fun ExportsBar(left: Int, onUpgrade: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp)
            .background(Color(0xFFF3F7FF), RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_infinity), null, Modifier.size(22.dp), tint = Accent)
        Text(stringResource(R.string.exports_left, left), Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onUpgrade, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.account_upgrade), color = Accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Tài liệu đã bị xóa hoặc id sai: báo rõ và cho quay lại, không quay vòng mãi. */
@Composable
private fun MissingDoc(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.doc_missing),
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        GradientButton(stringResource(R.string.doc_back), onBack, minHeight = 48.dp, fontSize = 16.sp)
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(value, { value = it.take(80) }, label = { Text(stringResource(R.string.rename_label)) }, singleLine = true)
        },
        confirmButton = {
            TextButton({ onSave(value) }, enabled = cleanDocName(value) != null) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun OcrDialog(text: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val copied = stringResource(R.string.copied)
    val chooser = stringResource(R.string.share_chooser)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ocr_title)) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text(text.ifBlank { stringResource(R.string.ocr_empty) })
            }
        },
        confirmButton = {
            Row {
                if (text.isNotBlank()) {
                    TextButton({
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    }) { Text(stringResource(R.string.copy)) }
                    TextButton({ context.shareText(text, chooser) }) { Text(stringResource(R.string.share_text)) }
                }
                TextButton(onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
    )
}
