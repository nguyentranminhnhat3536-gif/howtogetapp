package com.ethanstudio.snapsheet.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethanstudio.snapsheet.R
import com.ethanstudio.snapsheet.data.Doc
import com.ethanstudio.snapsheet.ui.theme.Gradients
import com.ethanstudio.snapsheet.util.decodeSampledCached
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Huy hiệu PRO: nền xanh đậm (đủ tương phản với chữ trắng), ngôi sao và chữ PRO. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondary) {
        Row(
            modifier = Modifier.heightIn(min = 23.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(painterResource(R.drawable.ic_star), null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondary)
            Text(
                stringResource(R.string.pro_badge),
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Hàng tiêu đề của một tab: tên ở giữa, huy hiệu PRO bên trái nếu đã mua. */
@Composable
fun TabHeader(title: String, isPro: Boolean) {
    Box(Modifier.background(Gradients.header())) {
        Box(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (isPro) ProBadge(Modifier.align(Alignment.CenterStart))
            Text(
                title,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 72.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
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

/** Lối tắt tròn trên trang chủ: vòng tròn có icon và nhãn ngắn bên dưới. */
@Composable
fun QuickAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = 48.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(56.dp).background(Gradients.soft(), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
        }
        Text(
            label,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

/** Thẻ công cụ lớn: icon, tên, mô tả một dòng, mũi tên. */
@Composable
fun ToolCard(@DrawableRes icon: Int, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 96.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(64.dp).background(Gradients.soft(), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.primary)
        }
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

/** Một hàng tài liệu: ảnh trang đầu, tên, ngày và số trang. */
@Composable
fun DocRow(doc: Doc, firstPage: File, onClick: () -> Unit) {
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(doc.createdAt))
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageThumbnail(firstPage, Modifier.size(width = 48.dp, height = 64.dp).clip(MaterialTheme.shapes.extraSmall))
        Column(Modifier.weight(1f)) {
            Text(doc.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(
                "$date · " + pluralStringResource(R.plurals.pages, doc.pageCount, doc.pageCount),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.outline)
    }
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
