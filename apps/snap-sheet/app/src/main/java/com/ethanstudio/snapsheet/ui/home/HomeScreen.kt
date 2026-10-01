package com.ethanstudio.snapsheet.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.scan.ScanActions
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.ui.common.DocRow
import com.ethanstudio.snapsheet.ui.common.EmptyState
import com.ethanstudio.snapsheet.ui.common.QuickAction
import com.ethanstudio.snapsheet.ui.common.SectionTitle
import com.ethanstudio.snapsheet.ui.common.TabHeader
import com.ethanstudio.snapsheet.ui.main.MainUiState
import java.io.File

private const val RECENT_COUNT = 5

@Composable
fun HomeScreen(
    state: MainUiState,
    actions: ScanActions,
    pageFile: (Long) -> File,
    onOpenDoc: (Long) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.app_name_short), state.pro.isPro)
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    QuickAction(R.drawable.ic_scan, stringResource(R.string.home_quick_scan), { actions.scan(ScanMode.SINGLE) }, Modifier.weight(1f))
                    QuickAction(R.drawable.ic_doc, stringResource(R.string.home_quick_batch), { actions.scan(ScanMode.BATCH) }, Modifier.weight(1f))
                    QuickAction(R.drawable.ic_id, stringResource(R.string.home_quick_id), { actions.scan(ScanMode.ID_CARD) }, Modifier.weight(1f))
                    QuickAction(R.drawable.ic_photo, stringResource(R.string.home_quick_import), actions.importPhotos, Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionTitle(stringResource(R.string.home_recents))
                    if (state.allDocs.size > RECENT_COUNT) {
                        TextButton(onClick = onSeeAll) {
                            Text(stringResource(R.string.home_see_all), color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            if (state.allDocs.isEmpty()) {
                item { EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body)) }
            } else {
                items(state.allDocs.take(RECENT_COUNT), key = { it.id }) { doc ->
                    DocRow(doc, pageFile(doc.id), onClick = { onOpenDoc(doc.id) })
                }
            }
        }
    }
}
