package com.ethanstudio.snapsheet.data

/*
 * Hàm thuần cho màn Sửa trang và chế độ chọn nhiều ở tab Files.
 * [order] là danh sách số trang GỐC (bắt đầu từ 1) theo thứ tự mới.
 */

/** Đưa trang ở vị trí [index] lên trên một bậc. Vị trí đầu hoặc ngoài khoảng: trả nguyên. */
fun moveUp(order: List<Int>, index: Int): List<Int> {
    if (index !in 1 until order.size) return order
    return order.toMutableList().apply { add(index - 1, removeAt(index)) }
}

/** Đưa trang ở vị trí [index] xuống một bậc. Vị trí cuối hoặc ngoài khoảng: trả nguyên. */
fun moveDown(order: List<Int>, index: Int): List<Int> {
    if (index !in 0 until order.size - 1) return order
    return order.toMutableList().apply { add(index + 1, removeAt(index)) }
}

/** Bỏ trang ở vị trí [index]. Chỉ còn một trang hoặc ngoài khoảng: trả nguyên (tài liệu phải có ít nhất 1 trang). */
fun removePage(order: List<Int>, index: Int): List<Int> {
    if (order.size <= 1 || index !in order.indices) return order
    return order.toMutableList().apply { removeAt(index) }
}

/** Không rỗng, không trùng, mọi phần tử nằm trong 1..[pageCount]. */
fun isValidOrder(order: List<Int>, pageCount: Int): Boolean =
    order.isNotEmpty() && order.toSet().size == order.size && order.all { it in 1..pageCount }

fun identityOrder(pageCount: Int): List<Int> = (1..pageCount).toList()

/** Có thì bỏ, chưa có thì thêm vào cuối (giữ thứ tự chạm chọn). */
fun toggleSelection(selection: List<Long>, id: Long): List<Long> =
    if (id in selection) selection - id else selection + id

/** Bỏ các id không còn tồn tại. */
fun pruneSelection(selection: List<Long>, existing: Set<Long>): List<Long> = selection.filter { it in existing }

/** Tài liệu theo đúng thứ tự chọn, bỏ id không còn. */
fun orderedSelection(selection: List<Long>, docs: List<Doc>): List<Doc> {
    val byId = docs.associateBy { it.id }
    return selection.mapNotNull { byId[it] }
}

/** Kết quả kiểm tra trước khi gộp. */
sealed interface MergeCheck {
    data object NeedTwo : MergeCheck
    data class TooMany(val total: Int) : MergeCheck
    data class Ok(val total: Int) : MergeCheck
}

/** Cần ít nhất 2 tài liệu; tổng số trang không vượt quá [maxPages]. */
fun checkMerge(docs: List<Doc>, maxPages: Int = FreeLimits.PRO_PAGES): MergeCheck {
    if (docs.size < 2) return MergeCheck.NeedTwo
    val total = docs.sumOf { it.pageCount }
    return if (total > maxPages) MergeCheck.TooMany(total) else MergeCheck.Ok(total)
}
