package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val isFavorite: Boolean = false,
    val tag: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    val resolutionLabel: String
        get() = "8K ULTRA"
}
