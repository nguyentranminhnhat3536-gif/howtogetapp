package com.ethanstudio.snapsheet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethanstudio.snapsheet.ui.AppRoot
import com.ethanstudio.snapsheet.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
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
