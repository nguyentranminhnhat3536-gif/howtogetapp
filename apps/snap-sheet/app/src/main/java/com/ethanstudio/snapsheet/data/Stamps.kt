package com.ethanstudio.snapsheet.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.roundToInt

/** Màu mực chữ ký (xanh đen). Đây là màu in lên giấy nên cố ý giữ cố định ở mọi giao diện. */
const val SIGN_INK: Int = 0xFF0B1A3A.toInt()

/**
 * Vẽ chữ mờ chéo giữa trang khổ [width] × [height]. Dùng chung cho ảnh xem trước và bản lưu,
 * nên xem trước giống hệt kết quả. Font đậm của hệ thống hiện được tiếng Việt có dấu, Hindi, Nhật.
 */
fun drawWatermark(canvas: Canvas, width: Int, height: Int, spec: WatermarkSpec) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        color = ((spec.strength.alpha * 255).roundToInt() shl 24) or spec.color.rgb
        textSize = 100f
    }
    val widthAt100 = paint.measureText(spec.text).coerceAtLeast(1f)
    paint.textSize = watermarkTextSize(width, height, widthAt100)
    val metrics = paint.fontMetrics
    canvas.save()
    canvas.rotate(watermarkAngle(width, height), width / 2f, height / 2f)
    canvas.drawText(spec.text, width / 2f, height / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
    canvas.restore()
}

/** Vẽ ảnh chữ ký vào khung [rect] (điểm ảnh) của trang. */
fun drawSignature(canvas: Canvas, signature: Bitmap, rect: PixelRect) {
    canvas.drawBitmap(
        signature,
        null,
        Rect(rect.left, rect.top, rect.right, rect.bottom),
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
    )
}

/**
 * Dựng ảnh chữ ký nền trong suốt từ các nét (toạ độ 0..1 theo khung ký 2:1), rồi cắt sát nét.
 * Không có điểm nào → null.
 */
fun renderSignature(strokes: List<List<StrokePoint>>): Bitmap? {
    val bounds = strokeBounds(strokes) ?: return null
    val width = SIGN_BITMAP_WIDTH
    val height = (SIGN_BITMAP_WIDTH / SIGN_PAD_ASPECT).roundToInt()
    val lineWidth = width * SIGN_STROKE_FRACTION
    val full = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(full)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SIGN_INK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = lineWidth
    }
    for (stroke in strokes) {
        when (stroke.size) {
            0 -> Unit
            1 -> canvas.drawPoint(stroke[0].x * width, stroke[0].y * height, paint)
            else -> {
                val path = Path()
                path.moveTo(stroke[0].x * width, stroke[0].y * height)
                for (i in 1 until stroke.size) path.lineTo(stroke[i].x * width, stroke[i].y * height)
                canvas.drawPath(path, paint)
            }
        }
    }
    val crop = cropRect(bounds, width, height, (2 * lineWidth).roundToInt())
    val cropped = Bitmap.createBitmap(full, crop.left, crop.top, crop.width, crop.height)
    if (cropped !== full) full.recycle()
    return cropped
}
