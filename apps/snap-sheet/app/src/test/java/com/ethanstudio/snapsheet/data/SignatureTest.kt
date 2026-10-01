package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ký tên: nét vẽ, cắt ảnh chữ ký, giữ chữ ký trong trang, lưu vị trí khi xoay màn hình. */
class SignatureTest {
    private fun p(x: Float, y: Float) = StrokePoint(x, y)

    // ---- strokeBounds ----

    @Test
    fun noPointsGiveNoBounds() {
        assertNull(strokeBounds(emptyList()))
        assertNull(strokeBounds(listOf(emptyList())))
    }

    @Test
    fun boundsCoverEveryStroke() {
        val strokes = listOf(listOf(p(0.25f, 0.5f), p(0.75f, 0.25f)), listOf(p(0.5f, 0.75f)))
        assertEquals(NormRect(0.25f, 0.25f, 0.75f, 0.75f), strokeBounds(strokes))
    }

    @Test
    fun singleDotGivesZeroSizeBounds() {
        assertEquals(NormRect(0.1f, 0.2f, 0.1f, 0.2f), strokeBounds(listOf(emptyList(), listOf(p(0.1f, 0.2f)))))
    }

    // ---- isSignatureUsable: một chấm chưa phải chữ ký ----

    @Test
    fun nothingDrawnIsNotUsable() {
        assertFalse(isSignatureUsable(emptyList()))
        assertFalse(isSignatureUsable(listOf(emptyList())))
    }

    @Test
    fun dotsAloneAreNotUsable() {
        assertFalse(isSignatureUsable(listOf(listOf(p(0.5f, 0.5f)))))
        assertFalse(isSignatureUsable(listOf(listOf(p(0.1f, 0.1f)), listOf(p(0.9f, 0.9f)))))
    }

    @Test
    fun oneLineIsUsable() {
        assertTrue(isSignatureUsable(listOf(listOf(p(0.1f, 0.5f), p(0.9f, 0.5f)))))
        assertTrue(isSignatureUsable(listOf(listOf(p(0.5f, 0.5f)), listOf(p(0.1f, 0.5f), p(0.9f, 0.5f)))))
    }

    // ---- appendPoint ----

    @Test
    fun firstPointStartsStroke() {
        assertEquals(listOf(p(0.3f, 0.4f)), appendPoint(emptyList(), p(0.3f, 0.4f)))
    }

    @Test
    fun pointTooCloseIsSkipped() {
        assertEquals(1, appendPoint(listOf(p(0f, 0f)), p(0.001f, 0f)).size)
    }

    @Test
    fun pointFarEnoughIsAdded() {
        assertEquals(listOf(p(0f, 0f), p(0.01f, 0f)), appendPoint(listOf(p(0f, 0f)), p(0.01f, 0f)))
    }

    @Test
    fun distanceIsMeasuredAsStraightLine() {
        // Chéo 0,0025 × 0,0025 → khoảng cách 0,0035 < 0,004: bỏ qua.
        assertEquals(1, appendPoint(listOf(p(0f, 0f)), p(0.0025f, 0.0025f)).size)
        // Chéo 0,003 × 0,003 → khoảng cách 0,0042 ≥ 0,004: thêm.
        assertEquals(2, appendPoint(listOf(p(0f, 0f)), p(0.003f, 0.003f)).size)
    }

    @Test
    fun pointExactlyAtMinStepIsAdded() {
        assertEquals(2, appendPoint(listOf(p(0f, 0f)), p(0.5f, 0f), minStep = 0.5f).size)
    }

    @Test
    fun pointOutsidePadIsClamped() {
        assertEquals(listOf(p(1f, 0f)), appendPoint(emptyList(), p(1.5f, -0.2f)))
    }

    @Test
    fun draggingFurtherOutsidePadAddsNothing() {
        // Điểm cuối đã ở mép (1, 0); kéo tiếp ra ngoài bị kẹp về đúng chỗ đó → không thêm điểm trùng.
        assertEquals(1, appendPoint(listOf(p(1f, 0f)), p(1.5f, -0.3f)).size)
    }

    // ---- cropRect: cắt sát nét, chừa lề, không ra ngoài ảnh ----

    @Test
    fun cropAddsMarginAroundInk() {
        assertEquals(PixelRect(284, 134, 616, 466), cropRect(NormRect(0.25f, 0.25f, 0.5f, 0.75f), 1200, 600, 16))
    }

    @Test
    fun cropRoundsOutwardSoInkIsNotCut() {
        // Mép trái/trên 0,28125 × 10 = 2,8125 → 2 (làm tròn xuống, không phải 3);
        // mép phải/dưới 0,71875 × 10 = 7,1875 → 8 (làm tròn lên, không phải 7).
        assertEquals(PixelRect(2, 2, 8, 8), cropRect(NormRect(0.28125f, 0.28125f, 0.71875f, 0.71875f), 10, 10, 0))
    }

