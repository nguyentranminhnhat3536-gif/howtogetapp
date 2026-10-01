package com.ethanstudio.lunartasks.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Bật/tắt rung phản hồi theo cài đặt của người dùng. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Trả về hàm rung nhẹ một lần (không làm gì nếu người dùng đã tắt rung). */
@Composable
fun rememberHapticTap(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return { if (enabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
}
