package com.ethanstudio.snapsheet.billing

/**
 * Ba gói bán trong app. Tên mã (ID) phải khớp từng chữ với Play Console:
 *  - Đăng ký (Subscription) `snapsheet_pro` có 2 gói cơ bản (base plan): `monthly` và `yearly`.
 *  - Sản phẩm trong app (One-time product) `snapsheet_lifetime`: mua một lần, dùng mãi.
 */
enum class Plan(val productId: String, val isSubscription: Boolean, val basePlanId: String?) {
    MONTHLY(SUBSCRIPTION_ID, true, "monthly"),
    YEARLY(SUBSCRIPTION_ID, true, "yearly"),
    LIFETIME(LIFETIME_ID, false, null),
}

const val SUBSCRIPTION_ID = "snapsheet_pro"
const val LIFETIME_ID = "snapsheet_lifetime"

/** Loại quyền lợi đang có: đăng ký (tháng hoặc năm) hay mua vĩnh viễn. */
enum class ProKind { SUBSCRIPTION, LIFETIME }

/** Một lần mua trong Google Play, rút gọn để dễ kiểm tra. */
data class PurchaseInfo(val productIds: List<String>, val purchased: Boolean)

/** Có quyền Pro không, và loại nào. Vĩnh viễn được ưu tiên hơn đăng ký. Giao dịch đang chờ thanh toán không tính. */
fun activeKind(purchases: List<PurchaseInfo>): ProKind? {
    val active = purchases.filter { it.purchased }.flatMap { it.productIds }
    return when {
        LIFETIME_ID in active -> ProKind.LIFETIME
        SUBSCRIPTION_ID in active -> ProKind.SUBSCRIPTION
        else -> null
    }
}

/** Một giai đoạn giá của gói đăng ký (dùng thử miễn phí, giá giới thiệu, giá thường). */
data class PhaseInfo(val priceMicros: Long, val formattedPrice: String)

/** Một ưu đãi của gói cơ bản. Giai đoạn cuối cùng là giá gia hạn lâu dài. */
data class OfferInfo(val offerId: String?, val token: String, val phases: List<PhaseInfo>) {
    val hasFreeTrial: Boolean get() = phases.size > 1 && phases.first().priceMicros == 0L
    val recurring: PhaseInfo get() = phases.last()
}

/** Chọn ưu đãi để hiện: ưu tiên ưu đãi có dùng thử miễn phí, nếu không có thì lấy gói thường. */
fun pickOffer(offers: List<OfferInfo>): OfferInfo? =
    offers.firstOrNull { it.hasFreeTrial } ?: offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()

/** Phần trăm tiết kiệm của gói năm so với 12 tháng mua lẻ. Null nếu không tính được hoặc không rẻ hơn. */
fun yearlySavingPercent(monthlyMicros: Long, yearlyMicros: Long): Int? {
    if (monthlyMicros <= 0 || yearlyMicros <= 0) return null
    val full = monthlyMicros * 12
    if (yearlyMicros >= full) return null
    return (((full - yearlyMicros) * 100) / full).toInt()
}
