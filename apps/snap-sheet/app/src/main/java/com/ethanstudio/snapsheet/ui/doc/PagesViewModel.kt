package com.ethanstudio.snapsheet.ui.doc

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.identityOrder
import com.ethanstudio.snapsheet.data.isValidOrder
import com.ethanstudio.snapsheet.data.moveDown as orderMoveDown
import com.ethanstudio.snapsheet.data.moveUp as orderMoveUp
import com.ethanstudio.snapsheet.data.removePage as orderRemovePage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PagesUiState(
    val doc: Doc? = null,
    val loaded: Boolean = false,
    /** Số trang GỐC (bắt đầu từ 1) theo thứ tự mới. */
    val order: List<Int> = emptyList(),
    val saving: Boolean = false,
) {
    /** Có thay đổi chưa lưu (đổi thứ tự hoặc bỏ trang). */
    val dirty: Boolean
        get() = doc != null && order != identityOrder(doc.pageCount)
}

sealed interface PagesEvent {
    data object Saved : PagesEvent
    data class Message(@StringRes val res: Int) : PagesEvent
}

/** Màn Sửa trang: đổi thứ tự, bỏ trang, rồi lưu một lần. Bản nháp giữ trong SavedStateHandle nên xoay màn hình không mất. */
class PagesViewModel(
    private val repo: DocRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    private val _uiState = MutableStateFlow(PagesUiState())
    val uiState: StateFlow<PagesUiState> = _uiState.asStateFlow()
    private val _events = Channel<PagesEvent>(Channel.BUFFERED)
    val events: Flow<PagesEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val doc = repo.observe(id).first()
            val draft = handle.get<IntArray>(KEY_ORDER)?.toList()
            val order = when {
                doc == null -> emptyList<Int>()
                draft != null && isValidOrder(draft, doc.pageCount) -> draft
                else -> identityOrder(doc.pageCount)
            }
            _uiState.value = PagesUiState(doc = doc, loaded = true, order = order)
        }
    }

    /** File ảnh của trang gốc số [page] (bắt đầu từ 1). */
    fun pageFile(doc: Doc, page: Int): File = File(repo.dir(doc.id), "page_$page.jpg")

    fun moveUp(index: Int) {
        setOrder(orderMoveUp(_uiState.value.order, index))
    }

    fun moveDown(index: Int) {
        setOrder(orderMoveDown(_uiState.value.order, index))
    }

    /** Bỏ trang ở vị trí [index]. Tài liệu phải còn ít nhất một trang. */
    fun remove(index: Int) {
        val order = _uiState.value.order
        if (order.size <= 1) {
            _events.trySend(PagesEvent.Message(R.string.pages_last_page))
            return
        }
        setOrder(orderRemovePage(order, index))
    }

    /** Lưu thứ tự mới. Không có thay đổi thì đóng luôn. Lỗi thì bản gốc còn nguyên và báo lỗi. */
    fun save() {
        val state = _uiState.value
        val doc = state.doc ?: return
        if (state.saving) return
        if (!state.dirty) {
            _events.trySend(PagesEvent.Saved)
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repo.editPages(doc.id, state.order)
                handle.remove<IntArray>(KEY_ORDER)
                _events.send(PagesEvent.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(PagesEvent.Message(R.string.error_edit))
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    private fun setOrder(order: List<Int>) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(order = order) }
        handle[KEY_ORDER] = order.toIntArray()
    }

    private companion object {
        const val KEY_ORDER = "order"
    }
}
