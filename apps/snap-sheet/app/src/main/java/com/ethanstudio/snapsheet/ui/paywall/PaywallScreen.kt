package com.ethanstudio.snapsheet.ui.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.billing.BillingUiState
import com.ethanstudio.snapsheet.billing.Plan
import com.ethanstudio.snapsheet.billing.PlanOffer
import com.ethanstudio.snapsheet.billing.yearlySavingPercent
import com.ethanstudio.snapsheet.data.FreeLimits

/** Màn mua Pro với ba gói: tháng, năm, vĩnh viễn. Giá lấy từ Google Play, không gõ cứng. */
@Composable
fun PaywallScreen(
    billing: BillingUiState,
    onClose: () -> Unit,
    onBuy: (Plan) -> Unit,
    onRestore: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(Plan.YEARLY) }
    val offers = billing.offers
    val monthly = offers[Plan.MONTHLY]
    val yearly = offers[Plan.YEARLY]
    val saving = if (monthly != null && yearly != null) yearlySavingPercent(monthly.priceMicros, yearly.priceMicros) else null

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close)) }
                TextButton(onClick = onRestore) {
                    Text(stringResource(R.string.paywall_restore), color = MaterialTheme.colorScheme.secondary, fontSize = 17.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.paywall_title_accent) + " ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    stringResource(R.string.paywall_title_rest),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Feature(R.drawable.ic_infinity, stringResource(R.string.pro_benefit_1))
                Feature(R.drawable.ic_text, stringResource(R.string.pro_benefit_2))
                Feature(R.drawable.ic_doc, stringResource(R.string.pro_benefit_3, FreeLimits.PRO_PAGES))
                Feature(R.drawable.ic_star, stringResource(R.string.pro_benefit_4))
            }
            if (!billing.loading && offers.isEmpty()) {
                Text(
                    stringResource(R.string.paywall_unavailable),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PlanCard(Plan.MONTHLY, stringResource(R.string.plan_monthly), monthly, saving = null, selected == Plan.MONTHLY) { selected = Plan.MONTHLY }
            PlanCard(Plan.YEARLY, stringResource(R.string.plan_yearly), yearly, saving, selected == Plan.YEARLY) { selected = Plan.YEARLY }
            PlanCard(Plan.LIFETIME, stringResource(R.string.plan_lifetime), offers[Plan.LIFETIME], saving = null, selected == Plan.LIFETIME) { selected = Plan.LIFETIME }
            Button(
                onClick = { onBuy(selected) },
                enabled = offers[selected] != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text(stringResource(R.string.paywall_continue), fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            Text(
                stringResource(R.string.paywall_legal),
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Feature(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onBackground)
        Text(text, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun PlanCard(plan: Plan, name: String, offer: PlanOffer?, saving: Int?, isSelected: Boolean, onSelect: () -> Unit) {
    val color = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(selected = isSelected, enabled = offer != null, role = Role.RadioButton, onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) color.primaryContainer else color.surface,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color.primary else color.outlineVariant),
    ) {
        Row(
            Modifier.heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = isSelected, onClick = null, enabled = offer != null)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = color.onSurface)
                    if (saving != null) {
                        Surface(shape = CircleShape, color = color.secondary) {
                            Text(
                                stringResource(R.string.plan_save, saving),
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color.onSecondary,
                            )
                        }
                    }
                }
                if (offer?.hasFreeTrial == true) {
                    Text(stringResource(R.string.plan_trial), fontSize = 12.sp, color = color.onSurfaceVariant)
                }
            }
            Text(
                if (offer == null) stringResource(R.string.plan_unavailable) else priceText(plan, offer.price),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = color.onSurface,
            )
        }
    }
}

@Composable
private fun priceText(plan: Plan, price: String): String = when (plan) {
    Plan.MONTHLY -> stringResource(R.string.plan_price_month, price)
    Plan.YEARLY -> stringResource(R.string.plan_price_year, price)
    Plan.LIFETIME -> stringResource(R.string.plan_price_once, price)
}
