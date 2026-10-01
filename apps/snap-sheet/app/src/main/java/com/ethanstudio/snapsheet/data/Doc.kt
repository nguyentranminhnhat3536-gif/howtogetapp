package com.ethanstudio.snapsheet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Một tài liệu đã quét. File ảnh và PDF nằm trong thư mục riêng của app (xem DocRepository). */
@Entity(tableName = "docs")
data class Doc(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val pageCount: Int,
)

/** Lọc danh sách theo từ khóa trong tên, không phân biệt hoa thường. Từ khóa trống: giữ nguyên. */
fun filterDocs(docs: List<Doc>, query: String): List<Doc> {
    val q = query.trim()
    return if (q.isEmpty()) docs else docs.filter { it.name.contains(q, ignoreCase = true) }
}
