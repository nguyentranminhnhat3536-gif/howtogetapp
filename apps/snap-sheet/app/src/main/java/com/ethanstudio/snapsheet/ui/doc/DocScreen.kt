package com.ethanstudio.snapsheet.ui.doc

import android.text.format.Formatter
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ethanstudio.snapsheet.data.CompressLevel
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.data.cleanDocName
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.scan.rememberScanActions
import com.ethanstudio.snapsheet.ui.common.BusyOverlay
import com.ethanstudio.snapsheet.ui.common.FolderNameDialog
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.MoveToFolderSheet
import com.ethanstudio.snapsheet.ui.common.PageThumbnail
import com.ethanstudio.snapsheet.ui.common.ProTag
import com.ethanstudio.snapsheet.ui.common.ScanChoiceSheet
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.util.printPdf
import com.ethanstudio.snapsheet.util.shareFiles
import com.ethanstudio.snapsheet.util.viewPdf
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun DocScreen(
    viewModel: DocViewModel,
    onBack: () -> Unit,
    onNeedPro: () -> Unit,
    onOpenOcr: (Long) -> Unit,
    onEditPages: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var addingPages by rememberSaveable { mutableStateOf(false) }
    var compressing by rememberSaveable { mutableStateOf(false) }
    var moving by rememberSaveable { mutableStateOf(false) }
    var creatingFolder by rememberSaveable { mutableStateOf(false) }

    val errorOpen = stringResource(R.string.error_open)
    val errorPrint = stringResource(R.string.error_print)
    val errorScan = stringResource(R.string.error_scan)
    val chooser = stringResource(R.string.share_chooser)

    // Thêm trang: quét tiếp hoặc chọn ảnh, tối đa số trang còn được thêm.
    val actions = rememberScanActions(
        pageLimit = state.pagesCanAdd.coerceAtLeast(1),
        onScanned = { pages, _ -> viewModel.addScanned(pages) },
        onPhotos = viewModel::addPhotos,
        onError = { scope.launch { snackbar.showSnackbar(errorScan) } },
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DocEvent.Export -> {
                    val doc = state.doc ?: return@collect
                    val ok = when (event.kind) {
                        ExportKind.OPEN_PDF -> context.viewPdf(viewModel.pdfFile())
                        ExportKind.SHARE_PDF -> context.shareFiles(listOf(viewModel.pdfFile()), "application/pdf", chooser)
                        ExportKind.SHARE_IMAGES -> context.shareFiles(viewModel.pageFiles(doc), "image/jpeg", chooser)
                        ExportKind.PRINT -> context.printPdf(viewModel.pdfFile(), doc.name)
                    }
                    if (!ok) {
                        val printFailed = event.kind == ExportKind.PRINT && viewModel.pdfFile().isFile
                        snackbar.showSnackbar(if (printFailed) errorPrint else errorOpen)
                    }
                }
                is DocEvent.NeedPro -> {
                    val text = if (event.reason == R.string.limit_pages_free) {
                        context.getString(event.reason, FreeLimits.FREE_PAGES, FreeLimits.PRO_PAGES)
                    } else {
                        context.getString(event.reason, FreeLimits.DAILY_EXPORTS)
                    }
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    onNeedPro()
                }
                is DocEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
                is DocEvent.MessageArgs -> snackbar.showSnackbar(context.getString(event.res, *event.args.toTypedArray()))
                DocEvent.Deleted -> onBack()
                is DocEvent.ShareCompressed -> {
                    if (!context.shareFiles(listOf(event.file), "application/pdf", chooser)) {
                        snackbar.showSnackbar(errorOpen)
                    } else if (event.noGain) {
                        snackbar.showSnackbar(context.getString(R.string.compress_no_gain))
                    } else {
                        val before = Formatter.formatShortFileSize(context, event.before)
                        val after = Formatter.formatShortFileSize(context, event.after)
                        snackbar.showSnackbar(context.getString(R.string.compress_result, before, after))
                    }
                }
                is DocEvent.Moved -> snackbar.showSnackbar(
                    event.folderName?.let { context.getString(R.string.move_done, it) } ?: context.getString(R.string.move_removed),
                )
                DocEvent.PageLimitPro -> snackbar.showSnackbar(context.getString(R.string.limit_pages_pro, FreeLimits.PRO_PAGES))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val doc = state.doc
            when {
                !state.loaded || state.deleted -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                doc == null -> MissingDoc(onBack, Modifier.padding(padding))
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) {
                    DocHeader(
                        doc.name,
                        onBack,
                        onMove = { moving = true },
                        onRename = { renaming = true },
                        onDelete = { deleting = true },
                    )
                    PagePager(doc, viewModel.pageFiles(doc), state.revision)
                    ActionRow(
                        isPro = state.pro.isPro,
                        onOpen = { viewModel.export(ExportKind.OPEN_PDF) },
                        onSharePdf = { viewModel.export(ExportKind.SHARE_PDF) },
                        onShareImages = { viewModel.export(ExportKind.SHARE_IMAGES) },
                        onExtract = { onOpenOcr(doc.id) },
                    )
                    ActionRow2(
                        onAddPages = { if (viewModel.requestAddPages()) addingPages = true },
                        onEditPages = { onEditPages(doc.id) },
                        onPrint = { viewModel.export(ExportKind.PRINT) },
                        onCompress = { compressing = true },
                    )
                    if (!state.pro.isPro) ExportsBar(state.exportsLeft, onNeedPro)
                }
            }
        }
        state.working?.let { BusyOverlay(stringResource(it)) }
    }

    if (addingPages && state.doc != null) {
        ScanChoiceSheet(
            title = stringResource(R.string.doc_add_pages),
            cameraSub = stringResource(R.string.add_scan_sub),
            photosSub = stringResource(R.string.add_photos_sub),
            onCamera = {
                addingPages = false
                actions.scan(ScanMode.BATCH)
            },
            onPhotos = {
                addingPages = false
                actions.importPhotos()
            },
            onDismiss = { addingPages = false },
        )
    }
    if (compressing && state.doc != null) {
        CompressSheet(
            onShare = { level ->
                compressing = false
                viewModel.compressAndShare(level)
            },
            onDismiss = { compressing = false },
        )
    }
    if (moving && state.doc != null) {
        MoveToFolderSheet(
            folders = state.folders,
            currentFolderId = state.doc?.folderId,
            onPick = {
                moving = false
                viewModel.moveTo(it)
            },
            onNewFolder = {
                moving = false
                creatingFolder = true
            },
            onDismiss = { moving = false },
        )
    }
    if (creatingFolder && state.doc != null) {
        FolderNameDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.folder_create),
            initial = "",
            folders = state.folders,
            editingId = null,
            onDismiss = { creatingFolder = false },
            onConfirm = {
                creatingFolder = false
                viewModel.createFolderAndMove(it)
            },
        )
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
}

