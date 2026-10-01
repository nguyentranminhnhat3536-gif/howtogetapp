package com.ethanstudio.snapsheet.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ethanstudio.snapsheet.ui.theme.Ink

/**
 * Nền "bầu trời" của màn chào, màn đăng nhập và màn mua: gradient xanh nhạt dần về trắng và 3 đám mây mờ.
 * Mây vẽ bằng gradient tròn (không dùng blur vì blur không chạy dưới Android 12).
 */
@Composable
fun SkyBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color(0xFFB9D5FF),
                0.38f to Color(0xFFDCEBFF),
                0.70f to Color(0xFFF3F8FF),
                1f to Color(0xFFFFFFFF),
            ),
        ),
    ) {
        Cloud(Alignment.TopStart, x = (-60).dp, y = 90.dp, size = 120.dp, alpha = 0.75f)
        Cloud(Alignment.TopEnd, x = 70.dp, y = 40.dp, size = 140.dp, alpha = 0.7f)
        Cloud(Alignment.TopStart, x = 40.dp, y = 330.dp, size = 120.dp, alpha = 0.55f)
    }
}

/** Một đám mây: vòng tròn trắng mờ dần ra mép, kéo dãn theo chiều ngang. */
@Composable
private fun BoxScope.Cloud(align: Alignment, x: Dp, y: Dp, size: Dp, alpha: Float) {
    Box(
        Modifier
            .align(align)
            .offset(x = x, y = y)
            .size(size)
            .graphicsLayer { scaleX = 2.2f }
            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent))),
    )
}

/** Thẻ kính: nền trắng trong mờ, viền trắng, bo 26dp. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White),
        contentColor = Ink,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
