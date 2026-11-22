package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WallpaperEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WallxDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao

    companion object {
        @Volatile
        private var INSTANCE: WallxDatabase? = null

        fun getInstance(context: Context): WallxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WallxDatabase::class.java,
                    "wallx_vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