/** Đầu trang: nút quay lại, tên tài liệu, nút chuyển thư mục, nút đổi tên và nút xóa. */
@Composable
private fun DocHeader(name: String, onBack: () -> Unit, onMove: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
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
        IconButton(onClick = onMove) {
            Icon(painterResource(R.drawable.ic_folder), stringResource(R.string.doc_move), tint = colors.onSurface)
        }
        IconButton(onClick = onRename) {
            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.doc_rename), tint = colors.onSurface)
        }
        IconButton(onClick = onDelete) {
            Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.doc_delete), tint = colors.error)
        }
    }
}

/** Xem từng trang bằng cách vuốt ngang, kèm dòng "Page x of y · ngày tạo". [revision] đổi thì đọc lại ảnh. */
@Composable
private fun PagePager(doc: Doc, pages: List<File>, revision: Long) {
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
            stamp = revision,
        )
    }
    if (pages.isEmpty()) return
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

/** Hàng nút thứ hai: thêm trang, sửa trang, in, nén. */
@Composable
private fun ActionRow2(
    onAddPages: () -> Unit,
    onEditPages: () -> Unit,
    onPrint: () -> Unit,
    onCompress: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionCircle(R.drawable.ic_plus, stringResource(R.string.doc_add_pages), false, onAddPages, Modifier.weight(1f))
        ActionCircle(R.drawable.ic_pages, stringResource(R.string.doc_edit_pages), false, onEditPages, Modifier.weight(1f))
        ActionCircle(R.drawable.ic_print, stringResource(R.string.doc_print), false, onPrint, Modifier.weight(1f))
        ActionCircle(R.drawable.ic_compress, stringResource(R.string.doc_compress), false, onCompress, Modifier.weight(1f))
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

/** Bảng chọn mức nén (Nhỏ / Vừa / Gốc) rồi nén và chia sẻ. Tài liệu đã lưu không đổi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompressSheet(onShare: (CompressLevel) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var level by rememberSaveable { mutableStateOf(CompressLevel.MEDIUM) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.compress_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(stringResource(R.string.compress_body), fontSize = 14.sp, lineHeight = 19.sp, color = colors.onSurfaceVariant)
            Column(Modifier.padding(vertical = 6.dp)) {
                CompressOption(R.string.compress_small, R.string.compress_small_sub, level == CompressLevel.SMALL) { level = CompressLevel.SMALL }
                CompressOption(R.string.compress_medium, R.string.compress_medium_sub, level == CompressLevel.MEDIUM) { level = CompressLevel.MEDIUM }
                CompressOption(R.string.compress_original, R.string.compress_original_sub, level == CompressLevel.ORIGINAL) { level = CompressLevel.ORIGINAL }
            }
            GradientButton(stringResource(R.string.compress_share), { onShare(level) }, Modifier.fillMaxWidth())
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompressOption(title: Int, sub: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(stringResource(sub), fontSize = 13.5.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
        }
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
