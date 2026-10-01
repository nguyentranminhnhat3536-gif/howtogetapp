package com.ethanstudio.snapsheet.ui.ocr

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.ocr.charCount
import com.ethanstudio.snapsheet.ocr.findMatches
import com.ethanstudio.snapsheet.ocr.wordCount
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.PageThumbnail
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.ui.theme.NavMuted
import com.ethanstudio.snapsheet.ui.theme.displaySerif
import com.ethanstudio.snapsheet.util.shareText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Màu tô chỗ khớp khi tìm; chữ tối cố định để luôn đọc được trên nền vàng. */
private val MatchBackground = Color(0xFFFFE58A)
private val MatchText = Color(0xFF131A2A)

private const val COPIED_MS = 1_600L
private const val SCAN_LINE_MS = 1_600

/** Màn "Extract text" của một tài liệu: đang đọc, kết quả, không thấy chữ, lỗi, khóa (bản miễn phí). */
@Composable
fun OcrScreen(
    viewModel: OcrViewModel,
    onBack: () -> Unit,
    onSeePlans: () -> Unit,
    onScanAgain: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val phase = state.phase

    fun show(@StringRes res: Int) {
        scope.launch { snackbar.showSnackbar(context.getString(res)) }
    }

    BackHandler(enabled = phase is OcrPhase.Reading) { viewModel.cancel() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OcrEvent.Close -> onBack()
                is OcrEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
            }
        }
    }

    val fallbackName = stringResource(R.string.app_name_short)
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let(viewModel::saveTxt)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        topBar = {
            OcrHeader(
                title = stringResource(if (phase is OcrPhase.Done) R.string.ocr_result_title else R.string.doc_extract),
                onBack = { if (phase is OcrPhase.Reading) viewModel.cancel() else onBack() },
                onReadAgain = if (phase is OcrPhase.Done) viewModel::retry else null,
            )
        },
        bottomBar = {
            if (phase is OcrPhase.Done) {
                ResultActions(
                    text = state.text,
                    onCopyFailed = { show(R.string.err_unknown) },
                    onCopied = { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) show(R.string.copied) },
                    onShareFailed = { show(it) },
                    onSave = {
                        try {
                            saveLauncher.launch(viewModel.suggestedFileName(fallbackName))
                        } catch (e: ActivityNotFoundException) {
                            show(R.string.ocr_save_failed)
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            val content = Modifier.widthIn(max = 640.dp).fillMaxSize()
            when (phase) {
                OcrPhase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                OcrPhase.Locked -> OcrLockedContent(onSeePlans = onSeePlans, onNotNow = onBack, modifier = content)
                OcrPhase.Missing -> MessageState(R.string.doc_missing, content) {
                    GradientButton(stringResource(R.string.doc_back), onBack, minHeight = 48.dp, fontSize = 16.sp)
                }
                is OcrPhase.Reading -> ReadingState(phase, onCancel = viewModel::cancel, modifier = content)
                is OcrPhase.Done -> ResultState(
                    pageCount = phase.pages.size,
                    selected = state.selected,
                    query = state.query,
                    joined = state.joined,
                    text = state.text,
                    onSelect = viewModel::selectPage,
                    onQuery = viewModel::setQuery,
                    onToggleJoin = viewModel::toggleJoin,
                    modifier = content,
                )
                OcrPhase.Empty -> EmptyResult(onScanAgain, onBack, content)
                OcrPhase.Failed -> MessageState(R.string.ocr_failed, content) {
                    GradientButton(stringResource(R.string.ocr_try_again), viewModel::retry, Modifier.fillMaxWidth(), minHeight = 52.dp, fontSize = 16.sp)
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.ocr_back_to_doc), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                    }
                }
            }
        }
    }
}

/** Đầu trang: nút quay lại, tiêu đề, nút "Đọc lại" (chỉ khi đã có kết quả). */
@Composable
private fun OcrHeader(title: String, onBack: () -> Unit, onReadAgain: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(colors.background).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.doc_back), tint = colors.onSurface)
        }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onReadAgain != null) {
            IconButton(onClick = onReadAgain) {
                Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.ocr_read_again), tint = colors.onSurface)
            }
        }
    }
}

