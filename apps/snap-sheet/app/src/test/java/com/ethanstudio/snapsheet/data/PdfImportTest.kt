package com.ethanstudio.snapsheet.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nhập file PDF: cỡ ảnh mỗi trang, số trang được nhập, tên tài liệu, chặn file quá lớn. */
class PdfImportTest {

    // ---- renderSize: 200 dpi nhưng cạnh dài không quá 2000 px ----

    @Test
    fun a4PortraitFitsLongSideOf2000() {
        assertEquals(1413 to 2000, renderSize(595, 842))
    }

    @Test
    fun letterPortraitFitsLongSideOf2000() {
        assertEquals(1545 to 2000, renderSize(612, 792))
    }

    @Test
    fun a4LandscapeKeepsOrientation() {
        assertEquals(2000 to 1413, renderSize(842, 595))
    }

    @Test
    fun smallPageStillGets200Dpi() {
        // 100 × 50 điểm = 1,39 × 0,69 inch → 277,8 × 138,9 px ở 200 dpi.
        assertEquals(278 to 139, renderSize(100, 50))
    }

    @Test
    fun hugePageShrinksTo2000() {
        assertEquals(2000 to 2000, renderSize(5000, 5000))
    }

    @Test
    fun veryThinPageKeepsAtLeastOnePixel() {
        assertEquals(2000 to 1, renderSize(10_000, 1))
        assertEquals(1 to 2000, renderSize(1, 10_000))
    }

    @Test
    fun customDpiAndMaxSideAreRespected() {
        // 72 dpi = 1 px mỗi điểm.
        assertEquals(72 to 36, renderSize(72, 36, dpi = 72, maxSide = 5000))
        // Cạnh dài 144 bị kéo về 100 → hệ số 100/144, cạnh 72 thành 50.
        assertEquals(100 to 50, renderSize(144, 72, dpi = 100, maxSide = 100))
    }

    @Test
    fun defaultSizeNeverExceedsMaxSide() {
        for ((w, h) in listOf(595 to 842, 842 to 595, 3000 to 100, 100 to 3000, 2000 to 2000, 721 to 720)) {
            val (pw, ph) = renderSize(w, h)
            assertTrue("$w×$h → $pw×$ph", pw in 1..IMPORT_MAX_SIDE && ph in 1..IMPORT_MAX_SIDE)
        }
    }

    @Test
    fun emptyOrNegativePageSizeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { renderSize(0, 10) }
        assertThrows(IllegalArgumentException::class.java) { renderSize(10, -1) }
        assertThrows(IllegalArgumentException::class.java) { renderSize(0, 0) }
    }

    // ---- importPlan: chỉ nhập N trang đầu ----

    @Test
    fun longPdfIsCutAtFreeLimit() {
        val plan = importPlan(12, 5)
        assertEquals(ImportPlan(12, 5), plan)
        assertTrue(plan.truncated)
    }

    @Test
    fun shortPdfIsImportedWhole() {
        val plan = importPlan(3, 5)
        assertEquals(3, plan.take)
        assertFalse(plan.truncated)
    }

    @Test
    fun pdfExactlyAtLimitIsNotTruncated() {
        val plan = importPlan(5, 5)
        assertEquals(5, plan.take)
        assertFalse(plan.truncated)
    }

    @Test
    fun proLimitCutsAt30() {
        val plan = importPlan(40, 30)
        assertEquals(30, plan.take)
        assertEquals(40, plan.total)
        assertTrue(plan.truncated)
    }

    @Test
    fun emptyPdfTakesNothing() {
        val plan = importPlan(0, 5)
        assertEquals(0, plan.take)
        assertFalse(plan.truncated)
    }

    @Test
    fun brokenPageCountNeverGivesNegativeTake() {
        val plan = importPlan(-3, 5)
        assertEquals(0, plan.take)
        assertFalse(plan.truncated)
    }

    // ---- docNameFromFileName: tên tài liệu lấy từ tên file ----

    @Test
    fun pdfExtensionIsDropped() {
        assertEquals("Hợp đồng", docNameFromFileName("Hợp đồng.pdf"))
        assertEquals("a.b", docNameFromFileName("a.b.pdf"))
    }

    @Test
    fun pdfExtensionIgnoresCase() {
        assertEquals("REPORT", docNameFromFileName("REPORT.PDF"))
        assertEquals("scan", docNameFromFileName("scan.PdF"))
    }

    @Test
    fun nameWithoutPdfExtensionIsKept() {
        assertEquals("notes", docNameFromFileName("notes"))
        assertEquals("report.pdf.txt", docNameFromFileName("report.pdf.txt"))
    }

    @Test
    fun spacesAroundNameAreTrimmed() {
        assertEquals("Bill", docNameFromFileName("  Bill  .pdf"))
    }

    @Test
    fun longNameIsCutTo80() {
        assertEquals(80, docNameFromFileName("x".repeat(200) + ".pdf")?.length)
    }

    @Test
    fun emptyNameGivesNullSoCallerUsesDefault() {
        assertNull(docNameFromFileName(null))
        assertNull(docNameFromFileName(""))
        assertNull(docNameFromFileName(".pdf"))
        assertNull(docNameFromFileName("  .pdf"))
    }

    // ---- copyAtMost: chặn file quá lớn ----

    @Test
    fun sourceExactlyAtLimitIsCopied() {
        val data = ByteArray(10) { it.toByte() }
        val out = ByteArrayOutputStream()
        assertTrue(copyAtMost(ByteArrayInputStream(data), out, 10))
        assertArrayEquals(data, out.toByteArray())
    }

    @Test
    fun sourceOneByteOverLimitIsRejected() {
        val out = ByteArrayOutputStream()
        assertFalse(copyAtMost(ByteArrayInputStream(ByteArray(11)), out, 10))
    }

    @Test
    fun emptySourceCopiesNothing() {
        val out = ByteArrayOutputStream()
        assertTrue(copyAtMost(ByteArrayInputStream(ByteArray(0)), out, 10))
        assertEquals(0, out.size())
    }

    @Test
    fun sourceLargerThanBufferIsCopiedWhole() {
        // 200 000 byte > bộ đệm 64 KB: phải chép qua nhiều lượt mà không mất byte nào.
        val data = ByteArray(200_000) { (it % 251).toByte() }
        val out = ByteArrayOutputStream()
        assertTrue(copyAtMost(ByteArrayInputStream(data), out, 200_000))
        assertArrayEquals(data, out.toByteArray())
    }

    @Test
    fun sourceLargerThanBufferAndOverLimitIsRejected() {
        val out = ByteArrayOutputStream()
        assertFalse(copyAtMost(ByteArrayInputStream(ByteArray(200_001)), out, 200_000))
    }

    @Test(timeout = 10_000)
    fun endlessSourceStopsAtLimit() {
        // Nguồn không bao giờ hết (ví dụ file rất lớn): phải dừng sớm và trả false, không đọc hết.
        val endless = object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int = len
        }
        assertFalse(copyAtMost(endless, ByteArrayOutputStream(), 1_000))
    }

    @Test
    fun importLimitIs100Megabytes() {
        assertEquals(100L * 1024 * 1024, IMPORT_MAX_BYTES)
    }

    @Test
    fun importErrorIsAnIOExceptionWithReason() {
        val error: Exception = PdfImportException(PdfImportException.Reason.LOCKED)
        assertTrue(error is IOException)
        assertEquals(PdfImportException.Reason.LOCKED, (error as PdfImportException).reason)
    }
}
