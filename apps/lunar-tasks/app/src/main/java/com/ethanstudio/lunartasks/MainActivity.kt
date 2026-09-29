package com.ethanstudio.lunartasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethanstudio.lunartasks.data.AppSettings
import com.ethanstudio.lunartasks.ui.AppRoot
import com.ethanstudio.lunartasks.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as LunarTasksApp).settings
        setContent {
            val display by settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            AppTheme(largeText = display.largeText, highContrast = display.highContrast) {
                AppRoot()
            }
        }
    }
}
