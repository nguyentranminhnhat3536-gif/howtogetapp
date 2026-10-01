package com.ethanstudio.snapsheet.data

/** Giới hạn của bản miễn phí. Bản Pro không bị giới hạn xuất file và được quét nhiều trang hơn. */
object FreeLimits {
    const val DAILY_EXPORTS = 3
    const val FREE_PAGES = 5
    const val PRO_PAGES = 30

    /** Số lần đã xuất trong ngày [day] (số ngày kể từ 1970-01-01). */
    data class Usage(val day: Long = 0, val count: Int = 0)

    fun pageLimit(isPro: Boolean): Int = if (isPro) PRO_PAGES else FREE_PAGES

    /** Số lần xuất còn lại hôm nay. Bản Pro: không giới hạn. */
    fun remaining(isPro: Boolean, usage: Usage, today: Long): Int {
        if (isPro) return Int.MAX_VALUE
        val used = if (usage.day == today) usage.count else 0
        return (DAILY_EXPORTS - used).coerceAtLeast(0)
    }

    /** Ghi nhận một lần xuất. Sang ngày mới thì đếm lại từ 1. */
    fun consume(usage: Usage, today: Long): Usage =
        if (usage.day == today) usage.copy(count = usage.count + 1) else Usage(today, 1)

    /** Số trang còn được thêm vào một tài liệu đang có [current] trang; không âm. */
    fun pagesCanAdd(isPro: Boolean, current: Int): Int = (pageLimit(isPro) - current).coerceAtLeast(0)

    /** Còn đủ [count] lượt xuất hôm nay không. Pro: luôn đủ; count <= 0: luôn đủ. */
    fun canConsume(isPro: Boolean, usage: Usage, today: Long, count: Int): Boolean =
        isPro || count <= 0 || remaining(false, usage, today) >= count

    /** Ghi nhận [count] lượt xuất. Sang ngày mới thì đếm lại từ [count]. */
    fun consumeMany(usage: Usage, today: Long, count: Int): Usage =
        if (usage.day == today) usage.copy(count = usage.count + count) else Usage(today, count)
}
