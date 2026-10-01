package com.ethanstudio.snapsheet.ui.doc

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.CompressLevel
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.Folder
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.data.ProState
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.cleanDocName
import com.ethanstudio.snapsheet.data.shouldShareOriginal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

enum class ExportKind { OPEN_PDF, SHARE_PDF, SHARE_IMAGES, PRINT }

data class DocUiState(
    val doc: Doc? = null,
    val pro: ProState = ProState(),
    /** Đã đọc xong từ cơ sở dữ liệu chưa (để phân biệt "đang tải" với "tài liệu không còn"). */
    val loaded: Boolean = false,
    /** Người dùng vừa xóa tài liệu: đang quay về, không hiện thông báo "tài liệu không còn". */
    val deleted: Boolean = false,
    val folders: List<Folder> = emptyList(),
    /** Đang ghi (thêm trang / nén / chuyển): hiện lớp phủ với chữ này, khóa nút. null = rảnh. */
    @StringRes val working: Int? = null,
    /** Tăng sau mỗi lần trang được ghi lại, để ảnh trang được đọc lại. */
    val revision: Long = 0,
) {
    val exportsLeft: Int
        get() = FreeLimits.remaining(pro.isPro, pro.usage, LocalDate.now().toEpochDay())

    /** Số trang còn được thêm vào tài liệu này theo gói hiện tại (5 miễn phí / 30 Pro). */
    val pagesCanAdd: Int
        get() = FreeLimits.pagesCanAdd(pro.isPro, doc?.pageCount ?: 0)
}

sealed interface DocEvent {
    data class Export(val kind: ExportKind) : DocEvent
    /** Hết lượt xuất miễn phí hoặc cần Pro: mở màn mua. */
    data class NeedPro(@StringRes val reason: Int) : DocEvent
    data class Message(@StringRes val res: Int) : DocEvent
    /** Thông báo có tham số (chuỗi có %1$s, %1$d…). */
    data class MessageArgs(@StringRes val res: Int, val args: List<Any>) : DocEvent
    data object Deleted : DocEvent
    /** Chia sẻ file nén; [before]/[after] (byte) để báo "2.1 MB → 640 KB"; [noGain] = đã gửi bản gốc. */
    data class ShareCompressed(val file: File, val before: Long, val after: Long, val noGain: Boolean) : DocEvent
    /** Đã chuyển vào thư mục [folderName]; null = đã bỏ khỏi thư mục. */
    data class Moved(val folderName: String?) : DocEvent
    /** Bản Pro: tài liệu đã đủ số trang tối đa. */
    data object PageLimitPro : DocEvent
}

