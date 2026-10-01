package com.example.factorytemplate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.factorytemplate.R
import com.example.factorytemplate.ui.home.HomeScreen
import com.example.factorytemplate.util.openStoreListing
import com.example.factorytemplate.util.openUrl
import com.example.factorytemplate.util.sendFeedbackEmail
import kotlinx.coroutines.launch

/** Khung chung của app: thanh tiêu đề + menu (chính sách, đánh giá, góp ý) + nội dung. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }

    val appName = stringResource(R.string.app_name)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val supportEmail = stringResource(R.string.support_email)
    val feedbackSubject = stringResource(R.string.feedback_subject, appName)
    val noAppMessage = stringResource(R.string.error_no_app)

    fun runOrReport(action: () -> Boolean) {
        menuOpen = false
        if (!action()) {
            scope.launch { snackbarHostState.showSnackbar(noAppMessage) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.menu_more),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_privacy)) },
                                onClick = { runOrReport { context.openUrl(privacyUrl) } },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_rate)) },
                                onClick = { runOrReport { context.openStoreListing() } },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_feedback)) },
                                onClick = { runOrReport { context.sendFeedbackEmail(supportEmail, feedbackSubject) } },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        HomeScreen(modifier = Modifier.padding(innerPadding))
    }
}
