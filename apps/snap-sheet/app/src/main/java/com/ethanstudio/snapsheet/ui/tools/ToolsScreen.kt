package com.ethanstudio.snapsheet.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.scan.ScanActions
import com.ethanstudio.snapsheet.scan.ScanMode
import com.ethanstudio.snapsheet.ui.common.TabHeader
import com.ethanstudio.snapsheet.ui.common.ToolCard

@Composable
fun ToolsScreen(isPro: Boolean, actions: ScanActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.tools_title), isPro)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToolCard(R.drawable.ic_scan, stringResource(R.string.tool_single), stringResource(R.string.tool_single_sub), { actions.scan(ScanMode.SINGLE) })
            ToolCard(R.drawable.ic_doc, stringResource(R.string.tool_batch), stringResource(R.string.tool_batch_sub), { actions.scan(ScanMode.BATCH) })
            ToolCard(R.drawable.ic_id, stringResource(R.string.tool_id), stringResource(R.string.tool_id_sub), { actions.scan(ScanMode.ID_CARD) })
            ToolCard(R.drawable.ic_photo, stringResource(R.string.tool_import), stringResource(R.string.tool_import_sub), actions.importPhotos)
        }
    }
}
