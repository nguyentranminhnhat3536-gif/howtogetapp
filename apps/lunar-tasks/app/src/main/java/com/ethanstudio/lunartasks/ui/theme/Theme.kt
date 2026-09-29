package com.ethanstudio.lunartasks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Nhân thêm với cỡ chữ đã đặt trong Cài đặt của máy. */
    textScale: Float = 1f,
    highContrast: Boolean = false,
    // Tắt để giữ màu thương hiệu (ảnh store nhất quán). Bật nếu muốn theo màu hình nền (Android 12+).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        highContrast -> HighContrastColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val density = LocalDensity.current
    val scaledDensity = if (textScale != 1f) Density(density.density, density.fontScale * textScale) else density

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/**
 * Tương phản cao: chữ vàng trên nền đen (dễ đọc nhất với người thị lực kém),
 * dùng cho cả chế độ sáng lẫn tối.
 */
private val HighContrastYellow = Color(0xFFFFE14D)
private val HighContrastColors = darkColorScheme(
    primary = HighContrastYellow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1C1C00),
    onPrimaryContainer = HighContrastYellow,
    secondary = HighContrastYellow,
    onSecondary = Color.Black,
    secondaryContainer = HighContrastYellow,
    onSecondaryContainer = Color.Black,
    background = Color.Black,
    onBackground = HighContrastYellow,
    surface = Color.Black,
    onSurface = HighContrastYellow,
    onSurfaceVariant = HighContrastYellow,
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF262626),
    outline = HighContrastYellow,
    error = Color(0xFFFF8A80),
    onError = Color.Black,
)
