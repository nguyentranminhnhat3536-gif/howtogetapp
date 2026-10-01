package com.ethanstudio.snapsheet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Doc::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun docDao(): DocDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "snapsheet.db").build()
    }
}
