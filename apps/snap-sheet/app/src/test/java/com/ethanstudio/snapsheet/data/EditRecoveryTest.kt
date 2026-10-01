package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bảng quyết định khi mở app mà còn sót thư mục tạm của lần ghi trang bị ngắt.
 * Tham số: (có docs/<id>, có docs/<id>.new, có docs/<id>.old).
 */
class EditRecoveryTest {

    @Test
    fun liveAndOldMeansCommitFinishedRename() {
        assertEquals(Recovery.FINISH_COMMIT, recoveryAction(hasLive = true, hasNew = true, hasOld = true))
        assertEquals(Recovery.FINISH_COMMIT, recoveryAction(hasLive = true, hasNew = false, hasOld = true))
    }

    @Test
    fun stoppedBetweenTwoRenamesPromotesNewCopy() {
        assertEquals(Recovery.PROMOTE_NEW, recoveryAction(hasLive = false, hasNew = true, hasOld = true))
    }

    @Test
    fun onlyOldLeftRestoresIt() {
        assertEquals(Recovery.RESTORE_OLD, recoveryAction(hasLive = false, hasNew = false, hasOld = true))
    }

    @Test
    fun unfinishedNewCopyIsDeletedAndOriginalKept() {
        // Tắt app lúc đang dựng bản mới: bản gốc còn nguyên, chỉ bỏ bản dở.
        assertEquals(Recovery.DELETE_NEW, recoveryAction(hasLive = true, hasNew = true, hasOld = false))
        // Tài liệu đã bị xóa nhưng còn sót bản dở.
        assertEquals(Recovery.DELETE_NEW, recoveryAction(hasLive = false, hasNew = true, hasOld = false))
    }

    @Test
    fun nothingLeftOverMeansNothingToDo() {
        assertEquals(Recovery.NOTHING, recoveryAction(hasLive = true, hasNew = false, hasOld = false))
        assertEquals(Recovery.NOTHING, recoveryAction(hasLive = false, hasNew = false, hasOld = false))
    }

    @Test
    fun oldCopyIsNeverDroppedWithoutReplacement() {
        // Mọi tổ hợp còn bản .old đều phải giữ được một bản dùng được (không bao giờ chỉ xóa hay bỏ qua).
        val keeps = setOf(Recovery.FINISH_COMMIT, Recovery.PROMOTE_NEW, Recovery.RESTORE_OLD)
        for (live in listOf(true, false)) {
            for (newCopy in listOf(true, false)) {
                val action = recoveryAction(live, newCopy, hasOld = true)
                assertTrue("live=$live new=$newCopy → $action", action in keeps)
            }
        }
    }
}