    @Test
    fun cropNeverLeavesTheImage() {
        assertEquals(PixelRect(0, 0, 1200, 600), cropRect(NormRect(0f, 0f, 1f, 1f), 1200, 600, 16))
    }

    @Test
    fun cropOfSingleDotIsAtLeastOnePixel() {
        val r = cropRect(NormRect(0.5f, 0.5f, 0.5f, 0.5f), 1200, 600, 0)
        assertEquals(1, r.width)
        assertEquals(1, r.height)
    }

    @Test
    fun cropOfDotAtFarCornerStaysInsideImage() {
        val r = cropRect(NormRect(1f, 1f, 1f, 1f), 1200, 600, 0)
        assertTrue("$r", r.left >= 0 && r.top >= 0 && r.right <= 1200 && r.bottom <= 600)
        assertTrue("$r", r.width >= 1 && r.height >= 1)
    }

    // ---- clampPlacement: chữ ký luôn nằm gọn trong trang ----

    @Test
    fun defaultPlacementOnA4IsKept() {
        assertEquals(DEFAULT_PLACEMENT, clampPlacement(DEFAULT_PLACEMENT, 0.5f, 1.414f))
    }

    @Test
    fun defaultPlacementIsBottomRight() {
        assertEquals(0.7f, DEFAULT_PLACEMENT.cx, 0.0001f)
        assertEquals(0.85f, DEFAULT_PLACEMENT.cy, 0.0001f)
        assertEquals(0.35f, DEFAULT_PLACEMENT.widthFraction, 0.0001f)
    }

    @Test
    fun draggedPastLeftEdgeStopsAtEdge() {
        val r = clampPlacement(SignaturePlacement(-1f, 0.85f, 0.35f), 0.5f, 1.414f)
        assertEquals(0.175f, r.cx, 0.0001f)
        assertEquals(0.85f, r.cy, 0.0001f)
    }

    @Test
    fun draggedPastBottomEdgeStopsAtEdge() {
        // Cao chữ ký = 0,35 × 0,5 / 1,414 = 0,1238 trang → tâm thấp nhất 1 − 0,0619 = 0,9381.
        val r = clampPlacement(SignaturePlacement(0.5f, 2f, 0.35f), 0.5f, 1.414f)
        assertEquals(0.93812f, r.cy, 0.0001f)
    }

    @Test
    fun widthStaysBetween10And80Percent() {
        assertEquals(0.8f, clampPlacement(SignaturePlacement(0.5f, 0.5f, 2f), 0.5f, 1.414f).widthFraction, 0.0001f)
        assertEquals(0.1f, clampPlacement(SignaturePlacement(0.5f, 0.5f, 0.01f), 0.5f, 1.414f).widthFraction, 0.0001f)
    }

    @Test
    fun tallSignatureOnLandscapePageShrinks() {
        // Cao = 0,35 × 2 / 0,5 = 1,4 trang > 0,9 → rộng còn 0,9 × 0,5 / 2 = 0,225.
        val r = clampPlacement(SignaturePlacement(0.5f, 0.5f, 0.35f), 2f, 0.5f)
        assertEquals(0.225f, r.widthFraction, 0.0001f)
        assertEquals(0.5f, r.cx, 0.0001f)
        assertEquals(0.5f, r.cy, 0.0001f)
    }

    @Test
    fun tallSignatureIsKeptInsideVertically() {
        // Cao 0,9 trang → tâm cao nhất là 0,45.
        val r = clampPlacement(SignaturePlacement(0.5f, 0f, 0.35f), 2f, 0.5f)
        assertEquals(0.45f, r.cy, 0.0001f)
    }

