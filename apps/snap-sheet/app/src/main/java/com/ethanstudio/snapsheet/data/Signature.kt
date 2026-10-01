package com.ethanstudio.snapsheet.data

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Khung ký: rộng = 2 × cao. */
const val SIGN_PAD_ASPECT = 2f

/** Bề dày nét = 0,8% bề rộng khung ký. */
const val SIGN_STROKE_FRACTION = 0.008f

/** Ảnh chữ ký dựng ở 1200 × 600 rồi cắt sát nét. */
const val SIGN_BITMAP_WIDTH = 1200

/** Bề rộng chữ ký trên trang (tỉ lệ bề rộng trang): nhỏ nhất, lớn nhất. */
const val SIGN_MIN_WIDTH = 0.1f
const val SIGN_MAX_WIDTH = 0.8f

/** Chiều cao chữ ký tối đa (tỉ lệ chiều cao trang). */
const val SIGN_MAX_HEIGHT = 0.9f

/** Hai điểm liên tiếp của một nét phải cách nhau ít nhất chừng này (tỉ lệ khung ký). */
const val SIGN_MIN_STEP = 0.004f

/** Một điểm của nét ký, toạ độ 0..1 theo khung ký. */
data class StrokePoint(val x: Float, val y: Float)

/** Khung chữ nhật theo tỉ lệ 0..1. */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Khung chữ nhật theo điểm ảnh. */
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** Vị trí chữ ký trên trang: tâm ([cx], [cy]) theo tỉ lệ trang; [widthFraction] = bề rộng chữ ký / bề rộng trang. */
data class SignaturePlacement(val cx: Float, val cy: Float, val widthFraction: Float)

/** Vị trí mặc định: góc dưới phải, rộng 35% trang. */
val DEFAULT_PLACEMENT = SignaturePlacement(0.7f, 0.85f, 0.35f)

/** Khung bao mọi điểm của mọi nét; không có điểm nào → null. */
fun strokeBounds(strokes: List<List<StrokePoint>>): NormRect? {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    var any = false
    for (stroke in strokes) {
        for (p in stroke) {
            any = true
            left = minOf(left, p.x)
            top = minOf(top, p.y)
            right = maxOf(right, p.x)
            bottom = maxOf(bottom, p.y)
        }
    }
    return if (any) NormRect(left, top, right, bottom) else null
}

/** Dùng được khi có ít nhất một nét có từ 2 điểm trở lên (một chấm thì chưa phải chữ ký). */
fun isSignatureUsable(strokes: List<List<StrokePoint>>): Boolean = strokes.any { it.size >= 2 }

/** Thêm điểm [p] (kẹp vào 0..1) vào cuối nét; quá gần điểm cuối (dưới [minStep]) thì giữ nguyên nét. */
fun appendPoint(stroke: List<StrokePoint>, p: StrokePoint, minStep: Float = SIGN_MIN_STEP): List<StrokePoint> {
    val point = StrokePoint(p.x.coerceIn(0f, 1f), p.y.coerceIn(0f, 1f))
    val last = stroke.lastOrNull() ?: return listOf(point)
    return if (hypot(point.x - last.x, point.y - last.y) < minStep) stroke else stroke + point
}

/** Vùng cắt (điểm ảnh) quanh [bounds] trên ảnh [bitmapW] × [bitmapH], chừa lề [marginPx]; luôn nằm trong ảnh, rộng và cao ≥ 1. */
fun cropRect(bounds: NormRect, bitmapW: Int, bitmapH: Int, marginPx: Int): PixelRect {
    val left = (floor(bounds.left * bitmapW).toInt() - marginPx).coerceIn(0, bitmapW - 1)
    val top = (floor(bounds.top * bitmapH).toInt() - marginPx).coerceIn(0, bitmapH - 1)
    var right = (ceil(bounds.right * bitmapW).toInt() + marginPx).coerceIn(0, bitmapW)
    var bottom = (ceil(bounds.bottom * bitmapH).toInt() + marginPx).coerceIn(0, bitmapH)
    if (right - left < 1) right = left + 1
    if (bottom - top < 1) bottom = top + 1
    return PixelRect(left, top, right, bottom)
}

