package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    @Query("SELECT * FROM wallpapers ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<WallpaperEntity>>

    @Query("SELECT * FROM wallpapers WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<WallpaperEntity?>

    @Query("SELECT * FROM wallpapers")
    suspend fun getAllSnapshot(): List<WallpaperEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallpaper: WallpaperEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallpapers: List<WallpaperEntity>)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET title = :title, tag = :tag WHERE id = :id")
    suspend fun updateDetails(id: Long, title: String, tag: String)

    @Query("DELETE FROM wallpapers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM wallpapers WHERE filePath IN (:filePaths)")
    suspend fun deleteByFilePaths(filePaths: List<String>)

    @Query("DELETE FROM wallpapers")
    suspend fun deleteAll()
}
