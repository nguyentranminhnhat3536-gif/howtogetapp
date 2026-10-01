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
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.data.ProState
import com.ethanstudio.snapsheet.data.ProStore
import com.ethanstudio.snapsheet.data.defaultDocName
import com.ethanstudio.snapsheet.data.filterDocs
import com.ethanstudio.snapsheet.data.sortDocs
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MainUiState(
    val allDocs: List<Doc> = emptyList(),
    val docs: List<Doc> = emptyList(),
    val query: String = "",
    /** Cách sắp xếp ở tab Files (chỉ áp cho [docs]; [allDocs] luôn mới nhất trước). */
    val sort: DocSort = DocSort.NEWEST,
    val pro: ProState = ProState(),
    val saving: Boolean = false,
) {
    val exportsLeft: Int
        get() = FreeLimits.remaining(pro.isPro, pro.usage, LocalDate.now().toEpochDay())
}

sealed interface MainEvent {
    /** Mở tài liệu vừa lưu; [extract] = mở tiếp màn lấy chữ (quét từ "Scan a new page"). */
    data class OpenDoc(val id: Long, val extract: Boolean = false) : MainEvent
    data class Message(@StringRes val res: Int) : MainEvent
    data object PurchaseDone : MainEvent
}

/** Trạng thái chung của các tab: danh sách tài liệu, tìm kiếm, Pro, lưu bản quét, thanh toán. */
class MainViewModel(
    private val repo: DocRepository,
    private val proStore: ProStore,
    private val billing: BillingManager,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(DocSort.NEWEST)
    private val saving = MutableStateFlow(false)
    /** Lần quét tới là để lấy chữ: lưu xong thì mở luôn màn Extract text. */
    private var ocrAfterScan = false
    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val billingState = billing.state

    val uiState: StateFlow<MainUiState> = combine(repo.docs, query, sort, proStore.state, saving) { docs, q, order, pro, busy ->
        MainUiState(allDocs = docs, docs = sortDocs(filterDocs(docs, q), order), query = q, sort = order, pro = pro, saving = busy)
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
        query.value = value
    }

    fun setSort(value: DocSort) {
        sort.value = value
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

    private fun save(extract: Boolean, block: suspend () -> Long) {
        viewModelScope.launch {
            saving.value = true
            try {
                _events.send(MainEvent.OpenDoc(block(), extract))
            } catch (e: Exception) {
                _events.send(MainEvent.Message(R.string.error_save))
            } finally {
                saving.value = false
            }
        }
    }

    fun message(@StringRes res: Int) {
        _events.trySend(MainEvent.Message(res))
    }

    fun buy(activity: android.app.Activity, plan: com.ethanstudio.snapsheet.billing.Plan) = billing.launch(activity, plan)

    fun restore() = billing.restore()
}
