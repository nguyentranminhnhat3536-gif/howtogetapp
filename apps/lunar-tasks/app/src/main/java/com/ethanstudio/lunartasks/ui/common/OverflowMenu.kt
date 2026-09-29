package com.ethanstudio.lunartasks.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ethanstudio.lunartasks.R
import com.ethanstudio.lunartasks.util.openStoreListing
import com.ethanstudio.lunartasks.util.openUrl
import com.ethanstudio.lunartasks.util.sendFeedbackEmail

/** Menu ba chấm: chính sách quyền riêng tư, đánh giá, góp ý. [onNoApp] khi máy không mở được. */
@Composable
fun OverflowMenu(onNoApp: () -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val appName = stringResource(R.string.app_name)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val supportEmail = stringResource(R.string.support_email)
    val feedbackSubject = stringResource(R.string.feedback_subject, appName)

    fun run(action: () -> Boolean) {
        open = false
        if (!action()) onNoApp()
    }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.menu_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_privacy)) },
                onClick = { run { context.openUrl(privacyUrl) } },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_rate)) },
                onClick = { run { context.openStoreListing() } },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_feedback)) },
                onClick = { run { context.sendFeedbackEmail(supportEmail, feedbackSubject) } },
            )
        }
    }
}
