package com.ethanstudio.snapsheet.ui.paywall

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.billing.BillingUiState
import com.ethanstudio.snapsheet.billing.Plan
import com.ethanstudio.snapsheet.billing.PlanOffer
import com.ethanstudio.snapsheet.billing.formatMicros
import com.ethanstudio.snapsheet.billing.formatPerMonth
import com.ethanstudio.snapsheet.billing.yearlySavingPercent
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.SkyBackground
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.ui.theme.Ink
import com.ethanstudio.snapsheet.ui.theme.InkSoft
import com.ethanstudio.snapsheet.ui.theme.InkStrong
import com.ethanstudio.snapsheet.ui.theme.displaySerif

/** Viền xanh của thẻ gói đang chọn. */
private val SelectedBorder = Color(0xFF2A6FE8)

/** Nội dung hiện trên một thẻ gói. */
private class PlanCardText(
    val name: String,
    val badge: String?,
    val sub: String?,
    val price: String,
    /** Giá gốc gạch ngang (chỉ khi Google Play có giảm giá thật). */
    val strikePrice: String? = null,
)

/**
 * Màn mua Pro kiểu "bầu trời": ba gói Yearly, Monthly, Lifetime. Giá lấy từ Google Play, không gõ cứng.
 * Không hiện số sao, đánh giá hay số người dùng.
 */
@Composable
fun PaywallScreen(
    billing: BillingUiState,
    onClose: () -> Unit,
    onBuy: (Plan) -> Unit,
    onRestore: () -> Unit,
    onPrivacy: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(Plan.YEARLY) }
    val offers = billing.offers
    val monthly = offers[Plan.MONTHLY]
    val yearly = offers[Plan.YEARLY]
    val saving = if (monthly != null && yearly != null) yearlySavingPercent(monthly.priceMicros, yearly.priceMicros) else null

    Box(Modifier.fillMaxSize().background(Color.White)) {
        SkyBackground()
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TopRow(onClose, onRestore)
                Header()
                Benefits()
                Spacer(Modifier.height(4.dp))
                if (billing.loading && offers.isEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.plan_loading), color = InkSoft)
                    }
                }
                if (!billing.loading && offers.isEmpty()) {
                    Text(
                        stringResource(R.string.paywall_unavailable),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                for (plan in listOf(Plan.YEARLY, Plan.MONTHLY, Plan.LIFETIME)) {
                    val offer = offers[plan]
                    PlanCard(planText(plan, offer, saving), offer != null, selected == plan) { selected = plan }
                }
                Spacer(Modifier.height(8.dp))
                val chosen = offers[selected]
                val trialDays = chosen?.trialDays
                GradientButton(
                    text = if (trialDays != null) stringResource(R.string.paywall_start_trial, trialDays) else stringResource(R.string.paywall_continue),
                    onClick = { onBuy(selected) },
                    enabled = chosen != null,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 58.dp,
                    fontSize = 18.sp,
                )
                if (chosen != null) {
                    Text(
                        termsText(selected, chosen),
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = InkSoft,
                    )
                }
                TextButton(onClick = onPrivacy, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.account_privacy), fontSize = 13.sp, color = Accent)
                }
            }
        }
    }
}

@Composable
private fun TopRow(onClose: () -> Unit, onRestore: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close), tint = Ink) }
        TextButton(onClick = onRestore, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.paywall_restore), color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.paywall_kicker),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.14.em,
            color = InkSoft,
        )
        Text(
            stringResource(R.string.paywall_headline),
            Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = displaySerif(),
            fontSize = 34.sp,
            lineHeight = 38.sp,
            color = InkStrong,
        )
    }
}

