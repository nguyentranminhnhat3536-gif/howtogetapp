package com.ethanstudio.snapsheet.ui.doc

import com.ethanstudio.snapsheet.data.Doc
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** "dirty" quyết định nút Lưu có bật không và Back có hỏi bỏ thay đổi không. */
class PagesUiStateTest {
    private val doc = Doc(1, "Scan", 100, 3)

    @Test
    fun notDirtyBeforeDocLoads() {
        assertFalse(PagesUiState().dirty)
        assertFalse(PagesUiState(doc = null, loaded = true, order = listOf(2, 1)).dirty)
    }

    @Test
    fun notDirtyWhenOrderIsUnchanged() {
        assertFalse(PagesUiState(doc = doc, loaded = true, order = listOf(1, 2, 3)).dirty)
    }

    @Test
    fun dirtyAfterReorder() {
        assertTrue(PagesUiState(doc = doc, loaded = true, order = listOf(2, 1, 3)).dirty)
    }

    @Test
    fun dirtyAfterRemovingPage() {
        assertTrue(PagesUiState(doc = doc, loaded = true, order = listOf(1, 2)).dirty)
    }

    @Test
    fun movingBackToOriginalOrderIsNotDirty() {
        val moved = PagesUiState(doc = doc, loaded = true, order = listOf(2, 1, 3))
        assertFalse(moved.copy(order = listOf(1, 2, 3)).dirty)
    }
}
