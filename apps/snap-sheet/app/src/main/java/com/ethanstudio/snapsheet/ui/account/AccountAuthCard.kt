package com.ethanstudio.snapsheet.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.auth.AuthError
import com.ethanstudio.snapsheet.auth.AuthUser
import com.ethanstudio.snapsheet.ui.auth.messageRes

/**
 * Thẻ tài khoản ở đầu tab Account. Chưa đăng nhập: lời mời đăng nhập (không bắt buộc).
 * Đã đăng nhập: email, trạng thái xác nhận, Đăng xuất, Xóa tài khoản (Google Play bắt buộc có).
 */
@Composable
fun AccountAuthCard(
    user: AuthUser?,
    busy: Boolean,
    error: AuthError?,
    onSignIn: () -> Unit,
    onResend: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: (password: String?) -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (user == null) {
                Text(stringResource(R.string.account_sign_in_prompt), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.account_sign_in_body), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                ) { Text(stringResource(R.string.auth_sign_in), fontWeight = FontWeight.SemiBold) }
            } else {
                Text(stringResource(R.string.account_signed_in_as), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(user.email.orEmpty(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (user.verified) {
                    Text(stringResource(R.string.account_verified), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.account_not_verified), Modifier.padding(top = 14.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onResend, enabled = !busy) { Text(stringResource(R.string.auth_resend), color = MaterialTheme.colorScheme.secondary) }
                    }
                }
                if (error != null && error != AuthError.CANCELED) {
                    Text(stringResource(error.messageRes()), fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.account_sign_out))
                    }
                    TextButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmDelete && user != null) {
        DeleteAccountDialog(
            needsPassword = user.usesPassword,
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete(it) },
        )
    }
}

@Composable
private fun DeleteAccountDialog(needsPassword: Boolean, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.account_delete_body))
                if (needsPassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.account_delete_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (needsPassword) password else null) }, enabled = !needsPassword || password.isNotEmpty()) {
                Text(stringResource(R.string.account_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
