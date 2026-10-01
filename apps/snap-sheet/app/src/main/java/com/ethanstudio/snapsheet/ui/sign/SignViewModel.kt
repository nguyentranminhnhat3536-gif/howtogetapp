package com.ethanstudio.snapsheet.ui.sign

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.DEFAULT_PLACEMENT
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.SignatureStore
import com.ethanstudio.snapsheet.data.SignaturePlacement
import com.ethanstudio.snapsheet.data.StrokePoint
import com.ethanstudio.snapsheet.data.appendPoint
import com.ethanstudio.snapsheet.data.clampPlacement
import com.ethanstudio.snapsheet.data.copyName
import com.ethanstudio.snapsheet.data.decodePlacements
import com.ethanstudio.snapsheet.data.encodePlacements
import com.ethanstudio.snapsheet.data.isSignatureUsable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SignUiState(
    val doc: Doc? = null,
    /** Đã đọc xong từ cơ sở dữ liệu chưa (để phân biệt "đang tải" với "tài liệu không còn"). */
    val loaded: Boolean = false,
    /** Cao/rộng của từng trang. */
    val pageAspects: List<Float> = emptyList(),
    /** Trang đang xem, bắt đầu từ 0. */
    val page: Int = 0,
    /** Vị trí chữ ký trên từng trang (khóa = trang, bắt đầu từ 0). */
    val placements: Map<Int, SignaturePlacement> = emptyMap(),
    val hasSignature: Boolean = false,
    /** Cao/rộng của ảnh chữ ký đã lưu. */
    val sigAspect: Float = DEFAULT_SIG_ASPECT,
    /** Tăng sau mỗi lần lưu/xóa chữ ký, để đọc lại ảnh. */
    val signatureVersion: Long = 0,
    /** Đang mở khung vẽ chữ ký. */
    val padOpen: Boolean = false,
    /** Các nét đang vẽ (toạ độ 0..1 theo khung ký). */
    val strokes: List<List<StrokePoint>> = emptyList(),
    val saving: Boolean = false,
) {
    val current: SignaturePlacement? get() = placements[page]
    val canSave: Boolean get() = doc != null && hasSignature && placements.isNotEmpty() && !saving
    val canUseStrokes: Boolean get() = isSignatureUsable(strokes)
}

sealed interface SignEvent {
    data class Done(val id: Long) : SignEvent
    data class Message(@StringRes val res: Int) : SignEvent
    data object NeedPro : SignEvent
    /** Rời màn (hủy khung ký khi chưa có chữ ký nào). */
    data object Leave : SignEvent
}

/** Cao/rộng mặc định của chữ ký khi chưa đọc được ảnh (khung ký 2:1). */
private const val DEFAULT_SIG_ASPECT = 0.5f

/** Cao/rộng khổ A4 dọc, dùng khi không biết tỉ lệ trang. */
private const val FALLBACK_PAGE_ASPECT = 1.414f

/**
 * Màn Ký tên (Pro): vẽ chữ ký một lần, đặt lên một hoặc nhiều trang, kéo để đổi chỗ, thanh trượt để đổi cỡ,
 * rồi lưu thành bản sao. Trang đang xem, các vị trí và trạng thái khung ký nằm trong SavedStateHandle;
 * nét đang vẽ chỉ giữ trong ViewModel (vẫn còn khi xoay màn hình).
 */