/** Đang đọc: ảnh trang có vạch sáng chạy, "Page x of y", thanh tiến độ, nút Hủy. */
@Composable
private fun ReadingState(phase: OcrPhase.Reading, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.background(Gradients.header()).navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScanningPage(phase)
            Text(
                stringResource(R.string.ocr_reading),
                Modifier.padding(top = 10.dp),
                fontFamily = displaySerif(),
                fontSize = 28.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                color = colors.onSurface,
            )
            Text(stringResource(R.string.ocr_page_of, phase.page, phase.total), fontSize = 15.sp, color = colors.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { ((phase.page - 1f) / phase.total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Accent,
                trackColor = colors.surfaceVariant,
            )
            OnDeviceChip()
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).heightIn(min = 54.dp),
        ) {
            Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        }
    }
}

/** Ảnh trang đang đọc, có một vạch sáng chạy từ trên xuống. */
@Composable
private fun ScanningPage(phase: OcrPhase.Reading) {
    val shape = RoundedCornerShape(16.dp)
    val pageHeight = 320.dp
    val lineHeight = 3.dp
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SCAN_LINE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "scanLine",
    )
    Box(
        Modifier
            .size(width = 240.dp, height = pageHeight)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        val file = phase.pageFile
        if (file != null) {
            PageThumbnail(file, Modifier.fillMaxSize(), target = 800, contentScale = ContentScale.Fit)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(lineHeight)
                .offset { IntOffset(0, (progress * (pageHeight - lineHeight).toPx()).roundToInt()) }
                .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = 0f), Accent, Accent.copy(alpha = 0f)))),
        )
    }
}

/** Kết quả: chọn trang, tìm, hộp chữ chọn được, thống kê và nút nối dòng. */
@Composable
private fun ResultState(
    pageCount: Int,
    selected: Int,
    query: String,
    joined: Boolean,
    text: String,
    onSelect: (Int) -> Unit,
    onQuery: (String) -> Unit,
    onToggleJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val matches = remember(text, query) { findMatches(text, query) }
    val words = remember(text) { wordCount(text) }
    val chars = remember(text) { charCount(text) }
    Column(modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (pageCount > 1) PageChips(pageCount, selected, onSelect)
        FindField(query, onQuery, matches.size, Modifier.padding(horizontal = 20.dp))
        TextBox(text, matches, blankPage = selected > 0 && text.isBlank(), modifier = Modifier.weight(1f).padding(horizontal = 20.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(
                    R.string.join_dot,
                    pluralStringResource(R.plurals.ocr_words, words, words),
                    pluralStringResource(R.plurals.ocr_chars, chars, chars),
                ),
                Modifier.weight(1f),
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
            TextButton(onClick = onToggleJoin, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(painterResource(R.drawable.ic_wrap), null, Modifier.size(18.dp), tint = Accent)
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(if (joined) R.string.ocr_keep_breaks else R.string.ocr_join_lines),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent,
                )
            }
        }
    }
}

/** Hàng chip "All pages", "Page 1", "Page 2"… cuộn ngang. */
@Composable
private fun PageChips(pageCount: Int, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(pageCount + 1) { index ->
            val label = if (index == 0) stringResource(R.string.ocr_all_pages) else stringResource(R.string.doc_page, index)
            PageChip(label, index == selected) { onSelect(index) }
        }
    }
}

@Composable
private fun PageChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val base = Modifier.heightIn(min = 40.dp).clip(CircleShape)
    val styled = if (isSelected) base.background(Gradients.Primary) else base.background(colors.surface).border(1.dp, colors.outlineVariant, CircleShape)
    Box(styled.clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else colors.onSurface,
            maxLines = 1,
        )
    }
}

/** Ô tìm trong chữ, kèm số chỗ khớp ở bên phải. */
@Composable
private fun FindField(query: String, onQuery: (String) -> Unit, matchCount: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.ocr_find_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, tint = NavMuted) },
        trailingIcon = if (query.isNotBlank()) {
            {
                Text(
                    pluralStringResource(R.plurals.ocr_matches, matchCount, matchCount),
                    Modifier.padding(end = 14.dp),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = Accent,
            unfocusedBorderColor = colors.outlineVariant,
        ),
    )
}

