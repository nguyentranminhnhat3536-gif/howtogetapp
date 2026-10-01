package com.ethanstudio.snapsheet.ui.doc

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.PageThumbnail
import java.io.File

/** Màn Sửa trang: mỗi hàng một trang, nút Lên / Xuống / Xóa; bấm Lưu mới ghi vào tài liệu. */
@Composable
fun PagesScreen(viewModel: PagesViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var discarding by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val savedText = stringResource(R.string.pages_saved)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PagesEvent.Saved -> {
                    Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
                    onBack()
                }
                is PagesEvent.Message -> snackbar.showSnackbar(context.getString(event.res))
            }
        }
    }

    // Tài liệu không còn (đã bị xóa ở nơi khác): quay lại.
    LaunchedEffect(state.loaded, state.doc == null) {
        if (state.loaded && state.doc == null) onBack()
    }

    BackHandler(enabled = state.dirty) { discarding = true }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { PagesTopBar(onBack = { if (state.dirty) discarding = true else onBack() }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(colors.background).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                GradientButton(
                    stringResource(R.string.save),
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.dirty,
                    busy = state.saving,
                )
            }
        },
    ) { padding ->
        val doc = state.doc
        if (doc == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val last = state.order.lastIndex
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Text(
                        stringResource(R.string.pages_hint),
                        Modifier.padding(20.dp),
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
                itemsIndexed(state.order, key = { _, page -> page }) { index, page ->
                    PageRow(
                        file = viewModel.pageFile(doc, page),
                        position = index,
                        canMoveUp = index > 0,
                        canMoveDown = index < last,
                        onUp = { viewModel.moveUp(index) },
                        onDown = { viewModel.moveDown(index) },
                        onDelete = { viewModel.remove(index) },
                    )
                }
            }
        }
    }

    if (discarding) {
        AlertDialog(
            onDismissRequest = { discarding = false },
            title = { Text(stringResource(R.string.pages_discard_title)) },
            text = { Text(stringResource(R.string.pages_discard_body)) },
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

/** Thanh trên: nút quay lại và tiêu đề "Sửa trang". */
@Composable
private fun PagesTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(colors.background).statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_back), stringResource(R.string.doc_back), tint = colors.onSurface)
        }
        Text(
            stringResource(R.string.doc_edit_pages),
            Modifier.weight(1f).padding(horizontal = 4.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Một trang: ảnh nhỏ, "Trang n" theo vị trí mới, nút Lên, Xuống, Xóa. */
@Composable
private fun PageRow(
    file: File,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageThumbnail(
            file,
            Modifier.size(width = 56.dp, height = 72.dp).clip(shape).border(1.dp, colors.outlineVariant, shape),
        )
        Text(
            stringResource(R.string.doc_page, position + 1),
            Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
        IconButton(onClick = onUp, enabled = canMoveUp) {
            Icon(painterResource(R.drawable.ic_arrow_up), stringResource(R.string.page_move_up))
        }
        IconButton(onClick = onDown, enabled = canMoveDown) {
            Icon(painterResource(R.drawable.ic_arrow_down), stringResource(R.string.page_move_down))
        }
        IconButton(onClick = onDelete) {
            Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.page_delete), tint = colors.error)
        }
    }
}