class SignViewModel(
    private val repo: DocRepository,
    private val signatures: SignatureStore,
    private val proStore: ProStore,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    private val _uiState = MutableStateFlow(SignUiState())
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()
    private val _events = Channel<SignEvent>(Channel.BUFFERED)
    val events: Flow<SignEvent> = _events.receiveAsFlow()

    val signatureFile: File get() = signatures.file

    init {
        viewModelScope.launch {
            try {
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Không đọc được tài liệu: coi như tài liệu không còn, màn hình tự quay lại.
                _uiState.value = SignUiState(loaded = true)
            }
        }
    }

    private suspend fun load() {
        val doc = repo.observe(id).first()
        if (doc == null || doc.pageCount <= 0) {
            _uiState.value = SignUiState(loaded = true)
            return
        }
        val aspects = repo.pageAspects(doc)
        val hasSignature = withContext(Dispatchers.IO) { signatures.exists() }
        val sigAspect = withContext(Dispatchers.IO) { signatures.aspect() } ?: DEFAULT_SIG_ASPECT
        val page = (handle.get<Int>(KEY_PAGE) ?: 0).coerceIn(0, doc.pageCount - 1)
        val saved = decodePlacements(handle.get<FloatArray>(KEY_PLACEMENTS) ?: FloatArray(0))
        val placements = if (hasSignature) {
            saved.filterKeys { it in 0 until doc.pageCount }
                .mapValues { (index, p) -> clampPlacement(p, sigAspect, aspectOf(aspects, index)) }
        } else {
            emptyMap()
        }
        val padOpen = !hasSignature || (handle.get<Boolean>(KEY_PAD) ?: false)
        _uiState.value = SignUiState(
            doc = doc,
            loaded = true,
            pageAspects = aspects,
            page = page,
            placements = placements,
            hasSignature = hasSignature,
            sigAspect = sigAspect,
            signatureVersion = signatures.version.value,
            padOpen = padOpen,
        )
        handle[KEY_PAD] = padOpen
        savePlacements()
    }

    /** Ảnh của trang [index] (bắt đầu từ 0). */
    fun pageFile(doc: Doc, index: Int): File = File(repo.dir(doc.id), "page_${index + 1}.jpg")

    fun goTo(index: Int) {
        val doc = _uiState.value.doc ?: return
        val page = index.coerceIn(0, doc.pageCount - 1)
        _uiState.update { it.copy(page = page) }
        handle[KEY_PAGE] = page
    }

    /** Mở khung vẽ chữ ký với khung trống. */
    fun openPad() {
        setPad(true)
    }

    /** Hủy khung ký: chưa có chữ ký nào thì rời màn, có rồi thì quay về bước đặt chữ ký. */
    fun cancelPad() {
        if (!_uiState.value.hasSignature) {
            _events.trySend(SignEvent.Leave)
            return
        }
        setPad(false)
    }

    fun startStroke(p: StrokePoint) {
        val point = StrokePoint(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))
        _uiState.update { it.copy(strokes = it.strokes + listOf(listOf(point))) }
    }

    fun extendStroke(p: StrokePoint) {
        _uiState.update { state ->
            val last = state.strokes.lastOrNull() ?: return@update state
            val next = appendPoint(last, p)
            if (next === last) state else state.copy(strokes = state.strokes.dropLast(1) + listOf(next))
        }
    }

    fun clearStrokes() {
        _uiState.update { it.copy(strokes = emptyList()) }
    }

    /**
     * Lưu nét vừa vẽ làm chữ ký. Mọi vị trí đã đặt được kẹp lại theo tỉ lệ của chữ ký mới;
     * trang đang xem chưa có chữ ký thì đặt luôn ở góc dưới phải. Lỗi thì giữ nguyên khung ký.
     */
    fun useStrokes() {
        val state = _uiState.value
        if (!state.canUseStrokes || state.saving) return
        val strokes = state.strokes
        // Bật cờ ngay để lần bấm thứ hai không chạy song song (hai lần ghi cùng một file tạm có thể làm mất chữ ký).
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                signatures.save(strokes)
                val sigAspect = withContext(Dispatchers.IO) { signatures.aspect() } ?: DEFAULT_SIG_ASPECT
                _uiState.update { s ->
                    val refit = s.placements.mapValues { (index, p) -> clampPlacement(p, sigAspect, aspectOf(s.pageAspects, index)) }
                    val placements = if (s.page in refit) {
                        refit
                    } else {
                        refit + (s.page to clampPlacement(DEFAULT_PLACEMENT, sigAspect, aspectOf(s.pageAspects, s.page)))
                    }
                    s.copy(
                        hasSignature = true,
                        sigAspect = sigAspect,
                        signatureVersion = signatures.version.value,
                        padOpen = false,
                        strokes = emptyList(),
                        placements = placements,
                    )
                }
                handle[KEY_PAD] = false
                savePlacements()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(SignEvent.Message(R.string.error_signature_save))
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    /** Xóa chữ ký đã lưu: bỏ luôn các vị trí đã đặt và mở khung ký. Đang lưu thì bỏ qua. */
    fun deleteSignature() {
        if (_uiState.value.saving) return
        // Bật cờ để "Dùng chữ ký này" không chạy cùng lúc với việc xóa (và ngược lại).
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                try {
                    signatures.delete()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Bỏ qua: lần lưu chữ ký sau sẽ ghi đè file cũ.
                }
                _uiState.update {
                    it.copy(
                        hasSignature = false,
                        placements = emptyMap(),
                        padOpen = true,
                        strokes = emptyList(),
                        signatureVersion = signatures.version.value,
                    )
                }
                handle[KEY_PAD] = true
                savePlacements()
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    /** Đặt chữ ký vào trang đang xem (ở góc dưới phải). Chưa có chữ ký thì mở khung ký. */
    fun placeOnCurrent() {
        val state = _uiState.value
        if (state.doc == null) return
        if (!state.hasSignature) {
            openPad()
            return
        }
        val placement = clampPlacement(DEFAULT_PLACEMENT, state.sigAspect, aspectOf(state.pageAspects, state.page))
        setPlacements(state.placements + (state.page to placement))
    }

    fun removeFromCurrent() {
        val state = _uiState.value
        setPlacements(state.placements - state.page)
    }

    /** Dời chữ ký ở trang đang xem; [dx], [dy] theo tỉ lệ khung trang. */
    fun moveBy(dx: Float, dy: Float) {
        if (dx.isNaN() || dy.isNaN()) return
        val state = _uiState.value
        val current = state.current ?: return
        val moved = current.copy(cx = current.cx + dx, cy = current.cy + dy)
        setPlacements(state.placements + (state.page to clampPlacement(moved, state.sigAspect, aspectOf(state.pageAspects, state.page))))
    }

    /** Đổi cỡ chữ ký ở trang đang xem ([widthFraction] = bề rộng chữ ký / bề rộng trang). */
    fun setSize(widthFraction: Float) {
        if (widthFraction.isNaN()) return
        val state = _uiState.value
        val current = state.current ?: return
        val resized = current.copy(widthFraction = widthFraction)
        setPlacements(state.placements + (state.page to clampPlacement(resized, state.sigAspect, aspectOf(state.pageAspects, state.page))))
    }

    /**
     * Lưu bản sao đã ký. [nameTemplate] có "%1$s" (ví dụ "%1$s (signed)") để đặt tên bản sao.
     * Kiểm lại Pro ngay lúc bấm: hết Pro thì mở màn mua, không lưu. File chữ ký bị mất thì mở lại khung ký.
     */
    fun save(nameTemplate: String) {
        val state = _uiState.value
        val doc = state.doc ?: return
        if (!state.canSave) return
        val placements = state.placements
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                when {
                    !proStore.state.first().isPro -> _events.send(SignEvent.NeedPro)
                    !withContext(Dispatchers.IO) { signatures.exists() } -> {
                        _uiState.update { it.copy(hasSignature = false) }
                        openPad()
                    }
                    else -> {
                        val newId = repo.signCopy(doc.id, placements, signatures.file, copyName(doc.name, nameTemplate))
                        _events.send(SignEvent.Done(newId))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(SignEvent.Message(R.string.error_sign))
            } finally {
                _uiState.update { it.copy(saving = false) }
            }
        }
    }

    private fun setPad(open: Boolean) {
        _uiState.update { it.copy(padOpen = open, strokes = emptyList()) }
        handle[KEY_PAD] = open
    }

    private fun setPlacements(placements: Map<Int, SignaturePlacement>) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(placements = placements) }
        savePlacements()
    }

    private fun savePlacements() {
        handle[KEY_PLACEMENTS] = encodePlacements(_uiState.value.placements)
    }

    private fun aspectOf(aspects: List<Float>, index: Int): Float =
        aspects.getOrNull(index)?.takeIf { it > 0f } ?: FALLBACK_PAGE_ASPECT

    private companion object {
        const val KEY_PAGE = "page"
        const val KEY_PAD = "pad"
        const val KEY_PLACEMENTS = "placements"
    }
}
