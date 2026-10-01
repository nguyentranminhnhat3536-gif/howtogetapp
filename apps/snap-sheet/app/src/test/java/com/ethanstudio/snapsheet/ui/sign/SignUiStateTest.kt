package com.ethanstudio.snapsheet.ui.sign

import com.ethanstudio.snapsheet.data.DEFAULT_PLACEMENT
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.SignaturePlacement
import com.ethanstudio.snapsheet.data.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nút "Lưu bản đã ký" và "Dùng chữ ký này" chỉ bật khi đủ điều kiện. */
class SignUiStateTest {
    private val doc = Doc(1, "Hợp đồng", 100, 3)
    private val placed = mapOf(0 to DEFAULT_PLACEMENT)

    @Test
    fun cannotSaveBeforeDocLoads() {
        assertFalse(SignUiState().canSave)
        assertFalse(SignUiState(loaded = true, hasSignature = true, placements = placed).canSave)
    }

    @Test
    fun canSaveWithSignaturePlacedOnAPage() {
        assertTrue(SignUiState(doc = doc, loaded = true, hasSignature = true, placements = placed).canSave)
    }

    @Test
    fun cannotSaveWithoutAnyPlacement() {
        assertFalse(SignUiState(doc = doc, loaded = true, hasSignature = true).canSave)
    }

    @Test
    fun cannotSaveWithoutSavedSignature() {
        assertFalse(SignUiState(doc = doc, loaded = true, hasSignature = false, placements = placed).canSave)
    }

    @Test
    fun cannotSaveTwiceWhileSaving() {
        assertFalse(SignUiState(doc = doc, loaded = true, hasSignature = true, placements = placed, saving = true).canSave)
    }

    @Test
    fun currentFollowsPageBeingViewed() {
        val second = SignaturePlacement(0.3f, 0.4f, 0.2f)
        val state = SignUiState(doc = doc, loaded = true, hasSignature = true, placements = mapOf(0 to DEFAULT_PLACEMENT, 2 to second))
        assertEquals(DEFAULT_PLACEMENT, state.current)
        assertNull(state.copy(page = 1).current)
        assertEquals(second, state.copy(page = 2).current)
    }

    @Test
    fun singleDotCannotBeUsedAsSignature() {
        assertFalse(SignUiState().canUseStrokes)
        assertFalse(SignUiState(strokes = listOf(listOf(StrokePoint(0.5f, 0.5f)))).canUseStrokes)
    }

    @Test
    fun drawnLineCanBeUsedAsSignature() {
        val line = listOf(StrokePoint(0.1f, 0.5f), StrokePoint(0.9f, 0.5f))
        assertTrue(SignUiState(strokes = listOf(line)).canUseStrokes)
    }
}