/** Hộp chữ cuộn được, chọn được; chỗ khớp được tô vàng. */
@Composable
private fun TextBox(text: String, matches: List<IntRange>, blankPage: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val annotated = remember(text, matches) { highlight(text, matches) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (blankPage) {
            Text(stringResource(R.string.ocr_page_blank), fontSize = 15.5.sp, lineHeight = 24.sp, color = colors.onSurfaceVariant)
        } else {
            SelectionContainer {
                Text(annotated, fontSize = 15.5.sp, lineHeight = 24.sp, color = colors.onSurface)
            }
        }
    }
}

private fun highlight(text: String, matches: List<IntRange>): AnnotatedString = buildAnnotatedString {
    append(text)
    val style = SpanStyle(background = MatchBackground, color = MatchText)
    matches.forEach { addStyle(style, it.first, it.last + 1) }
}

/** Thanh đáy màn kết quả: Copy (nút chính), Share, Save as .txt. */
@Composable
private fun ResultActions(
    text: String,
    onCopyFailed: () -> Unit,
    onCopied: () -> Unit,
    onShareFailed: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val chooser = stringResource(R.string.share_chooser)
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    Column(Modifier.fillMaxWidth().background(colors.surface).navigationBarsPadding()) {
        HorizontalDivider(color = colors.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CopyButton(
                label = stringResource(if (copied) R.string.ocr_copied_short else R.string.ocr_copy),
                modifier = Modifier.weight(1f),
                onClick = {
                    val ok = try {
                        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: error("no clipboard")
                        clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                        true
                    } catch (e: RuntimeException) {
                        false
                    }
                    if (ok) {
                        copied = true
                        onCopied()
                    } else {
                        onCopyFailed()
                    }
                },
            )
            RoundAction(R.drawable.ic_share, stringResource(R.string.share_text)) {
                val ok = try {
                    context.shareText(text, chooser)
                } catch (e: RuntimeException) {
                    onShareFailed(R.string.err_unknown)
                    return@RoundAction
                }
                if (!ok) onShareFailed(R.string.error_no_app)
            }
            RoundAction(R.drawable.ic_download, stringResource(R.string.ocr_save_txt), onSave)
        }
    }
}

@Composable
private fun CopyButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(Gradients.Primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(20.dp), tint = Color.White)
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoundAction(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, Modifier.size(22.dp), tint = Accent)
    }
}

/** Không thấy chữ: lý do, 3 mẹo, nút quét lại và quay về tài liệu. */
@Composable
private fun EmptyResult(onScanAgain: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.navigationBarsPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(96.dp).background(colors.surfaceVariant, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_text), null, Modifier.size(44.dp), tint = NavMuted)
            }
            Text(
                stringResource(R.string.ocr_none_title),
                Modifier.padding(top = 8.dp),
                fontFamily = displaySerif(),
                fontSize = 30.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                color = colors.onSurface,
            )
            Text(
                stringResource(R.string.ocr_none_body),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
            )
            TipsCard(Modifier.padding(top = 8.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GradientButton(stringResource(R.string.ocr_scan_again), onScanAgain, Modifier.fillMaxWidth(), minHeight = 54.dp, fontSize = 17.sp)
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ocr_back_to_doc), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
            }
        }
    }
}

@Composable
private fun TipsCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Tip(R.drawable.ic_bulb, R.string.ocr_tip_light_title, R.string.ocr_tip_light_body)
        Tip(R.drawable.ic_scan, R.string.ocr_tip_flat_title, R.string.ocr_tip_flat_body)
        Tip(R.drawable.ic_globe, R.string.ocr_tip_script_title, R.string.ocr_tip_script_body)
    }
}

@Composable
private fun Tip(@DrawableRes icon: Int, @StringRes title: Int, @StringRes body: Int) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).background(Gradients.soft(), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = Accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Text(stringResource(body), fontSize = 13.5.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
        }
    }
}

/** Một câu báo ở giữa màn (tài liệu không còn, đọc lỗi) kèm các nút bên dưới. */
@Composable
private fun MessageState(@StringRes message: Int, modifier: Modifier = Modifier, actions: @Composable ColumnScope.() -> Unit) {
    Box(modifier.navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(message),
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            actions()
        }
    }
}
