package com.ethanstudio.snapsheet.ui.doc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.PageThumbnail
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.util.shareFiles
import com.ethanstudio.snapsheet.util.shareText
import com.ethanstudio.snapsheet.util.viewPdf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocScreen(
    viewModel: DocViewModel,
    onBack: () -> Unit,
    onNeedPro: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

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
        topBar = {
            TopAppBar(
                title = { Text(state.doc?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_chevron), stringResource(R.string.doc_back), Modifier.rotate(180f))
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }) {
                        Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.doc_rename))
                    }
                    IconButton(onClick = { deleting = true }) {
                        Icon(painterResource(R.drawable.ic_trash), stringResource(R.string.doc_delete))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val doc = state.doc
        if (doc == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val pages = viewModel.pageFiles(doc)
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GradientButton(stringResource(R.string.doc_open), { viewModel.export(ExportKind.OPEN_PDF) }, Modifier.weight(1f), minHeight = 48.dp, fontSize = 15.sp)
                            GradientButton(stringResource(R.string.doc_share_pdf), { viewModel.export(ExportKind.SHARE_PDF) }, Modifier.weight(1f), minHeight = 48.dp, fontSize = 15.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton({ viewModel.export(ExportKind.SHARE_IMAGES) }, Modifier.weight(1f)) { Text(stringResource(R.string.doc_share_images)) }
                            FilledTonalButton({ viewModel.recognizeText() }, Modifier.weight(1f)) { Text(stringResource(R.string.doc_text)) }
                        }
                        if (!state.pro.isPro) {
                            Text(
                                stringResource(R.string.exports_left, state.exportsLeft),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                itemsIndexed(pages) { index, file ->
                    PageThumbnail(
                        file,
                        Modifier.fillMaxWidth().heightIn(min = 240.dp).clip(MaterialTheme.shapes.small),
                        target = 1200,
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
    }

    if (renaming) {
        RenameDialog(
            initial = state.doc?.name.orEmpty(),
            onDismiss = { renaming = false },
            onSave = { viewModel.rename(it); renaming = false },
        )
    }
    if (deleting) {
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

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(value, { value = it }, label = { Text(stringResource(R.string.rename_label)) }, singleLine = true)
        },
        confirmButton = { TextButton({ onSave(value) }) { Text(stringResource(R.string.save)) } },
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
