package com.ethanstudio.snapsheet.ui.doc

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.data.ProState
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.cleanDocName
import com.ethanstudio.snapsheet.ocr.TextOcr
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

enum class ExportKind { OPEN_PDF, SHARE_PDF, SHARE_IMAGES }

data class DocUiState(
    val doc: Doc? = null,
    val pro: ProState = ProState(),
    val ocrText: String? = null,
    val ocrBusy: Boolean = false,
    /** Đã đọc xong từ cơ sở dữ liệu chưa (để phân biệt "đang tải" với "tài liệu không còn"). */
    val loaded: Boolean = false,
) {
    val exportsLeft: Int
        get() = FreeLimits.remaining(pro.isPro, pro.usage, LocalDate.now().toEpochDay())
}

sealed interface DocEvent {
    data class Export(val kind: ExportKind) : DocEvent
    /** Hết lượt xuất miễn phí hoặc cần Pro: mở màn mua. */
    data class NeedPro(@StringRes val reason: Int) : DocEvent
    data class Message(@StringRes val res: Int) : DocEvent
    data object Deleted : DocEvent
}

class DocViewModel(
    private val appContext: Context,
    private val repo: DocRepository,
    private val proStore: ProStore,
    handle: SavedStateHandle,
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    private val ocr = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    private val _events = Channel<DocEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<DocUiState> = combine(repo.observe(id), proStore.state, ocr) { doc, pro, (text, busy) ->
        DocUiState(doc = doc, pro = pro, ocrText = text, ocrBusy = busy, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocUiState())

    fun pdfFile(): File = repo.pdfFile(id)

    fun pageFiles(doc: Doc): List<File> = repo.pageFiles(doc)

    /** Xuất file: bản miễn phí có giới hạn mỗi ngày. */
    fun export(kind: ExportKind) {
        viewModelScope.launch {
            if (proStore.tryConsumeExport(LocalDate.now().toEpochDay())) {
                _events.send(DocEvent.Export(kind))
            } else {
                _events.send(DocEvent.NeedPro(R.string.limit_exports))
            }
        }
    }

    fun rename(input: String) {
        val doc = uiState.value.doc ?: return
        val name = cleanDocName(input) ?: return
        viewModelScope.launch { repo.rename(id, doc, name) }
    }

    fun delete() {
        viewModelScope.launch {
            repo.delete(id)
            _events.send(DocEvent.Deleted)
        }
    }

    /** Nhận dạng chữ trên mọi trang. Chỉ bản Pro. */
    fun recognizeText() {
        val doc = uiState.value.doc ?: return
        viewModelScope.launch {
            if (!proStore.state.first().isPro) {
                _events.send(DocEvent.NeedPro(R.string.limit_pro_ocr))
                return@launch
            }
            ocr.value = null to true
            try {
                ocr.value = TextOcr.recognize(appContext, repo.pageFiles(doc)) to false
            } catch (e: Exception) {
                ocr.value = null to false
                _events.send(DocEvent.Message(R.string.ocr_failed))
            }
        }
    }

    fun dismissText() {
        ocr.value = null to false
    }
}