/** Ba lợi ích của Pro dạng chip trắng trong mờ, tự xuống dòng khi hẹp. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Benefits() {
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BenefitChip(R.drawable.ic_infinity, stringResource(R.string.pro_benefit_1))
        BenefitChip(R.drawable.ic_text, stringResource(R.string.pro_benefit_2))
        BenefitChip(R.drawable.ic_pages, stringResource(R.string.pro_benefit_3, FreeLimits.PRO_PAGES))
    }
}

@Composable
private fun BenefitChip(@DrawableRes icon: Int, text: String) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Accent)
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
    }
}

/** Dựng chữ cho thẻ gói: tên, nhãn góc, dòng phụ và giá theo bảng trong kế hoạch. */
@Composable
private fun planText(plan: Plan, offer: PlanOffer?, saving: Int?): PlanCardText {
    val name = stringResource(
        when (plan) {
            Plan.YEARLY -> R.string.plan_yearly
            Plan.MONTHLY -> R.string.plan_monthly
            Plan.LIFETIME -> R.string.plan_lifetime
        },
    )
    if (offer == null) return PlanCardText(name, null, null, stringResource(R.string.plan_unavailable))
    val locale = LocalConfiguration.current.locales[0]
    return when (plan) {
        Plan.YEARLY -> {
            val perMonth = formatPerMonth(offer.priceMicros, offer.currencyCode, locale)
            PlanCardText(
                name = name,
                badge = saving?.let { stringResource(R.string.plan_save, it) },
                sub = stringResource(R.string.plan_sub_year, offer.price),
                price = if (perMonth != null) stringResource(R.string.plan_per_month, perMonth) else stringResource(R.string.plan_price_year, offer.price),
            )
        }
        Plan.MONTHLY -> PlanCardText(
            name = name,
            badge = offer.trialDays?.let { stringResource(R.string.plan_trial_days, it) },
            sub = stringResource(if (offer.trialDays != null) R.string.plan_sub_month_trial else R.string.plan_sub_month),
            price = stringResource(R.string.plan_per_month, offer.price),
        )
        Plan.LIFETIME -> PlanCardText(
            name = name,
            badge = offer.discountPercent?.let { stringResource(R.string.plan_off, it) },
            sub = stringResource(R.string.plan_sub_lifetime),
            price = offer.price,
            strikePrice = offer.fullPriceMicros?.let { formatMicros(it, offer.currencyCode, locale) },
        )
    }
}

/** Dòng điều khoản dưới nút mua, đổi theo gói đang chọn. */
@Composable
private fun termsText(plan: Plan, offer: PlanOffer): String {
    val days = offer.trialDays
    return when (plan) {
        Plan.MONTHLY -> if (days != null) {
            stringResource(R.string.paywall_trial_terms, days, stringResource(R.string.plan_price_month, offer.price))
        } else {
            stringResource(R.string.paywall_terms_month, offer.price)
        }
        Plan.YEARLY -> if (days != null) {
            stringResource(R.string.paywall_trial_terms, days, stringResource(R.string.plan_price_year, offer.price))
        } else {
            stringResource(R.string.paywall_terms_year, offer.price)
        }
        Plan.LIFETIME -> stringResource(R.string.paywall_terms_lifetime, offer.price)
    }
}

@Composable
private fun PlanCard(text: PlanCardText, enabled: Boolean, isSelected: Boolean, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (isSelected) 0.86f else 0.55f))
            .border(2.dp, if (isSelected) SelectedBorder else Color.White.copy(alpha = 0.9f), shape)
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp).padding(top = if (text.badge != null) 8.dp else 0.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                text.sub?.let { Text(it, fontSize = 14.sp, lineHeight = 19.sp, color = InkSoft) }
            }
            Column(Modifier.weight(0.8f, fill = false), horizontalAlignment = Alignment.End) {
                text.strikePrice?.let { strike ->
                    val regular = stringResource(R.string.plan_regular_price, strike)
                    Text(
                        strike,
                        Modifier.semantics { contentDescription = regular },
                        fontSize = 13.sp,
                        color = InkSoft,
                        textDecoration = TextDecoration.LineThrough,
                        textAlign = TextAlign.End,
                    )
                }
                Text(text.price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.End)
            }
        }
        text.badge?.let {
            Text(
                it,
                Modifier
                    .background(Gradients.Primary, RoundedCornerShape(topStart = 18.dp, bottomEnd = 12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
