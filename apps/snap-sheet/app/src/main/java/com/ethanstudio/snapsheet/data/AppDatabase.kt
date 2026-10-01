package com.ethanstudio.snapsheet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Doc::class, Folder::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun docDao(): DocDao
    abstract fun folderDao(): FolderDao

    companion object {
        /** v1 → v2: thêm thư mục. Không bao giờ xóa dữ liệu cũ của người dùng. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2_SQL.forEach(db::execSQL)
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "snapsheet.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
