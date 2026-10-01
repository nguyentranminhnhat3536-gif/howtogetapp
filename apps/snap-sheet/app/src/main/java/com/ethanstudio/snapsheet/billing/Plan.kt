package com.ethanstudio.snapsheet.billing

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

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
data class PhaseInfo(
    val priceMicros: Long,
    val formattedPrice: String,
    val billingPeriod: String = "",
    /** Mã tiền tệ ISO 4217 (ví dụ "USD"), do Google Play trả về. */
    val currencyCode: String = "",
)

/** Một ưu đãi của gói cơ bản. Giai đoạn cuối cùng là giá gia hạn lâu dài. */
data class OfferInfo(val offerId: String?, val token: String, val phases: List<PhaseInfo>) {
    val hasFreeTrial: Boolean get() = phases.size > 1 && phases.first().priceMicros == 0L
    val recurring: PhaseInfo get() = phases.last()

    /** Số ngày dùng thử miễn phí (ví dụ 7), null nếu không có. */
    val trialDays: Int? get() = if (hasFreeTrial) periodDays(phases.first().billingPeriod) else null
}

/** Đổi kỳ hạn ISO 8601 của Google Play ("P7D", "P1W", "P1M") thành số ngày. Tháng tính 30 ngày. */
fun periodDays(period: String): Int? {
    val match = Regex("^P(\\d+)([DWMY])$").find(period.trim().uppercase()) ?: return null
    val n = match.groupValues[1].toInt()
    return when (match.groupValues[2]) {
        "D" -> n
        "W" -> n * 7
        "M" -> n * 30
        else -> n * 365
    }
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

/**
 * Giá gói năm chia 12, định dạng theo tiền tệ và [locale] (ví dụ "$0.83").
 * Null nếu giá <= 0 hoặc mã tiền tệ không hợp lệ.
 */
fun formatPerMonth(yearlyMicros: Long, currencyCode: String, locale: Locale): String? =
    formatAmount(yearlyMicros, 12_000_000L, currencyCode, locale)

/** Định dạng [micros] theo tiền tệ và [locale] (ví dụ 100_000_000 USD → "$100.00"). Null nếu <= 0 hoặc mã tiền tệ sai. */
fun formatMicros(micros: Long, currencyCode: String, locale: Locale): String? =
    formatAmount(micros, 1_000_000L, currencyCode, locale)

/** Chia [micros] cho [divisor], làm tròn theo số lẻ của tiền tệ rồi định dạng. */
private fun formatAmount(micros: Long, divisor: Long, currencyCode: String, locale: Locale): String? {
    if (micros <= 0) return null
    val money = try {
        Currency.getInstance(currencyCode)
    } catch (e: IllegalArgumentException) {
        return null
    }
    val digits = money.defaultFractionDigits.coerceAtLeast(0)
    val amount = BigDecimal.valueOf(micros)
        .divide(BigDecimal.valueOf(divisor), digits, RoundingMode.HALF_UP)
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        currency = money
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    return format.format(amount)
}

/** Một ưu đãi (offer) của sản phẩm mua một lần, rút gọn để kiểm tra trên JVM. */
data class OneTimeOfferInfo(
    val token: String,
    val priceMicros: Long,
    val formattedPrice: String,
    val currencyCode: String,
    /** Giá gốc trước giảm, null nếu Google Play không báo giảm giá. */
    val fullPriceMicros: Long? = null,
    /** Phần trăm giảm do Google Play trả về, null nếu giảm theo số tiền. */
    val percentDiscount: Int? = null,
)

/** Giảm giá thật do Google Play trả về: giá gốc và phần trăm giảm. */
data class Sale(val fullPriceMicros: Long, val percent: Int)

/**
 * Giảm giá của [offer], null nếu không có giảm giá thật (không có giá gốc, giá gốc không cao hơn giá bán,
 * giá bán <= 0, hoặc phần trăm < 1). Phần trăm lấy của Google Play nếu nằm trong 1..99,
 * không thì tự tính và làm tròn xuống để không phóng đại mức giảm.
 */
fun saleOf(offer: OneTimeOfferInfo): Sale? {
    val full = offer.fullPriceMicros ?: return null
    val price = offer.priceMicros
    if (price <= 0 || full <= price) return null
    val percent = offer.percentDiscount?.takeIf { it in 1..99 }
        ?: ((full - price) * 100 / full).toInt()
    if (percent < 1) return null
    return Sale(full, percent)
}

/** Chọn offer rẻ nhất (bỏ offer giá <= 0); bằng giá thì ưu tiên offer có giảm giá. Không có thì null. */
fun pickOneTimeOffer(offers: List<OneTimeOfferInfo>): OneTimeOfferInfo? =
    offers.filter { it.priceMicros > 0 }
        .minWithOrNull(compareBy<OneTimeOfferInfo> { it.priceMicros }.thenBy { if (saleOf(it) != null) 0 else 1 })
