package com.ethanstudio.snapsheet.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Giá một gói, đã định dạng theo tiền tệ của người dùng (do Google Play trả về). */
data class PlanOffer(
    val plan: Plan,
    val price: String,
    val priceMicros: Long,
    val hasFreeTrial: Boolean,
    internal val details: ProductDetails,
    internal val offerToken: String?,
    /** Số ngày dùng thử miễn phí của ưu đãi đang hiện (ví dụ 7), null nếu không có. */
    val trialDays: Int? = null,
    /** Mã tiền tệ ISO 4217 của giá (ví dụ "USD"), dùng để quy giá năm ra mỗi tháng. */
    val currencyCode: String = "",
)

data class BillingUiState(
    val offers: Map<Plan, PlanOffer> = emptyMap(),
    val loading: Boolean = true,
)

enum class BillingEvent { PURCHASED, CANCELED, FAILED, UNAVAILABLE, RESTORED, NOTHING_TO_RESTORE }

/**
 * Bọc Google Play Billing. Giá lấy từ Play (không gõ cứng). Kết quả mua được ghi qua [onProChanged]
 * để app dùng được cả khi mất mạng.
 */
class BillingManager(
    context: Context,
    private val scope: CoroutineScope,
    private val onProChanged: suspend (Boolean, ProKind?) -> Unit,
) : PurchasesUpdatedListener {

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Kết nối Google Play, tải giá và kiểm tra các lần mua cũ. Gọi một lần khi mở app. */
    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        loadOffers()
                        refreshPurchases()
                    }
                } else {
                    _state.update { it.copy(loading = false) }
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
        // Google Play không trả lời (máy không có Play, mạng chậm): sau 10 giây thôi chờ để màn mua báo rõ.
        scope.launch {
            delay(LOAD_TIMEOUT_MS)
            _state.update { if (it.loading) it.copy(loading = false) else it }
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 10_000L
    }

    /** Hỏi lại Google Play các lần mua (gọi khi app quay lại màn hình). Bỏ qua nếu chưa kết nối. */
    fun refresh() {
        if (client.isReady) scope.launch { refreshPurchases() }
    }

    /** Mở màn thanh toán của Google Play cho [plan]. */
    fun launch(activity: Activity, plan: Plan) {
        val offer = _state.value.offers[plan]
        if (offer == null || !client.isReady) {
            _events.tryEmit(BillingEvent.UNAVAILABLE)
            return
        }
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(offer.details)
        offer.offerToken?.let { params.setOfferToken(it) }
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build()
        val result = client.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK &&
            result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED
        ) {
            _events.tryEmit(BillingEvent.FAILED)
        }
    }

    /** "Restore Purchases": hỏi lại Google Play xem tài khoản này đã mua gì. */
    fun restore() {
        scope.launch {
            val kind = refreshPurchases()
            _events.emit(if (kind != null) BillingEvent.RESTORED else BillingEvent.NOTHING_TO_RESTORE)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                val kind = handle(purchases.orEmpty())
                if (kind != null) _events.emit(BillingEvent.PURCHASED)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(BillingEvent.CANCELED)
            else -> _events.tryEmit(BillingEvent.FAILED)
        }
    }

    private suspend fun loadOffers() {
        val offers = mutableMapOf<Plan, PlanOffer>()
        queryDetails(BillingClient.ProductType.SUBS, SUBSCRIPTION_ID).firstOrNull()?.let { details ->
            for (plan in listOf(Plan.MONTHLY, Plan.YEARLY)) {
                val candidates = details.subscriptionOfferDetails.orEmpty()
                    .filter { it.basePlanId == plan.basePlanId }
                val infos = candidates.map { o ->
                    OfferInfo(
                        o.offerId,
                        o.offerToken,
                        o.pricingPhases.pricingPhaseList.map { PhaseInfo(it.priceAmountMicros, it.formattedPrice, it.billingPeriod, it.priceCurrencyCode) },
                    )
                }
                val picked = pickOffer(infos) ?: continue
                offers[plan] = PlanOffer(
                    plan, picked.recurring.formattedPrice, picked.recurring.priceMicros,
                    picked.hasFreeTrial, details, picked.token, picked.trialDays, picked.recurring.currencyCode,
                )
            }
        }
        queryDetails(BillingClient.ProductType.INAPP, LIFETIME_ID).firstOrNull()?.let { details ->
            details.oneTimePurchaseOfferDetails?.let { o ->
                offers[Plan.LIFETIME] = PlanOffer(Plan.LIFETIME, o.formattedPrice, o.priceAmountMicros, false, details, null, currencyCode = o.priceCurrencyCode)
            }
        }
        _state.update { BillingUiState(offers = offers, loading = false) }
    }

    private suspend fun queryDetails(type: String, productId: String): List<ProductDetails> =
        suspendCancellableCoroutine { cont ->
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(type)
                .build()
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
            client.queryProductDetailsAsync(params) { result, queryResult ->
                val list = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryResult.productDetailsList
                } else {
                    emptyList()
                }
                if (cont.isActive) cont.resume(list)
            }
        }

    private suspend fun queryPurchases(type: String): List<Purchase>? =
        suspendCancellableCoroutine { cont ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            client.queryPurchasesAsync(params) { result, list ->
                val value = if (result.responseCode == BillingClient.BillingResponseCode.OK) list else null
                if (cont.isActive) cont.resume(value)
            }
        }

    /** Đọc lại các lần mua. Nếu Google Play không trả lời được thì giữ nguyên trạng thái đã lưu. */
    private suspend fun refreshPurchases(): ProKind? {
        val subs = queryPurchases(BillingClient.ProductType.SUBS)
        val inApp = queryPurchases(BillingClient.ProductType.INAPP)
        if (subs == null && inApp == null) return null
        return handle(subs.orEmpty() + inApp.orEmpty())
    }

    private suspend fun handle(purchases: List<Purchase>): ProKind? {
        val infos = purchases.map {
            PurchaseInfo(it.products, it.purchaseState == Purchase.PurchaseState.PURCHASED)
        }
        val kind = activeKind(infos)
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) }
        onProChanged(kind != null, kind)
        return kind
    }
}
