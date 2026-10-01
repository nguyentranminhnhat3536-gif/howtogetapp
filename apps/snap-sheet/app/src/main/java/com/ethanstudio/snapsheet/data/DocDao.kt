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

    @Insert
    suspend fun insert(doc: Doc): Long

    @Update
    suspend fun update(doc: Doc)

    @Query("DELETE FROM docs WHERE id = :id")
    suspend fun delete(id: Long)
}
