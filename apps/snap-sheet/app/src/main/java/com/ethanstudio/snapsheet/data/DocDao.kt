package com.ethanstudio.snapsheet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocDao {
    @Query("SELECT * FROM docs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Doc>>

    @Query("SELECT * FROM docs WHERE id = :id")
    fun observe(id: Long): Flow<Doc?>

    @Query("SELECT * FROM docs WHERE id = :id")
    suspend fun get(id: Long): Doc?

    @Query("SELECT * FROM docs")
    suspend fun getAll(): List<Doc>

    @Query("UPDATE docs SET pageCount = :count WHERE id = :id")
    suspend fun setPageCount(id: Long, count: Int)

    @Query("UPDATE docs SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun setFolder(ids: List<Long>, folderId: Long?)

    @Insert
    suspend fun insert(doc: Doc): Long

    @Update
    suspend fun update(doc: Doc)

    @Query("DELETE FROM docs WHERE id = :id")
    suspend fun delete(id: Long)
}
