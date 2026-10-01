package com.ethanstudio.snapsheet.data

/**
 * Các câu SQL nâng cơ sở dữ liệu từ version 1 lên 2: thêm bảng thư mục và cột folderId (cho phép null).
 * Chỉ thêm, không xóa gì, nên tài liệu cũ còn nguyên (folderId = NULL, tức nằm ở "All").
 * Để riêng ở đây (không import Room) cho test JVM kiểm tra được.
 */
val MIGRATION_1_2_SQL: List<String> = listOf(
    "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
    "ALTER TABLE `docs` ADD COLUMN `folderId` INTEGER",
)
