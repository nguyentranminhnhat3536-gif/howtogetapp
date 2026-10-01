package com.ethanstudio.snapsheet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<Folder>>

    @Insert
    abstract suspend fun insert(folder: Folder): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    @Query("UPDATE docs SET folderId = NULL WHERE folderId = :id")
    abstract suspend fun clearDocs(id: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    abstract suspend fun deleteRow(id: Long)

    /** Xóa thư mục nhưng giữ tài liệu (chỉ gỡ chúng khỏi thư mục). */
    @Transaction
    open suspend fun deleteKeepDocs(id: Long) {
        clearDocs(id)
        deleteRow(id)
    }
}
