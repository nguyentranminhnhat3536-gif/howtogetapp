package com.ethanstudio.snapsheet.ui.sign

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.SIGN_BITMAP_WIDTH
import com.ethanstudio.snapsheet.data.SIGN_INK
import com.ethanstudio.snapsheet.data.SIGN_MAX_WIDTH
import com.ethanstudio.snapsheet.data.SIGN_MIN_WIDTH
import com.ethanstudio.snapsheet.data.SIGN_PAD_ASPECT
import com.ethanstudio.snapsheet.data.SIGN_STROKE_FRACTION
import com.ethanstudio.snapsheet.data.SignaturePlacement
import com.ethanstudio.snapsheet.data.StrokePoint
import com.ethanstudio.snapsheet.data.fitInside
import com.ethanstudio.snapsheet.ui.common.BusyOverlay
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.PageThumbnail
import com.ethanstudio.snapsheet.ui.qr.OutlinedPillButton
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.util.decodeSampled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Viền khung ký và vạch kẻ ký (màu đã có sẵn trong app). */
private val PadBorder = Color(0xFF9EBDF2)
private val PadLine = Color(0xFFC9D6EE)

/** Màn Ký tên: vẽ chữ ký (khung ký), rồi đặt lên các trang và lưu thành bản sao. */
@Composable
fun SignScreen(viewModel: SignViewModel, onBack: () -> Unit, onDone: (Long) -> Unit, onNeedPro: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var discarding by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val nameTemplate = stringResource(R.string.sign_copy_name)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SignEvent.Done -> {
                    Toast.makeText(context, context.getString(R.string.sign_done), Toast.LENGTH_SHORT).show()
                    onDone(event.id)
                }
                SignEvent.NeedPro -> {
                    Toast.makeText(context, context.getString(R.string.sign_pro_only), Toast.LENGTH_LONG).show()
                    onNeedPro()
                }
                is SignEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
                SignEvent.Leave -> onBack()
            }
        }
    }

    // Tài liệu không còn (đã bị xóa ở nơi khác): quay lại.
    LaunchedEffect(state.loaded, state.doc == null) {
        if (state.loaded && state.doc == null) onBack()
    }

    BackHandler(enabled = state.padOpen) { viewModel.cancelPad() }
    BackHandler(enabled = !state.padOpen && state.placements.isNotEmpty() && !state.saving) { discarding = true }
    // Đang lưu thì nút Back của hệ thống không làm gì, để việc lưu không bị hủy giữa chừng.
    // Đặt sau cùng để được ưu tiên hơn hai BackHandler ở trên.
    BackHandler(enabled = state.saving) {}

    // Nút quay lại trên thanh trên làm giống hệt nút Back của hệ thống.
    fun goBack() {
        if (state.padOpen) {
            viewModel.cancelPad()
        } else if (state.placements.isNotEmpty() && !state.saving) {
            discarding = true
        } else {
            onBack()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { SignTopBar(onBack = { goBack() }) },
            bottomBar = {
                if (!state.padOpen && state.doc != null) {
                    Box(Modifier.fillMaxWidth().background(colors.background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        GradientButton(
                            stringResource(R.string.sign_save),
                            onClick = { viewModel.save(nameTemplate) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.canSave,
                            busy = state.saving,
                        )
                    }
                }
            },
        ) { padding ->
            val doc = state.doc
            when {
                doc == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.padOpen -> SignPad(state, viewModel, Modifier.padding(padding))
                else -> PlaceSignature(doc, state, viewModel, Modifier.padding(padding))
            }
        }
        if (state.saving) BusyOverlay(stringResource(R.string.saving))
    }

    if (discarding) {
        AlertDialog(
            onDismissRequest = { discarding = false },
            title = { Text(stringResource(R.string.pages_discard_title)) },
            text = { Text(stringResource(R.string.sign_discard_body)) },
            confirmButton = {
                TextButton({
                    discarding = false
                    onBack()
                }) {
                    Text(stringResource(R.string.pages_discard), color = colors.error)
                }
            },
            dismissButton = { TextButton({ discarding = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Thanh trên: nút quay lại và tiêu đề "Ký tên". */
@Composable
private fun SignTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(colors.background).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.doc_back), tint = colors.onSurface)
        }
        Text(
            stringResource(R.string.tool_sign_title),
            Modifier.weight(1f).padding(horizontal = 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Khung vẽ chữ ký: vẽ bằng ngón tay, xóa nét, xóa chữ ký đã lưu, dùng chữ ký này. */
@Composable
private fun SignPad(state: SignUiState, viewModel: SignViewModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.sign_pad_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Text(stringResource(R.string.sign_pad_hint), fontSize = 14.sp, lineHeight = 19.sp, color = colors.onSurfaceVariant)
        DrawingPad(
            strokes = state.strokes,
            onStart = viewModel::startStroke,
            onExtend = viewModel::extendStroke,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::clearStrokes, enabled = state.strokes.isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.sign_clear), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            if (state.hasSignature) {
                TextButton(onClick = viewModel::deleteSignature, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.sign_delete_saved), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.error)
                }
            }
        }
        GradientButton(
            stringResource(R.string.sign_use),
            onClick = viewModel::useStrokes,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canUseStrokes,
            busy = state.saving,
        )
        TextButton(onClick = viewModel::cancelPad, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.cancel), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
        }
        SignOnDeviceChip()
    }
}

/** Khung ký tỉ lệ 2:1 nền trắng, có vạch kẻ ký; nét vẽ màu mực xanh đen, cùng bề dày với ảnh chữ ký được lưu. */
@Composable
private fun DrawingPad(
    strokes: List<List<StrokePoint>>,
    onStart: (StrokePoint) -> Unit,
    onExtend: (StrokePoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val start by rememberUpdatedState(onStart)
    val extend by rememberUpdatedState(onExtend)
    Box(
        modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .aspectRatio(SIGN_PAD_ASPECT)
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, PadBorder, shape),
    ) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                fun norm(offset: Offset): StrokePoint = StrokePoint(offset.x / size.width, offset.y / size.height)
                detectDragGestures(
                    onDragStart = { offset: Offset -> start(norm(offset)) },
                    onDrag = { change, _ ->
                        change.consume()
                        extend(norm(change.position))
                    },
                )
            },
        ) {
            val lineY = size.height * 0.75f
            drawLine(PadLine, Offset(size.width * 0.08f, lineY), Offset(size.width * 0.92f, lineY), strokeWidth = 1.dp.toPx())
            val ink = Color(SIGN_INK)
            val stroke = Stroke(width = size.width * SIGN_STROKE_FRACTION, cap = StrokeCap.Round, join = StrokeJoin.Round)
            for (line in strokes) {
                if (line.isEmpty()) continue
                if (line.size == 1) {
                    drawCircle(ink, radius = stroke.width / 2, center = Offset(line[0].x * size.width, line[0].y * size.height))
                } else {
                    val path = Path()
                    path.moveTo(line[0].x * size.width, line[0].y * size.height)
                    for (i in 1 until line.size) path.lineTo(line[i].x * size.width, line[i].y * size.height)
                    drawPath(path, ink, style = stroke)
                }
            }
        }
    }
}