class DocViewModel(
    private val repo: DocRepository,
    private val proStore: ProStore,
    handle: SavedStateHandle,
) : ViewModel() {
    /** Trạng thái chỉ có trong ViewModel (không đọc từ cơ sở dữ liệu). */
    private data class Local(val deleted: Boolean = false, @StringRes val working: Int? = null)

    private val id: Long = handle.get<Long>("id") ?: -1L
    private val local = MutableStateFlow(Local())
    private val _events = Channel<DocEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<DocUiState> =
        combine(repo.observe(id), proStore.state, local, repo.folders, repo.pageVersion) { doc, pro, loc, folders, version ->
            DocUiState(
                doc = doc,
                pro = pro,
                loaded = true,
                deleted = loc.deleted,
                folders = folders,
                working = loc.working,
                revision = version,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocUiState())

    fun pdfFile(): File = repo.pdfFile(id)

    fun pageFiles(doc: Doc): List<File> = repo.pageFiles(doc)

    /** Xuất file: bản miễn phí có giới hạn mỗi ngày. In cũng tính là một lượt. */
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
        local.update { it.copy(deleted = true) }
        viewModelScope.launch {
            try {
                repo.delete(id)
                _events.send(DocEvent.Deleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                local.update { it.copy(deleted = false) }
                _events.send(DocEvent.Message(R.string.error_delete))
            }
        }
    }

    /**
     * Bấm "Thêm trang": true nếu còn được thêm (giao diện mở bảng chọn nguồn).
     * Hết chỗ: bản miễn phí mở màn mua, bản Pro báo đã đủ số trang tối đa.
     */
    fun requestAddPages(): Boolean {
        val state = uiState.value
        if (state.doc == null || state.working != null) return false
        if (state.pagesCanAdd > 0) return true
        _events.trySend(if (state.pro.isPro) DocEvent.PageLimitPro else DocEvent.NeedPro(R.string.limit_pages_free))
        return false
    }

    /** Nối các trang vừa quét vào cuối tài liệu (cắt theo số trang còn được thêm). */
    fun addScanned(uris: List<Uri>) {
        addPages(uris) { repo.addScannedPages(id, it) }
    }

    /** Nối các ảnh vừa chọn vào cuối tài liệu (cắt theo số trang còn được thêm). */
    fun addPhotos(uris: List<Uri>) {
        addPages(uris) { repo.addImages(id, it) }
    }

    private fun addPages(uris: List<Uri>, add: suspend (List<Uri>) -> Int) {
        if (uris.isEmpty()) return
        runWork(R.string.saving, R.string.error_edit) {
            // Đợi dữ liệu thật (kết quả quét có thể về trước khi màn hình đọc xong cơ sở dữ liệu).
            val pages = uris.take(uiState.first { it.loaded }.pagesCanAdd)
            if (pages.isEmpty()) return@runWork
            add(pages)
            _events.send(DocEvent.Message(R.string.pages_added))
        }
    }

    /**
     * Nén rồi chia sẻ. Gốc: chia sẻ nguyên PDF như nút "Chia sẻ PDF". Nhỏ/Vừa: hết lượt thì mở màn mua
     * trước khi nén; bản nén không nhỏ hơn thì gửi bản gốc.
     */
    fun compressAndShare(level: CompressLevel) {
        if (level == CompressLevel.ORIGINAL) {
            export(ExportKind.SHARE_PDF)
            return
        }
        val state = uiState.value
        if (!state.pro.isPro && state.exportsLeft == 0) {
            _events.trySend(DocEvent.NeedPro(R.string.limit_exports))
            return
        }
        runWork(R.string.compress_working, R.string.error_compress) {
            val original = repo.pdfFile(id)
            val compressed = repo.compressed(id, level)
            val before = withContext(Dispatchers.IO) { original.length() }
            val after = withContext(Dispatchers.IO) { compressed.length() }
            if (!proStore.tryConsumeExport(LocalDate.now().toEpochDay())) {
                _events.send(DocEvent.NeedPro(R.string.limit_exports))
                return@runWork
            }
            // PDF gốc bị mất (dung lượng 0) thì vẫn gửi bản nén.
            val noGain = before > 0 && shouldShareOriginal(before, after)
            _events.send(DocEvent.ShareCompressed(if (noGain) original else compressed, before, after, noGain))
        }
    }

    /** Chuyển tài liệu vào thư mục [folderId]; null = bỏ khỏi thư mục. */
    fun moveTo(folderId: Long?) {
        val name = folderId?.let { fid -> uiState.value.folders.firstOrNull { it.id == fid }?.name }
        if (folderId != null && name == null) return // Thư mục vừa bị xóa.
        runWork(R.string.saving, R.string.err_unknown) {
            repo.moveToFolder(listOf(id), folderId)
            _events.send(DocEvent.Moved(name))
        }
    }

    /** Tạo thư mục mới (tên đã kiểm ở hộp thoại) rồi chuyển tài liệu vào đó. */
    fun createFolderAndMove(name: String) {
        runWork(R.string.saving, R.string.err_unknown) {
            val folderId = repo.createFolder(name)
            repo.moveToFolder(listOf(id), folderId)
            _events.send(DocEvent.Moved(name))
        }
    }

    /** Chạy một việc ghi: hiện lớp phủ [label], lỗi thì báo [error]. Đang có việc khác thì bỏ qua. */
    private fun runWork(@StringRes label: Int, @StringRes error: Int, block: suspend () -> Unit) {
        if (local.value.working != null) return
        local.update { it.copy(working = label) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(DocEvent.Message(error))
            } finally {
                local.update { it.copy(working = null) }
            }
        }
    }
}
