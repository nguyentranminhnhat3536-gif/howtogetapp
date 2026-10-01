package com.ethanstudio.snapsheet.ui.ocr

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.ocr.TextOcr
import com.ethanstudio.snapsheet.ocr.displayText
import com.ethanstudio.snapsheet.ocr.txtFileName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Giai đoạn của màn lấy chữ. */
sealed interface OcrPhase {
    /** Đang đọc cơ sở dữ liệu hoặc trạng thái Pro. */
    data object Loading : OcrPhase
    /** Chưa có Pro: không chạy OCR, chỉ hiện bảng khóa. */
    data object Locked : OcrPhase
    /** Tài liệu không còn. */
    data object Missing : OcrPhase
    /** Đang đọc trang [page] (tính từ 1) trên tổng [total]. */
    data class Reading(val page: Int, val total: Int, val pageFile: File?) : OcrPhase
    data class Done(val pages: List<String>) : OcrPhase
    /** Mọi trang đều không có chữ. */
    data object Empty : OcrPhase
    data object Failed : OcrPhase
}

data class OcrUiState(
    val phase: OcrPhase = OcrPhase.Loading,
    val docName: String = "",
    /** 0 = mọi trang, n = trang n. */
    val selected: Int = 0,
    val query: String = "",
    val joined: Boolean = false,
    /** Chữ đang hiển thị (đã chọn trang, đã nối dòng nếu bật). Chỉ có khi [phase] là Done. */
    val text: String = "",
)

sealed interface OcrEvent {
    data object Close : OcrEvent
    data class Message(@StringRes val res: Int) : OcrEvent
}

/** Lấy chữ từ mọi trang của một tài liệu (chỉ bản Pro), có tiến độ, hủy được, tìm, nối dòng, lưu .txt. */
class OcrViewModel(
    private val appContext: Context,
    private val repo: DocRepository,
    private val proStore: ProStore,
    handle: SavedStateHandle,
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()
    private val _events = Channel<OcrEvent>(Channel.BUFFERED)
    val events: Flow<OcrEvent> = _events.receiveAsFlow()

    private var files: List<File> = emptyList()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            val doc = repo.observe(id).first()
            if (doc == null) {
                _uiState.update { it.copy(phase = OcrPhase.Missing) }
                return@launch
            }
            files = repo.pageFiles(doc)
            _uiState.update { it.copy(docName = doc.name) }
            proStore.state.map { it.isPro }.distinctUntilChanged().collect { pro -> onProChanged(pro) }
        }
    }

    private fun onProChanged(pro: Boolean) {
        val phase = _uiState.value.phase
        when {
            !pro && phase == OcrPhase.Loading -> _uiState.update { it.copy(phase = OcrPhase.Locked) }
            pro && (phase == OcrPhase.Loading || phase == OcrPhase.Locked) -> start()
        }
    }

    private fun start() {
        job?.cancel()
        val total = files.size
        if (total == 0) {
            _uiState.update { it.copy(phase = OcrPhase.Empty) }
            return
        }
        job = viewModelScope.launch {
            setPhase(OcrPhase.Reading(1, total, files.firstOrNull()))
            try {
                val pages = TextOcr.recognizePages(appContext, files) { i -> setPhase(OcrPhase.Reading(i + 1, total, files[i])) }
                val phase = if (pages.all { it.isBlank() }) OcrPhase.Empty else OcrPhase.Done(pages)
                _uiState.update { it.copy(selected = 0) }
                setPhase(phase)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setPhase(OcrPhase.Failed)
            }
        }
    }

    /** Hủy lượt đọc và rời màn hình. */
    fun cancel() {
        job?.cancel()
        _events.trySend(OcrEvent.Close)
    }

    /** Đọc lại tài liệu (sau khi lỗi, hoặc nút "Read again"). */
    fun retry() {
        if (_uiState.value.phase !in listOf(OcrPhase.Loading, OcrPhase.Locked, OcrPhase.Missing)) start()
    }

    fun selectPage(index: Int) = updateText { it.copy(selected = index) }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value.take(MAX_QUERY)) }
    }

    fun toggleJoin() = updateText { it.copy(joined = !it.joined) }

    /** Ghi chữ đang hiển thị (UTF-8) vào file người dùng vừa chọn. */
    fun saveTxt(target: Uri) {
        val text = _uiState.value.text
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(target)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        ?: error("no stream")
                }
                _events.send(OcrEvent.Message(R.string.ocr_saved))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(OcrEvent.Message(R.string.ocr_save_failed))
            }
        }
    }

    fun suggestedFileName(fallback: String): String = txtFileName(_uiState.value.docName, fallback)

    private fun setPhase(phase: OcrPhase) = updateText { it.copy(phase = phase) }

    /** Đổi trạng thái rồi tính lại chữ đang hiển thị. */
    private fun updateText(change: (OcrUiState) -> OcrUiState) {
        _uiState.update { old ->
            val next = change(old)
            val phase = next.phase
            next.copy(text = if (phase is OcrPhase.Done) displayText(phase.pages, next.selected, next.joined) else "")
        }
    }

    private companion object {
        const val MAX_QUERY = 100
    }
}