/** Bước đặt chữ ký: xem từng trang, đặt/bỏ chữ ký, kéo để đổi chỗ, thanh trượt để đổi cỡ. */
@Composable
private fun PlaceSignature(doc: Doc, state: SignUiState, viewModel: SignViewModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val current = state.current
    val last = (doc.pageCount - 1).coerceAtLeast(0)
    val pageAspect = state.pageAspects.getOrNull(state.page)?.takeIf { it > 0f } ?: 1.414f
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.sign_drag_hint),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = colors.onSurfaceVariant,
        )
        PageWithSignature(
            page = viewModel.pageFile(doc, state.page),
            pageAspect = pageAspect,
            placement = current,
            sigAspect = state.sigAspect,
            signatureFile = viewModel.signatureFile,
            signatureVersion = state.signatureVersion,
            onMove = viewModel::moveBy,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.goTo(state.page - 1) }, enabled = state.page > 0) {
                Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.sign_prev))
            }
            Text(stringResource(R.string.ocr_page_of, state.page + 1, doc.pageCount), fontSize = 15.sp, color = colors.onSurface)
            IconButton(onClick = { viewModel.goTo(state.page + 1) }, enabled = state.page < last) {
                Icon(painterResource(R.drawable.ic_chevron), stringResource(R.string.sign_next))
            }
        }
        if (current != null) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                val sizeLabel = stringResource(R.string.sign_size)
                Text(sizeLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Slider(
                    value = current.widthFraction,
                    onValueChange = viewModel::setSize,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = sizeLabel },
                    valueRange = SIGN_MIN_WIDTH..SIGN_MAX_WIDTH,
                )
                TextButton(onClick = viewModel::removeFromCurrent, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.sign_remove), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.error)
                }
            }
        } else {
            OutlinedPillButton(
                R.drawable.ic_plus,
                stringResource(R.string.sign_place),
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                onClick = viewModel::placeOnCurrent,
            )
        }
        TextButton(onClick = viewModel::openPad, modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.sign_change), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        }
        SignOnDeviceChip(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp))
    }
}

