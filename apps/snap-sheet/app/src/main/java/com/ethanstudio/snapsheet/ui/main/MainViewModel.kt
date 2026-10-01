package com.ethanstudio.snapsheet.ui.main

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.billing.BillingEvent
import com.ethanstudio.snapsheet.billing.BillingManager
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.DocSort
import com.ethanstudio.snapsheet.data.DocRepository
import com.ethanstudio.snapsheet.data.Folder
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.data.MergeCheck
import com.ethanstudio.snapsheet.data.ProState
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.checkMerge
import com.ethanstudio.snapsheet.data.defaultDocName
import com.ethanstudio.snapsheet.data.filterByFolder
import com.ethanstudio.snapsheet.data.filterDocs
import com.ethanstudio.snapsheet.data.orderedSelection
import com.ethanstudio.snapsheet.data.pruneSelection
import com.ethanstudio.snapsheet.data.sortDocs
import com.ethanstudio.snapsheet.data.toggleSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

data class MainUiState(
    val allDocs: List<Doc> = emptyList(),
    val docs: List<Doc> = emptyList(),
    val query: String = "",
    /** Cách sắp xếp ở tab Files (chỉ áp cho [docs]; [allDocs] luôn mới nhất trước). */
    val sort: DocSort = DocSort.NEWEST,
    val pro: ProState = ProState(),
    val saving: Boolean = false,
    val folders: List<Folder> = emptyList(),
    /** Thư mục đang lọc ở tab Files; null = "All". Thư mục đã bị xóa thì coi như null. */
    val folderFilter: Long? = null,
    /** Đang ở chế độ chọn nhiều trong tab Files. */
    val selecting: Boolean = false,
    /** id tài liệu đã chọn, theo thứ tự chạm chọn (dùng làm thứ tự gộp). */
    val selection: List<Long> = emptyList(),
) {
    val exportsLeft: Int
        get() = FreeLimits.remaining(pro.isPro, pro.usage, LocalDate.now().toEpochDay())
}

sealed interface MainEvent {
    /** Mở tài liệu vừa lưu; [extract] = mở tiếp màn lấy chữ (quét từ "Scan a new page"). */
    data class OpenDoc(val id: Long, val extract: Boolean = false) : MainEvent
    data class Message(@StringRes val res: Int) : MainEvent
    /** Thông báo có tham số (chuỗi có %1$s, %1$d…). */
    data class MessageArgs(@StringRes val res: Int, val args: List<Any>) : MainEvent
    data object PurchaseDone : MainEvent
    /** Chia sẻ nhiều file PDF cùng lúc. */
    data class ShareFiles(val files: List<File>) : MainEvent
    /** Cần Pro: [reason] có thì báo trước, rồi mở màn mua; null = chỉ mở màn mua. */
    data class NeedPro(@StringRes val reason: Int?) : MainEvent
    /** Đã chuyển vào thư mục [folderName]; null = đã bỏ khỏi thư mục. */
    data class Moved(val folderName: String?) : MainEvent
}

