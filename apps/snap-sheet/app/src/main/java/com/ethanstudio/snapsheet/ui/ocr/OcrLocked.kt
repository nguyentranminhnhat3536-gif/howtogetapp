package com.ethanstudio.snapsheet.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.FreeLimits
import com.ethanstudio.snapsheet.ui.common.GradientButton
import com.ethanstudio.snapsheet.ui.common.ProTag
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.InkStrong
import com.ethanstudio.snapsheet.ui.theme.displaySerif

/** Màu các vạch giả làm dòng chữ (không có chữ thật: bản miễn phí không chạy OCR). */
private val FakeLine = Color(0xFFC9D6EE)
private val FakeLineWidths = listOf(0.60f, 0.95f, 0.88f, 0.92f, 0.70f, 0.90f, 0.84f, 0.66f, 0.93f, 0.78f)

/** Màn khóa của Extract text cho bản miễn phí: trang mờ phía trên, bảng PRO phía dưới. */
@Composable
fun OcrLockedContent(onSeePlans: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            BlurredPage(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
            LockedPanel(onSeePlans, onNotNow, Modifier.padding(top = 16.dp))
        }
    }
}

/** Thẻ trang có các vạch xám mờ dần, thay cho chữ. */
@Composable
private fun BlurredPage(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.fillMaxWidth().height(330.dp).clip(shape).background(colors.surface).border(1.dp, colors.outlineVariant, shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp).blur(3.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            FakeLineWidths.forEach { width ->
                Box(Modifier.fillMaxWidth(width).height(9.dp).background(FakeLine, RoundedCornerShape(4.dp)))
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(0f to colors.surface.copy(alpha = 0f), 0.85f to colors.surface)),
        )
    }
}

/** Bảng PRO: tiêu đề, 3 lợi ích, nút xem gói và nút "Để sau". */
@Composable
private fun LockedPanel(onSeePlans: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFFB9D5FF),
                    0.38f to Color(0xFFDCEBFF),
                    0.70f to Color(0xFFF3F8FF),
                    1f to Color(0xFFFFFFFF),
                ),
            )
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProTag()
        Text(
            stringResource(R.string.ocr_locked_title),
            fontFamily = displaySerif(),
            fontSize = 30.sp,
            lineHeight = 34.sp,
            color = InkStrong,
        )
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Benefit(stringResource(R.string.ocr_locked_1))
            Benefit(stringResource(R.string.ocr_locked_2))
            Benefit(stringResource(R.string.ocr_locked_3, FreeLimits.PRO_PAGES))
        }
        GradientButton(
            stringResource(R.string.ocr_see_plans),
            onSeePlans,
            Modifier.fillMaxWidth().padding(top = 4.dp),
            minHeight = 56.dp,
            fontSize = 17.sp,
        )
        TextButton(onClick = onNotNow, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.ocr_not_now), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp), tint = Accent)
        Text(text, fontSize = 15.sp, color = InkStrong)
    }
}
