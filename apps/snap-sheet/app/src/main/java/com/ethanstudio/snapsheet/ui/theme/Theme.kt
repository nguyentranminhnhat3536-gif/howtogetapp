package com.ethanstudio.snapsheet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryTint,
    onPrimaryContainer = PrimaryStrong,
    secondary = PrimaryStrong,
    onSecondary = OnPrimary,
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Fill,
    onSurfaceVariant = MutedAa,
    outline = Muted,
    outlineVariant = Hairline,
    error = Danger,
    onError = OnPrimary,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryTintDark,
    onPrimaryContainer = PrimaryStrongDark,
    secondary = PrimaryStrongDark,
    onSecondary = OnPrimaryDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = FillDark,
    onSurfaceVariant = MutedDark,
    outline = MutedDark,
    outlineVariant = HairlineDark,
    error = DangerDark,
    onError = OnPrimaryDark,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** App đang dùng giao diện tối hay không (theo AppTheme, không theo cài đặt máy). */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Giao diện chủ đạo trắng, xanh là màu phụ (theo yêu cầu của Ethan, 2026-10-01): app luôn dùng bản sáng,
 * kể cả khi máy bật chế độ tối. Bộ màu tối vẫn giữ để bật lại sau này (truyền darkTheme = true).
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = AppShapes,
            content = content,
        )
    }
}
