package com.ethanstudio.snapsheet.ui.watermark

import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.data.WatermarkColor
import com.ethanstudio.snapsheet.data.WatermarkSpec
import com.ethanstudio.snapsheet.data.WatermarkStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nút "Lưu bản có chữ mờ" chỉ bật khi đã có tài liệu, chữ không rỗng và không đang lưu. */
class WatermarkUiStateTest {
    private val doc = Doc(1, "Scan", 100, 3)

    @Test
    fun cannotSaveBeforeDocLoads() {
        assertFalse(WatermarkUiState().canSave)
        assertFalse(WatermarkUiState(doc = null, loaded = true, text = "DRAFT").canSave)
    }

    @Test
    fun startsGrayMediumWithNoText() {
        val state = WatermarkUiState(doc = doc, loaded = true)
        assertEquals(WatermarkColor.GRAY, state.color)
        assertEquals(WatermarkStrength.MEDIUM, state.strength)
        assertNull(state.spec)
        assertFalse(state.canSave)
    }

    @Test
    fun blankTextCannotBeSaved() {
        val state = WatermarkUiState(doc = doc, loaded = true, text = "  \n ")
        assertNull(state.spec)
        assertFalse(state.canSave)
    }

    @Test
    fun specUsesCleanedTextAndChoices() {
        val state = WatermarkUiState(
            doc = doc, loaded = true, text = "  BẢN \n SAO  ",
            color = WatermarkColor.RED, strength = WatermarkStrength.STRONG,
        )
        assertEquals(WatermarkSpec("BẢN SAO", WatermarkColor.RED, WatermarkStrength.STRONG), state.spec)
        assertTrue(state.canSave)
    }

    @Test
    fun cannotSaveTwiceWhileSaving() {
        assertFalse(WatermarkUiState(doc = doc, loaded = true, text = "MẬT", saving = true).canSave)
    }
}
