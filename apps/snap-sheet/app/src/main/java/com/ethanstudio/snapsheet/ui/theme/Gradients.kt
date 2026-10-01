package com.ethanstudio.snapsheet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient xanh của app (nền chủ đạo trắng, xanh là màu phụ).
 * Gradient nút đi từ #2A6FE8 tới #1747C9: chữ trắng đạt ≥ 4.6:1 ở mọi điểm.
 */
object Gradients {
    val ButtonStart = Color(0xFF2A6FE8)
    val ButtonEnd = Color(0xFF1747C9)

    /** Nút chính, nút +, ảnh đại diện. */
    val Primary: Brush = Brush.linearGradient(listOf(ButtonStart, ButtonEnd))

    /** Dải nền đầu màn hình: xanh rất nhạt chuyển dần về nền. */
    @Composable
    fun header(): Brush = if (isSystemInDarkTheme()) {
        Brush.verticalGradient(listOf(Color(0xFF13233F), Color(0xFF0F1114)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFD6E6FF), Color(0xFFFFFFFF)))
    }

    /** Nền nhẹ cho thẻ, vòng tròn icon, thẻ Pro. */
    @Composable
    fun soft(): Brush = if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color(0xFF1A2A48), Color(0xFF151C2A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFE3EEFF), Color(0xFFF5F9FF)))
    }
}