    @Test
    fun anyPlacementEndsUpInsidePage() {
        val eps = 0.0001f
        for (sig in listOf(0.2f, 0.5f, 1f, 2f, 5f)) {
            for (page in listOf(0.5f, 0.707f, 1.414f, 2f)) {
                for (x in listOf(-1f, 0f, 0.5f, 1f, 2f)) {
                    for (y in listOf(-1f, 0f, 0.5f, 1f, 2f)) {
                        for (w in listOf(0.01f, 0.35f, 2f)) {
                            val r = clampPlacement(SignaturePlacement(x, y, w), sig, page)
                            val h = r.widthFraction * sig / page
                            val msg = "sig=$sig page=$page in=($x,$y,$w) out=$r"
                            assertTrue(msg, r.widthFraction <= SIGN_MAX_WIDTH + eps)
                            assertTrue(msg, h <= SIGN_MAX_HEIGHT + eps)
                            assertTrue(msg, r.cx - r.widthFraction / 2 >= -eps && r.cx + r.widthFraction / 2 <= 1 + eps)
                            assertTrue(msg, r.cy - h / 2 >= -eps && r.cy + h / 2 <= 1 + eps)
                            assertEquals(msg, r, clampPlacement(r, sig, page))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun zeroOrNegativeAspectIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { clampPlacement(DEFAULT_PLACEMENT, 0f, 1.414f) }
        assertThrows(IllegalArgumentException::class.java) { clampPlacement(DEFAULT_PLACEMENT, 0.5f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { clampPlacement(DEFAULT_PLACEMENT, -0.5f, 1.414f) }
    }

    // ---- placementRect: khung điểm ảnh để vẽ chữ ký lên trang ----

    @Test
    fun centeredSignatureRect() {
        assertEquals(
            PixelRect(250, 875, 750, 1125),
            placementRect(SignaturePlacement(0.5f, 0.5f, 0.5f), 0.5f, 1000, 2000),
        )
    }

    @Test
    fun defaultSignatureRectOnA4() {
        // Rộng 350, cao 175; tâm (700, 1201,9).
        assertEquals(PixelRect(525, 1114, 875, 1289), placementRect(DEFAULT_PLACEMENT, 0.5f, 1000, 1414))
    }

    @Test
    fun clampedSignatureRectStaysOnPage() {
        val pageW = 1240
        val pageH = 1754
        val sig = 0.5f
        for (raw in listOf(SignaturePlacement(5f, 5f, 0.8f), SignaturePlacement(-5f, -5f, 0.8f), SignaturePlacement(-5f, 5f, 0.1f))) {
            val r = placementRect(clampPlacement(raw, sig, pageH.toFloat() / pageW), sig, pageW, pageH)
            // Cho phép lệch 1 px do làm tròn.
            assertTrue("$raw → $r", r.left >= -1 && r.top >= -1 && r.right <= pageW + 1 && r.bottom <= pageH + 1)
        }
    }

    // ---- fitInside ----

    @Test
    fun tallFrameFitsByHeight() {
        assertEquals(200f to 400f, fitInside(400f, 400f, 2f))
    }

    @Test
    fun wideFrameFitsByWidth() {
        assertEquals(400f to 200f, fitInside(400f, 400f, 0.5f))
    }

    @Test
    fun exactFitFillsContainer() {
        assertEquals(300f to 600f, fitInside(300f, 600f, 2f))
    }

    @Test
    fun zeroAspectIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { fitInside(400f, 400f, 0f) }
    }

    // ---- encode/decodePlacements: giữ vị trí khi xoay màn hình ----

    @Test
    fun placementsSurviveRoundTrip() {
        val map = mapOf(0 to SignaturePlacement(0.5f, 0.5f, 0.3f), 2 to SignaturePlacement(0.25f, 0.75f, 0.5f))
        assertEquals(map, decodePlacements(encodePlacements(map)))
    }

    @Test
    fun encodedPlacementsAreSortedByPage() {
        val map = linkedMapOf(2 to SignaturePlacement(0.25f, 0.75f, 0.5f), 0 to SignaturePlacement(0.5f, 0.5f, 0.3f))
        val data = encodePlacements(map)
        assertEquals(8, data.size)
        assertEquals(0f, data[0], 0f)
        assertEquals(2f, data[4], 0f)
    }

    @Test
    fun emptyPlacementsRoundTrip() {
        assertEquals(0, encodePlacements(emptyMap()).size)
        assertTrue(decodePlacements(FloatArray(0)).isEmpty())
    }

    @Test
    fun brokenDataGivesNoPlacements() {
        assertTrue(decodePlacements(floatArrayOf(1f, 2f)).isEmpty())
        assertTrue(decodePlacements(floatArrayOf(0f, 0.5f, 0.5f, 0.3f, 1f)).isEmpty())
    }

    @Test
    fun negativePageIsDropped() {
        val data = floatArrayOf(-1f, 0.5f, 0.5f, 0.3f, 1f, 0.2f, 0.3f, 0.4f)
        assertEquals(mapOf(1 to SignaturePlacement(0.2f, 0.3f, 0.4f)), decodePlacements(data))
    }

    @Test
    fun nanIsDropped() {
        assertTrue(decodePlacements(floatArrayOf(0f, Float.NaN, 0.5f, 0.3f)).isEmpty())
        assertTrue(decodePlacements(floatArrayOf(Float.NaN, 0.5f, 0.5f, 0.3f)).isEmpty())
    }
}
