package com.ethanstudio.snapsheet.ui.watermark

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.WATERMARK_MAX_CHARS
import com.ethanstudio.snapsheet.data.WatermarkColor
import com.ethanstudio.snapsheet.data.WatermarkSpec
import com.ethanstudio.snapsheet.data.WatermarkStrength
import com.ethanstudio.snapsheet.data.cleanWatermarkText
import com.ethanstudio.snapsheet.data.copyName
import com.ethanstudio.snapsheet.data.takeSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class WatermarkUiState(
    val doc: Doc? = null,
    /** Đã đọc xong từ cơ sở dữ liệu chưa (để phân biệt "đang tải" với "tài liệu không còn"). */
    val loaded: Boolean = false,
    val text: String = "",
    val color: WatermarkColor = WatermarkColor.GRAY,
    val strength: WatermarkStrength = WatermarkStrength.MEDIUM,
    val saving: Boolean = false,
) {
    /** Chữ mờ sẽ in; chữ rỗng → null (nút Lưu tắt). */
    val spec: WatermarkSpec? get() = cleanWatermarkText(text)?.let { WatermarkSpec(it, color, strength) }
    val canSave: Boolean get() = doc != null && spec != null && !saving
}

sealed interface WatermarkEvent {
    data class Done(val id: Long) : WatermarkEvent
    data class Message(@StringRes val res: Int) : WatermarkEvent
    data object NeedPro : WatermarkEvent
}

/** Màn Chèn chữ mờ (Pro): chọn chữ, màu, độ đậm rồi lưu thành bản sao. Lựa chọn giữ trong SavedStateHandle. */
class WatermarkViewModel(
    private val repo: DocRepository,
    private val proStore: ProStore,
    private val handle: SavedStateHandle,
) : ViewModel() {
    /** Phần người dùng chọn trên màn hình (không đọc từ cơ sở dữ liệu). */
    private data class Form(
        val text: String,
        val color: WatermarkColor,
        val strength: WatermarkStrength,
        val saving: Boolean = false,
    )

    private val id: Long = handle.get<Long>("id") ?: -1L
    private val form = MutableStateFlow(
        Form(
            text = handle.get<String>(KEY_TEXT).orEmpty(),
            color = handle.get<String>(KEY_COLOR)?.let { name -> WatermarkColor.entries.firstOrNull { it.name == name } }
                ?: WatermarkColor.GRAY,
            strength = handle.get<String>(KEY_STRENGTH)?.let { name -> WatermarkStrength.entries.firstOrNull { it.name == name } }
                ?: WatermarkStrength.MEDIUM,
        ),
    )
    private val _events = Channel<WatermarkEvent>(Channel.BUFFERED)
    val events: Flow<WatermarkEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<WatermarkUiState> =
        combine(repo.observe(id), form) { doc, f ->
            WatermarkUiState(
                doc = doc,
                loaded = true,
                text = f.text,
                color = f.color,
                strength = f.strength,
                saving = f.saving,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatermarkUiState())

    /** Ảnh trang đầu, dùng cho ô xem trước. */
    fun firstPage(doc: Doc): File = File(repo.dir(doc.id), "page_1.jpg")

    fun setText(value: String) {
        val text = takeSafe(value, WATERMARK_MAX_CHARS)
        form.update { it.copy(text = text) }
        handle[KEY_TEXT] = text
    }

    fun setColor(color: WatermarkColor) {
        form.update { it.copy(color = color) }
        handle[KEY_COLOR] = color.name
    }

    fun setStrength(strength: WatermarkStrength) {
        form.update { it.copy(strength = strength) }
        handle[KEY_STRENGTH] = strength.name
    }

    /**
     * Lưu bản sao có chữ mờ. [nameTemplate] có "%1$s" (ví dụ "%1$s (watermark)") để đặt tên bản sao.
     * Kiểm lại Pro ngay lúc bấm: hết Pro thì mở màn mua, không lưu.
     */
    fun save(nameTemplate: String) {
        val state = uiState.value
        val doc = state.doc ?: return
        val spec = state.spec ?: return
        if (form.value.saving || !state.canSave) return
        form.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                if (!proStore.state.first().isPro) {
                    _events.send(WatermarkEvent.NeedPro)
                    return@launch
                }
                val newId = repo.watermarkCopy(doc.id, spec, copyName(doc.name, nameTemplate))
                _events.send(WatermarkEvent.Done(newId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(WatermarkEvent.Message(R.string.error_watermark))
            } finally {
                form.update { it.copy(saving = false) }
            }
        }
    }

    private companion object {
        const val KEY_TEXT = "text"
        const val KEY_COLOR = "color"
        const val KEY_STRENGTH = "strength"
    }
}
