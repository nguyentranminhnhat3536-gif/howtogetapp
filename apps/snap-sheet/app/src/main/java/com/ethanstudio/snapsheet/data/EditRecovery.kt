package com.ethanstudio.snapsheet.data

/** Việc cần làm khi mở app mà còn sót thư mục tạm của một lần ghi trang bị ngắt giữa chừng. */
enum class Recovery { NOTHING, DELETE_NEW, PROMOTE_NEW, FINISH_COMMIT, RESTORE_OLD }

/**
 * live = docs/<id>, new = docs/<id>.new, old = docs/<id>.old. Xét theo thứ tự, dòng đầu khớp thì chọn:
 *  1. live & old         → FINISH_COMMIT (đã đổi tên xong: xóa old, xóa new nếu còn)
 *  2. !live & new & old  → PROMOTE_NEW   (tắt giữa 2 lần đổi tên: new → live, xóa old)
 *  3. !live & !new & old → RESTORE_OLD   (old → live)
 *  4. new (& !old)       → DELETE_NEW    (tắt lúc đang dựng bản mới, hoặc tài liệu đã bị xóa)
 *  5. còn lại            → NOTHING
 */
fun recoveryAction(hasLive: Boolean, hasNew: Boolean, hasOld: Boolean): Recovery = when {
    hasLive && hasOld -> Recovery.FINISH_COMMIT
    hasNew && hasOld -> Recovery.PROMOTE_NEW
    hasOld -> Recovery.RESTORE_OLD
    hasNew -> Recovery.DELETE_NEW
    else -> Recovery.NOTHING
}
