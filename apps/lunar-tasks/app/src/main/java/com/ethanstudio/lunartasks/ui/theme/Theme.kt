package com.ethanstudio.lunartasks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = BrandSecondaryDark,
)

private val AppTypography = Typography()

/** Hệ số phóng chữ khi bật "Chữ to" (cộng dồn với cỡ chữ đã đặt trong Cài đặt của máy). */
const val LARGE_TEXT_SCALE = 1.3f

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    largeText: Boolean = false,
    highContrast: Boolean = false,
    // Tắt để giữ màu thương hiệu (ảnh store nhất quán). Bật nếu muốn theo màu hình nền (Android 12+).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val colorScheme = if (highContrast) baseScheme.highContrast(darkTheme) else baseScheme
    val density = LocalDensity.current
    val scaledDensity = if (largeText) Density(density.density, density.fontScale * LARGE_TEXT_SCALE) else density

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Tương phản cao: chữ phụ cũng đậm như chữ chính, nền thuần trắng / thuần đen. */
private fun ColorScheme.highContrast(dark: Boolean): ColorScheme {
    val background = if (dark) Color.Black else Color.White
    val foreground = if (dark) Color.White else Color.Black
    return copy(
        background = background,
        onBackground = foreground,
        surface = background,
        onSurface = foreground,
        onSurfaceVariant = foreground,
        surfaceContainerLow = if (dark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0),
        outline = foreground,
        primary = if (dark) Color(0xFFFFD54F) else Color(0xFF002080),
        onPrimary = if (dark) Color.Black else Color.White,
        primaryContainer = if (dark) Color(0xFF002080) else Color(0xFFFFE082),
        onPrimaryContainer = if (dark) Color.White else Color.Black,
    )
}
