package com.ethanstudio.snapsheet.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.ui.theme.Accent
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.util.decodeSampledCached
import java.text.BreakIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Nhãn PRO nhỏ: chữ trắng đậm trên nền gradient xanh. */
@Composable
fun ProTag(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.pro_badge),
        modifier = modifier.background(Gradients.Primary, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Đầu trang của một tab: nền gradient xanh nhạt, né thanh trạng thái. */
@Composable
fun ScreenHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Gradients.header()).statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** Lối tắt tròn: vòng tròn có icon và nhãn ngắn (tối đa 2 dòng) bên dưới. */
@Composable
fun ShortcutCircle(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 48.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(56.dp).background(Gradients.soft(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(26.dp), tint = Accent)
        }
        Text(
            label,
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Ô công cụ trong lưới: icon, tên, mô tả ngắn; nhãn PRO ở góc phải trên nếu cần Pro. */
@Composable
fun ToolTile(@DrawableRes icon: Int, title: String, desc: String, pro: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(48.dp).background(Gradients.soft(), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(24.dp), tint = Accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text(desc, fontSize = 13.sp, lineHeight = 17.sp, color = colors.onSurfaceVariant)
            }
        }
        if (pro) ProTag(Modifier.align(Alignment.TopEnd))
    }
}

/** Ảnh trang nhỏ, đọc ngoài luồng chính. */
@Composable
fun PageThumbnail(file: File, modifier: Modifier = Modifier, target: Int = 256, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap by produceState<ImageBitmap?>(null, file) {
        value = withContext(Dispatchers.IO) { decodeSampledCached(file, target)?.asImageBitmap() }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = contentScale) }
    }
}

/** Một hàng tài liệu: ảnh trang đầu, tên, dòng phụ dựng sẵn (ngày, số trang, dung lượng…). */
@Composable
fun DocRow(doc: Doc, firstPage: File, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageThumbnail(
            firstPage,
            Modifier.size(width = 46.dp, height = 60.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(doc.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_chevron), null, tint = colors.outline)
    }
}

/** Ngày tạo của tài liệu, kiểu ngắn gọn theo ngôn ngữ đang hiện (ví dụ "Oct 1, 2026"). */
@Composable
fun docDate(doc: Doc): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(doc.createdAt))
}

/** Trạng thái trống: hình, tiêu đề, một câu hướng dẫn. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_scan), null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        Text(body, fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Nhóm hàng cài đặt: thẻ trắng bo tròn, các hàng cách nhau bằng đường kẻ mảnh. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column { content() }
    }
}

@Composable
fun SettingsRow(@DrawableRes icon: Int, label: String, showDivider: Boolean, onClick: () -> Unit, detail: String? = null) {
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick).padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
        Text(label, Modifier.weight(1f), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        if (detail != null) Text(detail, Modifier.width(104.dp), fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

/** Chữ cái đầu viết hoa của tên, lấy trọn một ký tự hiển thị (giữ dấu tiếng Việt, emoji, chữ ngoài BMP). Tên rỗng → "". */
fun initialOf(name: String): String {
    val text = name.trim()
    if (text.isEmpty()) return ""
    val graphemes = BreakIterator.getCharacterInstance()
    graphemes.setText(text)
    val end = graphemes.next().takeIf { it != BreakIterator.DONE } ?: text.length
    return text.substring(0, end).uppercase()
}
