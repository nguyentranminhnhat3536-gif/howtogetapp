package com.ethanstudio.snapsheet.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.scan.ScanActions
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.ui.common.DocRow
import com.ethanstudio.snapsheet.ui.common.EmptyState
import com.ethanstudio.snapsheet.ui.common.ScreenHeader
import com.ethanstudio.snapsheet.ui.common.SectionTitle
import com.ethanstudio.snapsheet.ui.common.ShortcutCircle
import com.ethanstudio.snapsheet.ui.common.docDate
import com.ethanstudio.snapsheet.ui.common.initialOf
import com.ethanstudio.snapsheet.ui.main.MainUiState
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import java.io.File

private const val RECENT_COUNT = 5

@Composable
fun HomeScreen(
    state: MainUiState,
    userName: String?,
    actions: ScanActions,
    pageFile: (Long) -> File,
    onOpenDoc: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onScanCard: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            ScreenHeader {
                Greeting(userName, onAccount)
                ScanCard(onScanCard)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShortcutCircle(R.drawable.ic_doc, stringResource(R.string.shortcut_single), { actions.scan(ScanMode.SINGLE) }, Modifier.weight(1f))
                ShortcutCircle(R.drawable.ic_pages, stringResource(R.string.shortcut_multi), { actions.scan(ScanMode.BATCH) }, Modifier.weight(1f))
                ShortcutCircle(R.drawable.ic_id, stringResource(R.string.shortcut_id), { actions.scan(ScanMode.ID_CARD) }, Modifier.weight(1f))
                ShortcutCircle(R.drawable.ic_photo, stringResource(R.string.shortcut_import), actions.importPhotos, Modifier.weight(1f))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionTitle(stringResource(R.string.home_recents), Modifier.weight(1f, fill = false))
                if (state.allDocs.isNotEmpty()) {
                    TextButton(onClick = onSeeAll, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.home_see_all), color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (state.allDocs.isEmpty()) {
            item { EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body)) }
        } else {
            items(state.allDocs.take(RECENT_COUNT), key = { it.id }) { doc ->
                val subtitle = stringResource(R.string.join_dot, docDate(doc), pluralStringResource(R.plurals.pages, doc.pageCount, doc.pageCount))
                DocRow(doc, pageFile(doc.id), subtitle, onClick = { onOpenDoc(doc.id) })
            }
        }
    }
}

/** Lời chào và ảnh đại diện tròn (bấm để mở tab Account). */
@Composable
private fun Greeting(userName: String?, onAccount: () -> Unit) {
    val name = userName?.takeIf { it.isNotBlank() }
    val accountLabel = stringResource(R.string.nav_account)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (name != null) stringResource(R.string.home_greeting, name) else stringResource(R.string.home_greeting_guest),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.app_name_short),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Gradients.Primary)
                .clickable(role = Role.Button, onClick = onAccount)
                .semantics { contentDescription = accountLabel },
            contentAlignment = Alignment.Center,
        ) {
            if (name != null) {
                Text(initialOf(name), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            } else {
                Icon(painterResource(R.drawable.ic_person), null, Modifier.size(22.dp), tint = Color.White)
            }
        }
    }
}

/** Thẻ lớn "Scan document": nền gradient xanh, mở bảng chọn cách quét. */
@Composable
private fun ScanCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Gradients.Primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(56.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_scan), null, Modifier.size(30.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.home_scan_title), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(stringResource(R.string.home_scan_sub), fontSize = 14.sp, lineHeight = 19.sp, color = Color.White.copy(alpha = 0.9f))
        }
        Icon(painterResource(R.drawable.ic_chevron), null, tint = Color.White)
    }
}
