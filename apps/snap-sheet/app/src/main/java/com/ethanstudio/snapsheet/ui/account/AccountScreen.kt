package com.ethanstudio.snapsheet.ui.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ethanstudio.snapsheet.ui.theme.LocalDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.auth.AuthError
import com.ethanstudio.snapsheet.auth.AuthUser
import com.ethanstudio.snapsheet.billing.ProKind
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.ui.auth.messageRes
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.ui.main.MainUiState

/** Màu nền nhạt của đầu trang và thẻ Pro (tím lavender sang xanh), có bản cho chế độ tối. */
private object ProfileColors {
    @Composable fun headerTop() = if (LocalDarkTheme.current) Color(0xFF13233F) else Color(0xFFCFE2FF)
    @Composable fun headerMid() = if (LocalDarkTheme.current) Color(0xFF111A2B) else Color(0xFFEAF2FF)
    @Composable fun cardStart() = if (LocalDarkTheme.current) Color(0xFF1A2E52) else Color(0xFFDDEAFF)
    @Composable fun cardEnd() = if (LocalDarkTheme.current) Color(0xFF151C2A) else Color(0xFFF4F8FF)
}

@Composable
fun AccountScreen(
    state: MainUiState,
    user: AuthUser?,
    authBusy: Boolean,
    authError: AuthError?,
    version: String,
    onSignIn: () -> Unit,
    onGetPro: () -> Unit,
    onRestore: () -> Unit,
    onRate: () -> Unit,
    onShareApp: () -> Unit,
    onContact: () -> Unit,
    onPrivacy: () -> Unit,
    onResend: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: (password: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(ProfileColors.headerTop(), ProfileColors.headerMid(), colors.background)),
            ),
        ) {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)) {
                ProfileHeader(user, state.pro.isPro, onSignIn)
                Spacer(Modifier.height(24.dp))
                ProCard(state, onGetPro)
            }
        }
        if (user != null && !user.verified) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.account_not_verified), Modifier.weight(1f), fontSize = 14.sp, color = colors.onSurfaceVariant)
                TextButton(onClick = onResend, enabled = !authBusy) { Text(stringResource(R.string.auth_resend), color = colors.secondary) }
            }
        }
        if (authError != null && authError != AuthError.CANCELED) {
            Text(stringResource(authError.messageRes()), Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontSize = 14.sp, color = colors.error)
        }
        Spacer(Modifier.height(8.dp))
        MenuRow(R.drawable.ic_restore, stringResource(R.string.account_restore), onRestore)
        MenuRow(R.drawable.ic_rate, stringResource(R.string.account_rate), onRate)
        MenuRow(R.drawable.ic_share, stringResource(R.string.account_share), onShareApp)
        MenuDivider()
        MenuRow(R.drawable.ic_headset, stringResource(R.string.account_help), onContact)
        MenuRow(R.drawable.ic_doc, stringResource(R.string.account_privacy), onPrivacy)
        if (user != null) {
            MenuDivider()
            MenuRow(R.drawable.ic_logout, stringResource(R.string.account_sign_out), onSignOut, enabled = !authBusy)
            MenuRow(R.drawable.ic_trash, stringResource(R.string.account_delete), { confirmDelete = true }, enabled = !authBusy, danger = true)
        }
        Text(
            stringResource(R.string.account_version, version),
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
        )
    }
    if (confirmDelete && user != null) {
        DeleteAccountDialog(
            needsPassword = user.usesPassword,
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete(it) },
        )
    }
}

/** Ảnh đại diện tròn có chữ cái đầu, tên, huy hiệu Pro và dòng phụ (email, hoặc lời mời đăng nhập). */
@Composable
private fun ProfileHeader(user: AuthUser?, isPro: Boolean, onSignIn: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(Modifier.size(84.dp).background(Gradients.Primary, CircleShape), contentAlignment = Alignment.Center) {
            if (user != null) {
                Text(user.shownName.take(1).uppercase(), fontSize = 38.sp, fontWeight = FontWeight.Medium, color = Color.White)
            } else {
                Icon(painterResource(R.drawable.ic_person), null, Modifier.size(40.dp), tint = Color.White)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    user?.shownName ?: stringResource(R.string.account_guest),
                    Modifier.weight(1f, fill = false),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isPro) {
                    Surface(shape = RoundedCornerShape(8.dp), color = colors.surface.copy(alpha = 0.6f), border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.5f))) {
                        Text(stringResource(R.string.pro_badge), Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.secondary)
                    }
                }
            }
            if (user != null) {
                Text(user.email.orEmpty(), fontSize = 15.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Row(
                    Modifier.heightIn(min = 40.dp).clickable(role = Role.Button, onClick = onSignIn),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.account_sign_in_link), fontSize = 17.sp, color = colors.secondary, fontWeight = FontWeight.Medium)
                    Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(18.dp), tint = colors.secondary)
                }
            }
        }
    }
}

/** Thẻ Pro: gói đang dùng, nút xem chi tiết, và bốn con số thật của người dùng. */
@Composable
private fun ProCard(state: MainUiState, onGetPro: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pro = state.pro
    val planText = when {
        !pro.isPro -> R.string.account_plan_free
        pro.kind == ProKind.LIFETIME -> R.string.account_plan_lifetime
        else -> R.string.account_plan_sub
    }
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(ProfileColors.cardStart(), ProfileColors.cardEnd())), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_star), null, Modifier.size(22.dp), tint = colors.secondary)
                    Text(stringResource(R.string.pro_badge), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                }
                Text(stringResource(planText), fontSize = 14.sp, color = colors.onSurfaceVariant)
            }
            GradientButton(
                text = stringResource(if (pro.isPro) R.string.account_details else R.string.account_upgrade),
                onClick = onGetPro,
                minHeight = 48.dp,
                fontSize = 16.sp,
            )
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(state.allDocs.size.toString(), stringResource(R.string.stat_docs), Modifier.weight(1f))
            Stat(state.allDocs.sumOf { it.pageCount }.toString(), stringResource(R.string.stat_pages), Modifier.weight(1f))
            Stat(
                if (pro.isPro) stringResource(R.string.stat_unlimited) else state.exportsLeft.toString(),
                stringResource(R.string.stat_exports),
                Modifier.weight(1f),
            )
            Stat(FreeLimits.pageLimit(pro.isPro).toString(), stringResource(R.string.stat_page_limit), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 13.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Một mục trong danh sách: icon nét, nhãn, mũi tên. */
@Composable
private fun MenuRow(@DrawableRes icon: Int, label: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val tint = if (danger) colors.error else colors.onSurface
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(26.dp), tint = tint)
        Text(label, Modifier.weight(1f), fontSize = 18.sp, color = tint)
        Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(20.dp), tint = colors.outline)
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
