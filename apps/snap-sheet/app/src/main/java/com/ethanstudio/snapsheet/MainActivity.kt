package com.ethanstudio.snapsheet

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethanstudio.snapsheet.i18n.AppLocale
import com.ethanstudio.snapsheet.ui.AppRoot
import com.ethanstudio.snapsheet.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    // Android 8–12: áp ngôn ngữ người dùng chọn trong app (Android 13+ hệ thống tự làm).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            AppTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as SnapSheetApp).billing.refresh()
    }
}