/** Trạng thái chung của các tab: danh sách tài liệu, tìm kiếm, Pro, lưu bản quét, thanh toán. */
class MainViewModel(
    private val repo: DocRepository,
    private val proStore: ProStore,
    private val billing: BillingManager,
) : ViewModel() {
    /** Trạng thái riêng của tab Files (gom vào một luồng để combine không vượt quá 5 luồng). */
    private data class FilesUi(
        val query: String = "",
        val sort: DocSort = DocSort.NEWEST,
        val folderFilter: Long? = null,
        val selecting: Boolean = false,
        val selection: List<Long> = emptyList(),
        val saving: Boolean = false,
    )

    private val filesUi = MutableStateFlow(FilesUi())
    /** Lần quét tới là để lấy chữ: lưu xong thì mở luôn màn Extract text. */
    private var ocrAfterScan = false
    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val billingState = billing.state

    val uiState: StateFlow<MainUiState> = combine(repo.docs, repo.folders, proStore.state, filesUi) { docs, folders, pro, ui ->
        val filter = ui.folderFilter?.takeIf { id -> folders.any { it.id == id } }
        MainUiState(
            allDocs = docs,
            docs = sortDocs(filterByFolder(filterDocs(docs, ui.query), filter), ui.sort),
            query = ui.query,
            sort = ui.sort,
            pro = pro,
            saving = ui.saving,
            folders = folders,
            folderFilter = filter,
            selecting = ui.selecting,
            selection = pruneSelection(ui.selection, docs.mapTo(HashSet()) { it.id }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            billing.events.collect { event ->
                when (event) {
                    BillingEvent.PURCHASED -> {
                        _events.send(MainEvent.Message(R.string.billing_purchased))
                        _events.send(MainEvent.PurchaseDone)
                    }
                    BillingEvent.CANCELED -> _events.send(MainEvent.Message(R.string.billing_canceled))
                    BillingEvent.FAILED -> _events.send(MainEvent.Message(R.string.billing_failed))
                    BillingEvent.UNAVAILABLE -> _events.send(MainEvent.Message(R.string.billing_unavailable))
                    BillingEvent.RESTORED -> {
                        _events.send(MainEvent.Message(R.string.billing_restored))
                        _events.send(MainEvent.PurchaseDone)
                    }
                    BillingEvent.NOTHING_TO_RESTORE -> _events.send(MainEvent.Message(R.string.billing_nothing))
                }
            }
        }
    }

    fun setQuery(value: String) {
        filesUi.update { it.copy(query = value) }
    }

    fun setSort(value: DocSort) {
        filesUi.update { it.copy(sort = value) }
    }

    /** Lọc tab Files theo thư mục; null = tất cả. */
    fun setFolderFilter(id: Long?) {
        filesUi.update { it.copy(folderFilter = id) }
    }

    fun startSelection() {
        filesUi.update { it.copy(selecting = true) }
    }

    fun clearSelection() {
        filesUi.update { it.copy(selecting = false, selection = emptyList()) }
    }

    fun toggleSelect(id: Long) {
        filesUi.update { it.copy(selecting = true, selection = toggleSelection(it.selection, id)) }
    }

    /** Chia sẻ PDF của các tài liệu đã chọn. Bản miễn phí: mỗi tài liệu một lượt xuất, không đủ thì chặn cả lần. */
    fun shareSelected() {
        val state = uiState.value
        val docs = orderedSelection(state.selection, state.allDocs)
        if (docs.isEmpty()) return
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { docs.map { repo.pdfFile(it.id) }.filter { it.isFile } }
            when {
                files.isEmpty() -> _events.send(MainEvent.Message(R.string.error_open))
                proStore.tryConsumeExports(files.size, LocalDate.now().toEpochDay()) -> {
                    _events.send(MainEvent.ShareFiles(files))
                    clearSelection()
                }
                else -> _events.send(MainEvent.NeedPro(R.string.limit_exports_many))
            }
        }
    }

    /** Xóa các tài liệu đã chọn (đã hỏi lại ở hộp thoại). */
    fun deleteSelected() {
        val ids = uiState.value.selection
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                repo.deleteMany(ids)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(MainEvent.Message(R.string.error_delete))
            }
            clearSelection()
        }
    }

    /** Chuyển các tài liệu đã chọn vào thư mục [folderId]; null = bỏ khỏi thư mục. */
    fun moveSelected(folderId: Long?) {
        val state = uiState.value
        val ids = state.selection
        if (ids.isEmpty()) return
        val name = folderId?.let { fid -> state.folders.firstOrNull { it.id == fid }?.name }
        if (folderId != null && name == null) return // Thư mục vừa bị xóa.
        launchFolderWork {
            repo.moveToFolder(ids, folderId)
            clearSelection()
            _events.send(MainEvent.Moved(name))
        }
    }

    /** Tạo thư mục mới (tên đã kiểm ở hộp thoại) rồi chuyển các tài liệu đã chọn vào đó. */
    fun createFolderAndMoveSelected(name: String) {
        val ids = uiState.value.selection
        if (ids.isEmpty()) return
        launchFolderWork {
            val folderId = repo.createFolder(name)
            repo.moveToFolder(ids, folderId)
            clearSelection()
            _events.send(MainEvent.Moved(name))
        }
    }

    /** Gộp các tài liệu đã chọn (theo thứ tự chọn) thành một tài liệu mới rồi mở nó. Chỉ bản Pro. */
    fun mergeSelected(namePrefix: String) {
        val state = uiState.value
        if (state.saving) return
        if (!state.pro.isPro) {
            _events.trySend(MainEvent.NeedPro(null))
            return
        }
        val docs = orderedSelection(state.selection, state.allDocs)
        when (checkMerge(docs)) {
            MergeCheck.NeedTwo -> _events.trySend(MainEvent.Message(R.string.merge_need_two))
            is MergeCheck.TooMany -> _events.trySend(MainEvent.MessageArgs(R.string.merge_too_many, listOf(FreeLimits.PRO_PAGES)))
            is MergeCheck.Ok -> save(extract = false, error = R.string.error_merge) {
                repo.merge(docs.map { it.id }, defaultDocName(namePrefix, System.currentTimeMillis())).also { clearSelection() }
            }
        }
    }

    /** Tạo thư mục (tên đã kiểm ở hộp thoại) và chuyển bộ lọc sang thư mục đó. */
    fun createFolder(name: String) {
        launchFolderWork {
            val id = repo.createFolder(name)
            filesUi.update { it.copy(folderFilter = id) }
        }
    }

    fun renameFolder(id: Long, name: String) {
        launchFolderWork { repo.renameFolder(id, name) }
    }

    /** Xóa thư mục; tài liệu bên trong được giữ lại. Đang lọc theo thư mục này thì quay về "All". */
    fun deleteFolder(id: Long) {
        launchFolderWork {
            repo.deleteFolder(id)
            filesUi.update { if (it.folderFilter == id) it.copy(folderFilter = null) else it }
        }
    }

    /** Việc với thư mục: lỗi thì báo chung "có lỗi, thử lại". */
    private fun launchFolderWork(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(MainEvent.Message(R.string.err_unknown))
            }
        }
    }

    /** Lưu kết quả của trình quét rồi mở tài liệu vừa tạo. */
    fun saveScan(pages: List<Uri>, pdf: Uri?, namePrefix: String) {
        val extract = ocrAfterScan
        ocrAfterScan = false
        save(extract) {
            repo.saveScan(pages, pdf, defaultDocName(namePrefix, System.currentTimeMillis()))
        }
    }

    /** Lưu các ảnh chọn từ thư viện thành một PDF. */
    fun savePhotos(uris: List<Uri>, namePrefix: String) {
        ocrAfterScan = false
        save(extract = false) {
            repo.saveImages(uris, defaultDocName(namePrefix, System.currentTimeMillis()))
        }
    }

    /** Đánh dấu lần quét tới là để lấy chữ. */
    fun scanForOcr() {
        ocrAfterScan = true
    }

    /** Bỏ đánh dấu (người dùng hủy quét hoặc quét lỗi). */
    fun clearOcrAfterScan() {
        ocrAfterScan = false
    }

    private fun save(extract: Boolean, @StringRes error: Int = R.string.error_save, block: suspend () -> Long) {
        viewModelScope.launch {
            filesUi.update { it.copy(saving = true) }
            try {
                _events.send(MainEvent.OpenDoc(block(), extract))
            } catch (e: Exception) {
                _events.send(MainEvent.Message(error))
            } finally {
                filesUi.update { it.copy(saving = false) }
            }
        }
    }

    fun message(@StringRes res: Int) {
        _events.trySend(MainEvent.Message(res))
    }

    fun buy(activity: android.app.Activity, plan: com.ethanstudio.snapsheet.billing.Plan) = billing.launch(activity, plan)

    fun restore() = billing.restore()
}
