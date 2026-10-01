package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageOpsTest {

    // --- Sắp trang: Lên / Xuống ---

    @Test
    fun moveUpSwapsPageWithThePreviousOne() {
        assertEquals(listOf(1, 3, 2, 4), moveUp(listOf(1, 2, 3, 4), 2))
    }

    @Test
    fun moveUpOnFirstRowKeepsOrder() {
        assertEquals(listOf(1, 2, 3), moveUp(listOf(1, 2, 3), 0))
    }

    @Test
    fun moveUpOutsideTheListKeepsOrder() {
        assertEquals(listOf(1, 2, 3), moveUp(listOf(1, 2, 3), -1))
        assertEquals(listOf(1, 2, 3), moveUp(listOf(1, 2, 3), 3))
    }

    @Test
    fun moveDownSwapsPageWithTheNextOne() {
        assertEquals(listOf(1, 3, 2, 4), moveDown(listOf(1, 2, 3, 4), 1))
    }

    @Test
    fun moveDownOnLastRowKeepsOrder() {
        assertEquals(listOf(1, 2, 3), moveDown(listOf(1, 2, 3), 2))
        assertEquals(listOf(1, 2, 3), moveDown(listOf(1, 2, 3), 7))
        assertEquals(listOf(1, 2, 3), moveDown(listOf(1, 2, 3), -1))
    }

    @Test
    fun movesUseRowPositionNotPageNumber() {
        // Thứ tự đã bị đảo: hàng 1 đang là trang gốc số 1.
        assertEquals(listOf(1, 3, 2), moveUp(listOf(3, 1, 2), 1))
        assertEquals(listOf(1, 3, 2), moveDown(listOf(3, 1, 2), 0))
    }

    @Test
    fun moveUpThenDownRestoresOrderAndKeepsInputUntouched() {
        val start = listOf(1, 2, 3, 4)
        val moved = moveUp(start, 3)
        assertEquals(listOf(1, 2, 4, 3), moved)
        assertEquals(start, moveDown(moved, 2))
        assertEquals(listOf(1, 2, 3, 4), start)
    }

    // --- Xóa trang ---

    @Test
    fun removePageKeepsTheRestInOrder() {
        assertEquals(listOf(4, 3, 2), removePage(listOf(4, 1, 3, 2), 1))
        assertEquals(listOf(1, 2), removePage(listOf(1, 2, 3), 2))
    }

    @Test
    fun removePageRefusesToRemoveTheOnlyPage() {
        // Tài liệu phải còn ít nhất 1 trang.
        assertEquals(listOf(1), removePage(listOf(1), 0))
    }

    @Test
    fun removePageOutsideTheListKeepsOrder() {
        assertEquals(listOf(1, 2), removePage(listOf(1, 2), 2))
        assertEquals(listOf(1, 2), removePage(listOf(1, 2), -1))
    }

    // --- Kiểm tra thứ tự trước khi lưu ---

    @Test
    fun validOrderAcceptsShuffledSubset() {
        assertTrue(isValidOrder(listOf(3, 1), 3))
        assertTrue(isValidOrder(listOf(1, 2, 3), 3))
        assertTrue(isValidOrder(listOf(2), 2))
    }

    @Test
    fun validOrderRejectsBadLists() {
        assertFalse("rỗng", isValidOrder(emptyList(), 3))
        assertFalse("trùng", isValidOrder(listOf(1, 1, 2), 3))
        assertFalse("số 0", isValidOrder(listOf(0, 1), 3))
        assertFalse("số âm", isValidOrder(listOf(-1, 1), 3))
        assertFalse("lớn hơn số trang", isValidOrder(listOf(1, 4), 3))
    }

    @Test
    fun identityOrderListsPagesFromOne() {
        assertEquals(listOf(1, 2, 3, 4), identityOrder(4))
        assertEquals(listOf(1), identityOrder(1))
        assertEquals(emptyList<Int>(), identityOrder(0))
    }

    // --- Chọn nhiều ---

    @Test
    fun toggleSelectionAddsToEndThenRemoves() {
        val one = toggleSelection(emptyList(), 5L)
        assertEquals(listOf(5L), one)
        val two = toggleSelection(one, 2L)
        assertEquals(listOf(5L, 2L), two)
        assertEquals(listOf(2L), toggleSelection(two, 5L))
    }

    @Test
    fun pruneSelectionDropsMissingIdsAndKeepsTapOrder() {
        assertEquals(listOf(3L, 2L), pruneSelection(listOf(3L, 1L, 2L), setOf(2L, 3L, 9L)))
        assertEquals(emptyList<Long>(), pruneSelection(listOf(4L), emptySet()))
    }

    @Test
    fun orderedSelectionFollowsTapOrderAndSkipsMissing() {
        val docs = listOf(Doc(1, "a", 10, 1), Doc(2, "b", 20, 2), Doc(3, "c", 30, 3))
        val picked = orderedSelection(listOf(3L, 9L, 1L), docs)
        assertEquals(listOf(3L, 1L), picked.map { it.id })
    }

    // --- Gộp ---

    @Test
    fun mergeNeedsAtLeastTwoDocs() {
        assertEquals(MergeCheck.NeedTwo, checkMerge(emptyList()))
        assertEquals(MergeCheck.NeedTwo, checkMerge(listOf(Doc(1, "a", 1, 3))))
    }

    @Test
    fun mergeAllowsExactlyThirtyPages() {
        val docs = listOf(Doc(1, "a", 1, 12), Doc(2, "b", 2, 18))
        assertEquals(MergeCheck.Ok(30), checkMerge(docs))
    }

    @Test
    fun mergeRejectsMoreThanThirtyPages() {
        val docs = listOf(Doc(1, "a", 1, 12), Doc(2, "b", 2, 18), Doc(3, "c", 3, 1))
        assertEquals(MergeCheck.TooMany(31), checkMerge(docs))
    }

    @Test
    fun mergeRespectsCustomLimit() {
        val docs = listOf(Doc(1, "a", 1, 2), Doc(2, "b", 2, 2))
        assertEquals(MergeCheck.Ok(4), checkMerge(docs, maxPages = 4))
        assertEquals(MergeCheck.TooMany(4), checkMerge(docs, maxPages = 3))
    }
}