/**
 * Giữ chữ ký nằm gọn trong trang. [sigAspect] = cao/rộng của chữ ký, [pageAspect] = cao/rộng của trang.
 * Bề rộng trong 10%..80%; chữ ký quá cao so với trang thì thu hẹp để cao tối đa 90% trang; tâm không cho ra ngoài trang.
 */
fun clampPlacement(p: SignaturePlacement, sigAspect: Float, pageAspect: Float): SignaturePlacement {
    require(sigAspect > 0f && pageAspect > 0f) { "Invalid aspect" }
    var width = p.widthFraction.coerceIn(SIGN_MIN_WIDTH, SIGN_MAX_WIDTH)
    var height = width * sigAspect / pageAspect
    if (height > SIGN_MAX_HEIGHT) {
        width = SIGN_MAX_HEIGHT * pageAspect / sigAspect
        height = SIGN_MAX_HEIGHT
    }
    return SignaturePlacement(
        cx = p.cx.coerceIn(width / 2, 1 - width / 2),
        cy = p.cy.coerceIn(height / 2, 1 - height / 2),
        widthFraction = width,
    )
}

/** Khung (điểm ảnh) để vẽ chữ ký lên trang [pageW] × [pageH]. */
fun placementRect(p: SignaturePlacement, sigAspect: Float, pageW: Int, pageH: Int): PixelRect {
    val widthPx = p.widthFraction * pageW
    val heightPx = widthPx * sigAspect
    val left = (p.cx * pageW - widthPx / 2).roundToInt()
    val top = (p.cy * pageH - heightPx / 2).roundToInt()
    return PixelRect(left, top, left + widthPx.roundToInt(), top + heightPx.roundToInt())
}

/** Khung lớn nhất có cao/rộng = [aspect] nằm vừa trong khung [containerW] × [containerH]. */
fun fitInside(containerW: Float, containerH: Float, aspect: Float): Pair<Float, Float> {
    require(aspect > 0f) { "Invalid aspect" }
    return if (containerW * aspect <= containerH) containerW to containerW * aspect else containerH / aspect to containerH
}

/** Ghi các vị trí thành dãy số [trang, cx, cy, rộng] nối tiếp, theo trang tăng dần (để lưu vào SavedStateHandle). */
fun encodePlacements(map: Map<Int, SignaturePlacement>): FloatArray {
    val out = FloatArray(map.size * PLACEMENT_FIELDS)
    map.entries.sortedBy { it.key }.forEachIndexed { i, entry ->
        val base = i * PLACEMENT_FIELDS
        out[base] = entry.key.toFloat()
        out[base + 1] = entry.value.cx
        out[base + 2] = entry.value.cy
        out[base + 3] = entry.value.widthFraction
    }
    return out
}

/** Đọc lại dãy số của [encodePlacements]. Độ dài sai → rỗng; bỏ bộ có trang âm hoặc có NaN. */
fun decodePlacements(data: FloatArray): Map<Int, SignaturePlacement> {
    if (data.size % PLACEMENT_FIELDS != 0) return emptyMap()
    val out = LinkedHashMap<Int, SignaturePlacement>()
    for (base in data.indices step PLACEMENT_FIELDS) {
        val page = data[base]
        val cx = data[base + 1]
        val cy = data[base + 2]
        val width = data[base + 3]
        if (page.isNaN() || cx.isNaN() || cy.isNaN() || width.isNaN() || page < 0f) continue
        out[page.toInt()] = SignaturePlacement(cx, cy, width)
    }
    return out
}

private const val PLACEMENT_FIELDS = 4
