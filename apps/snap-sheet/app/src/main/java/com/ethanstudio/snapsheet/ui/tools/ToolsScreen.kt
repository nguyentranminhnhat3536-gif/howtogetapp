package com.ethanstudio.snapsheet.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.scan.ScanActions
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.ui.common.ScreenHeader
import com.ethanstudio.snapsheet.ui.common.ToolTile

/** Tab công cụ: lưới thẻ (2 cột trên điện thoại, nhiều cột hơn trên máy tính bảng). */
@Composable
fun ToolsScreen(
    isPro: Boolean,
    actions: ScanActions,
    onOcr: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(Modifier.bleed(16.dp)) {
                Text(stringResource(R.string.tools_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.tools_subtitle), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { ToolTile(R.drawable.ic_doc, stringResource(R.string.shortcut_single), stringResource(R.string.tool_single_desc), false, { actions.scan(ScanMode.SINGLE) }) }
        item { ToolTile(R.drawable.ic_pages, stringResource(R.string.shortcut_multi), stringResource(R.string.tool_multi_desc), false, { actions.scan(ScanMode.BATCH) }) }
        item { ToolTile(R.drawable.ic_id, stringResource(R.string.shortcut_id), stringResource(R.string.tool_id_desc), false, { actions.scan(ScanMode.ID_CARD) }) }
        item { ToolTile(R.drawable.ic_photo, stringResource(R.string.tool_import), stringResource(R.string.tool_import_desc), false, actions.importPhotos) }
        item { ToolTile(R.drawable.ic_text, stringResource(R.string.doc_text), stringResource(R.string.tool_ocr_desc), !isPro, onOcr) }
        item { ToolTile(R.drawable.ic_share, stringResource(R.string.tool_share_title), stringResource(R.string.tool_share_desc), false, onShare) }
    }
}

/** Cho phần tử tràn ra hai bên [side] (bù lại padding ngang của lưới) để nền đầu trang phủ hết chiều ngang. */
private fun Modifier.bleed(side: Dp): Modifier = layout { measurable, constraints ->
    val extra = side.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
