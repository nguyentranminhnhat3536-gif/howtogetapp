package com.ethanstudio.snapsheet.ui.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.ui.common.DocRow
import com.ethanstudio.snapsheet.ui.common.EmptyState
import com.ethanstudio.snapsheet.ui.common.TabHeader
import com.ethanstudio.snapsheet.ui.main.MainUiState
import java.io.File

@Composable
fun FilesScreen(
    state: MainUiState,
    onQuery: (String) -> Unit,
    pageFile: (Long) -> File,
    onOpenDoc: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                TabHeader(stringResource(R.string.files_title), state.pro.isPro)
                TextField(
                    value = state.query,
                    onValueChange = onQuery,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                    shape = MaterialTheme.shapes.small,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }
        }
        when {
            state.allDocs.isEmpty() -> EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body))
            state.docs.isEmpty() -> Text(
                stringResource(R.string.empty_search),
                Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn {
                items(state.docs, key = { it.id }) { doc -> DocRow(doc, pageFile(doc.id), onClick = { onOpenDoc(doc.id) }) }
            }
        }
    }
}
