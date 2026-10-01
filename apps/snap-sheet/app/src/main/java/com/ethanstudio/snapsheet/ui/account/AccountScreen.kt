package com.ethanstudio.snapsheet.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.billing.ProKind
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.ui.common.ProBadge
import com.ethanstudio.snapsheet.ui.common.SettingsGroup
import com.ethanstudio.snapsheet.ui.common.SettingsRow
import com.ethanstudio.snapsheet.ui.common.TabHeader
import com.ethanstudio.snapsheet.ui.main.MainUiState

@Composable
fun AccountScreen(
    state: MainUiState,
    version: String,
    onGetPro: () -> Unit,
    onRestore: () -> Unit,
    onRate: () -> Unit,
    onShareApp: () -> Unit,
    onContact: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.account_title), state.pro.isPro)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumCard(state, onGetPro)
            SettingsGroup {
                SettingsRow(R.drawable.ic_star, stringResource(R.string.account_get_pro), false, onGetPro)
                SettingsRow(R.drawable.ic_restore, stringResource(R.string.account_restore), true, onRestore)
            }
            SettingsGroup {
                SettingsRow(R.drawable.ic_rate, stringResource(R.string.account_rate), false, onRate)
                SettingsRow(R.drawable.ic_share, stringResource(R.string.account_share), true, onShareApp)
                SettingsRow(R.drawable.ic_headset, stringResource(R.string.account_contact), true, onContact)
                SettingsRow(R.drawable.ic_doc, stringResource(R.string.account_privacy), true, onPrivacy)
            }
            Text(
                stringResource(R.string.account_version, version),
                Modifier.fillMaxWidth().padding(8.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Thẻ quyền lợi Pro: người chưa mua thấy danh sách quyền lợi và nút nâng cấp; người đã mua thấy gói đang dùng. */
@Composable
private fun PremiumCard(state: MainUiState, onGetPro: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.app_name_short),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ProBadge()
            }
            if (state.pro.isPro) {
                val label = if (state.pro.kind == ProKind.LIFETIME) R.string.pro_active_lifetime else R.string.pro_active_sub
                Text(stringResource(label), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Benefit(R.string.pro_benefit_1)
                Benefit(R.string.pro_benefit_2)
                Text(
                    stringResource(R.string.pro_benefit_3, FreeLimits.PRO_PAGES),
                    Modifier.padding(start = 34.dp),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.exports_left, state.exportsLeft),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onGetPro,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) { Text(stringResource(R.string.account_get_pro), fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun Benefit(textRes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp)) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onSecondary)
        }
        Text(stringResource(textRes), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
