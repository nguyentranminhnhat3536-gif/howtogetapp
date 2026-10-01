package com.ethanstudio.snapsheet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Thư mục một cấp trong tab Files. Mỗi tài liệu thuộc 0 hoặc 1 thư mục (xem Doc.folderId). */
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Kết quả kiểm tra tên thư mục người dùng nhập. */
sealed interface FolderNameCheck {
    data class Ok(val name: String) : FolderNameCheck
    data object Empty : FolderNameCheck
    data object Taken : FolderNameCheck
}

const val FOLDER_NAME_MAX = 40

/** Bỏ khoảng trắng hai đầu, cắt còn 40 ký tự; rỗng → Empty; trùng tên (không phân biệt hoa thường) với thư mục KHÁC [editingId] → Taken. */
fun checkFolderName(input: String, folders: List<Folder>, editingId: Long? = null): FolderNameCheck {
    val name = input.trim().take(FOLDER_NAME_MAX).trim()
    if (name.isEmpty()) return FolderNameCheck.Empty
    val taken = folders.any { it.id != editingId && it.name.equals(name, ignoreCase = true) }
    return if (taken) FolderNameCheck.Taken else FolderNameCheck.Ok(name)
}