/** Ảnh trang (vừa khung, giữ tỉ lệ) và chữ ký đặt trên đó; kéo chữ ký để đổi chỗ. */
@Composable
private fun PageWithSignature(
    page: File,
    pageAspect: Float,
    placement: SignaturePlacement?,
    sigAspect: Float,
    signatureFile: File,
    signatureVersion: Long,
    onMove: (Float, Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val signature by produceState<ImageBitmap?>(null, signatureFile, signatureVersion) {
        value = withContext(Dispatchers.IO) {
            try {
                decodeSampled(signatureFile, SIGN_BITMAP_WIDTH)?.asImageBitmap()
            } catch (e: OutOfMemoryError) {
                null
            }
        }
    }
    val description = stringResource(R.string.sign_signature_desc)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(420.dp).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        val (boxW, boxH) = fitInside(maxWidth.value, maxHeight.value, pageAspect)
        val density = LocalDensity.current
        val pageWidthPx by rememberUpdatedState(with(density) { boxW.dp.toPx() })
        val pageHeightPx by rememberUpdatedState(with(density) { boxH.dp.toPx() })
        val move by rememberUpdatedState(onMove)
        Box(Modifier.size(boxW.dp, boxH.dp).border(1.dp, colors.outlineVariant)) {
            PageThumbnail(page, Modifier.fillMaxSize(), target = 1200, contentScale = ContentScale.FillBounds)
            if (placement != null) {
                val sigW = placement.widthFraction * boxW
                val sigH = sigW * sigAspect
                Box(
                    Modifier
                        .offset {
                            val wPx = placement.widthFraction * pageWidthPx
                            val hPx = wPx * sigAspect
                            IntOffset(
                                (placement.cx * pageWidthPx - wPx / 2).roundToInt(),
                                (placement.cy * pageHeightPx - hPx / 2).roundToInt(),
                            )
                        }
                        .size(sigW.dp, sigH.dp)
                        .border(1.dp, Accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                if (pageWidthPx > 0f && pageHeightPx > 0f) move(amount.x / pageWidthPx, amount.y / pageHeightPx)
                            }
                        }
                        .semantics { contentDescription = description },
                ) {
                    signature?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
                }
            }
        }
    }
}

/** Dòng nhỏ "chữ ký chỉ lưu trên điện thoại này" (cùng kiểu với dòng "chạy trên điện thoại" ở màn lấy chữ). */
@Composable
private fun SignOnDeviceChip(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Gradients.soft()).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_shield), null, Modifier.size(16.dp), tint = Accent)
        Text(stringResource(R.string.sign_on_device), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
