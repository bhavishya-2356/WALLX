package com.example.data

import android.app.WallpaperManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

data class WallpaperCropSpec(
    val scale: Float = 1f,
    val normalizedOffsetX: Float = 0f,
    val normalizedOffsetY: Float = 0f
)

class WallpaperRepository(
    private val context: Context,
    private val dao: WallpaperDao
) {
    val allWallpapers: Flow<List<WallpaperEntity>> = dao.observeAll()
    val favoriteWallpapers: Flow<List<WallpaperEntity>> = dao.observeFavorites()

    fun observeWallpaper(id: Long): Flow<WallpaperEntity?> = dao.observeById(id)

    private fun wallpapersDir(): File {
        val dir = File(context.filesDir, "wallpapers")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    companion object {
        const val COLLECTION_IPHONE_16_PRO = "iPhone 16 Pro"
        const val COLLECTION_CAR = "CAR WALLPAPER"
        const val COLLECTION_WALLPAPER = "WALLPAPER"
        const val COLLECTION_NATURE = "NATURE WALLPAPER"
        const val COLLECTION_DEPTH_EFFECT = "DEPTH EFFECT"
        const val COLLECTION_SAMSUNG = "SAMSUNG WALLPAPER"
    }

    private data class BuiltInWallpaper(
        val resId: Int,
        val title: String,
        val fileName: String,
        val collectionName: String
    )

    suspend fun seedBuiltInCollections() = withContext(Dispatchers.IO) {
        val dir = wallpapersDir()

        // Remove ALL previous built-in wallpapers from database and internal storage before seeding fresh collections
        dao.deleteAll()
        dir.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }

        val builtIns = listOf(
            // Collection 1: iPhone 16 Pro (15 wallpapers — inside title matches folder name)
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_01,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_01.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_02,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_02.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_03,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_03.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_04,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_04.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_05,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_05.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_06,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_06.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_07,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_07.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_08,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_08.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_09,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_09.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_10,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_10.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_11,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_11.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_12,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_12.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_13,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_13.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_14,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_14.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_ti_15,
                title = COLLECTION_IPHONE_16_PRO,
                fileName = "wallx_pack_ti_15.jpg",
                collectionName = COLLECTION_IPHONE_16_PRO
            ),
            // Collection 2: CAR WALLPAPER (18 wallpapers — inside title matches folder name)
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_porsche_01,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_porsche_01.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_porsche_02,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_porsche_02.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_01,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_01.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_02,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_02.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_03,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_03.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_04,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_04.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_05,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_05.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_06,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_06.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_07,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_07.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_08,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_08.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_09,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_09.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_10,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_10.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_11,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_11.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_12,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_12.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_13,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_13.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_14,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_14.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_15,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_15.jpg",
                collectionName = COLLECTION_CAR
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_car_16,
                title = COLLECTION_CAR,
                fileName = "wallx_pack_car_16.jpg",
                collectionName = COLLECTION_CAR
            ),
            // Collection 3: WALLPAPER (9 wallpapers — inside title matches folder name)
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_01,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_01.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_02,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_02.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_03,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_03.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_04,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_04.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_05,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_05.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_06,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_06.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_07,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_07.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_08,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_08.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_wall_09,
                title = COLLECTION_WALLPAPER,
                fileName = "wallx_pack_wall_09.jpg",
                collectionName = COLLECTION_WALLPAPER
            ),
            // Collection 4: NATURE WALLPAPER (15 unique uploaded wallpapers — inside title matches folder name)
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_01,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_01.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_02,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_02.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_03,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_03.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_04,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_04.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_05,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_05.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_06,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_06.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_07,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_07.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_08,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_08.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_09,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_09.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_10,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_10.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_11,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_11.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_12,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_12.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_13,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_13.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_14,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_14.jpg",
                collectionName = COLLECTION_NATURE
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_nature_15,
                title = COLLECTION_NATURE,
                fileName = "wallx_pack_nature_15.jpg",
                collectionName = COLLECTION_NATURE
            ),
            // Collection 5: DEPTH EFFECT (15 uploaded depth-effect wallpapers — inside title matches folder name)
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_01,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_01.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_02,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_02.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_03,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_03.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_04,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_04.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_05,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_05.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_06,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_06.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_07,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_07.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_08,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_08.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_09,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_09.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_10,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_10.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_11,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_11.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_12,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_12.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_13,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_13.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_14,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_14.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_depth_15,
                title = COLLECTION_DEPTH_EFFECT,
                fileName = "wallx_pack_depth_15.jpg",
                collectionName = COLLECTION_DEPTH_EFFECT
            ),
            // Collection 6: SAMSUNG WALLPAPER (19 uploaded wallpapers — inside title matches folder name)
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_01,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_01.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_02,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_02.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_03,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_03.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_04,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_04.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_05,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_05.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_06,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_06.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_07,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_07.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_08,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_08.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_09,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_09.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_10,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_10.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_11,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_11.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_12,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_12.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_13,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_13.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_14,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_14.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_15,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_15.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_16,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_16.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_17,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_17.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_18,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_18.jpg",
                collectionName = COLLECTION_SAMSUNG
            ),
            BuiltInWallpaper(
                resId = com.example.R.drawable.img_pack_samsung_19,
                title = COLLECTION_SAMSUNG,
                fileName = "wallx_pack_samsung_19.jpg",
                collectionName = COLLECTION_SAMSUNG
            )
        )

        val baseTime = System.currentTimeMillis()
        val toInsert = mutableListOf<WallpaperEntity>()
        builtIns.forEachIndexed { index, item ->
            runCatching {
                val targetFile = File(dir, item.fileName)
                context.resources.openRawResource(item.resId).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.exists() && targetFile.length() > 0L) {
                    toInsert.add(
                        WallpaperEntity(
                            title = item.title,
                            filePath = targetFile.absolutePath,
                            width = 8640,
                            height = 15360,
                            fileSizeBytes = targetFile.length(),
                            isFavorite = false,
                            tag = item.collectionName,
                            addedAt = baseTime + (builtIns.size - index)
                        )
                    )
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            dao.insertAll(toInsert)
        }
    }

    suspend fun importFromUris(
        uris: List<Uri>,
        targetCollection: String = COLLECTION_WALLPAPER
    ): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val dir = wallpapersDir()
        val resolvedCollection = targetCollection.trim().ifEmpty { COLLECTION_WALLPAPER }

        uris.forEachIndexed { index, uri ->
            runCatching {
                val rawDisplayName = queryDisplayName(uri)
                val cleanTitle = resolvedCollection
                val rawExtension = rawDisplayName
                    ?.substringAfterLast('.', "")
                    ?.lowercase()
                    .orEmpty()

                val targetFile = if (rawExtension in setOf("jpg", "jpeg", "png", "webp")) {
                    File(dir, "wallx_${UUID.randomUUID()}.$rawExtension").also { file ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        } ?: return@runCatching
                    }
                } else {
                    // Safely transcode HEIC/HEIF or unknown image formats to crisp JPEG
                    transcodeUriToJpeg(uri, dir) ?: return@runCatching
                }

                if (!targetFile.exists() || targetFile.length() == 0L) {
                    targetFile.delete()
                    return@runCatching
                }

                var width = 0
                var height = 0
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(targetFile.absolutePath, bounds)
                width = bounds.outWidth
                height = bounds.outHeight

                // Fallback for formats BitmapFactory bounds didn't parse directly
                if (width <= 0 || height <= 0) {
                    val decoded = decodeFileWithOrientation(targetFile, maxDim = 2400)
                    if (decoded != null) {
                        width = decoded.width
                        height = decoded.height
                        decoded.recycle()
                    } else {
                        targetFile.delete()
                        return@runCatching
                    }
                }

                dao.insert(
                    WallpaperEntity(
                        title = cleanTitle,
                        filePath = targetFile.absolutePath,
                        width = width,
                        height = height,
                        fileSizeBytes = targetFile.length(),
                        isFavorite = false,
                        tag = resolvedCollection,
                        addedAt = System.currentTimeMillis() + index
                    )
                )
                importedCount++
            }
        }
        importedCount
    }

    private fun transcodeUriToJpeg(uri: Uri, dir: File): File? {
        val targetFile = File(dir, "wallx_${UUID.randomUUID()}.jpg")
        val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = 3200
                    val w = info.size.width
                    val h = info.size.height
                    if (w > maxDim || h > maxDim) {
                        val scale = maxDim.toFloat() / max(w, h).toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }

        if (bitmap == null) return null
        return try {
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
            targetFile
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    suspend fun toggleFavorite(wallpaper: WallpaperEntity) {
        dao.setFavorite(wallpaper.id, !wallpaper.isFavorite)
    }

    suspend fun updateDetails(id: Long, title: String, tag: String) {
        val safeTitle = title.trim().ifEmpty { "My Wallpaper" }
        val safeTag = tag.trim()
        dao.updateDetails(id = id, title = safeTitle, tag = safeTag)
    }

    suspend fun deleteWallpaper(wallpaper: WallpaperEntity) = withContext(Dispatchers.IO) {
        dao.deleteById(wallpaper.id)
        runCatching {
            val file = File(wallpaper.filePath)
            if (file.exists()) file.delete()
        }
    }

    suspend fun clearAllWallpapers() = withContext(Dispatchers.IO) {
        val all = dao.getAllSnapshot()
        dao.deleteAll()
        all.forEach { item ->
            runCatching {
                val file = File(item.filePath)
                if (file.exists()) file.delete()
            }
        }
        wallpapersDir().listFiles()?.forEach { runCatching { it.delete() } }
    }

    fun createSystemWallpaperIntent(wallpaper: WallpaperEntity): Intent? {
        val file = File(wallpaper.filePath)
        if (!file.exists()) return null
        val contentUri = runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull() ?: return null

        val manager = WallpaperManager.getInstance(context)
        val systemCropIntent = runCatching {
            manager.getCropAndSetWallpaperIntent(contentUri).apply {
                clipData = ClipData.newRawUri("wallpaper", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()

        if (systemCropIntent != null && systemCropIntent.resolveActivity(context.packageManager) != null) {
            grantReadPermissionToHandlers(systemCropIntent, contentUri)
            return systemCropIntent
        }

        val mimeType = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        val attachIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(contentUri, mimeType)
            putExtra("mimeType", mimeType)
            clipData = ClipData.newRawUri("wallpaper", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return if (attachIntent.resolveActivity(context.packageManager) != null) {
            grantReadPermissionToHandlers(attachIntent, contentUri)
            Intent.createChooser(attachIntent, "Set wallpaper").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            null
        }
    }

    private fun grantReadPermissionToHandlers(intent: Intent, uri: Uri) {
        runCatching {
            val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            matches.forEach { info ->
                context.grantUriPermission(
                    info.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
    }

    private fun resolvePhysicalScreenSize(): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) {
                    return bounds.width() to bounds.height()
                }
            } else {
                @Suppress("DEPRECATION")
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay?.getRealMetrics(metrics)
                if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                    return metrics.widthPixels to metrics.heightPixels
                }
            }
        }
        val fallback = context.resources.displayMetrics
        return fallback.widthPixels.coerceAtLeast(1080) to fallback.heightPixels.coerceAtLeast(1920)
    }

    suspend fun applyWallpaper(
        wallpaper: WallpaperEntity,
        target: ApplyTarget,
        cropSpec: WallpaperCropSpec = WallpaperCropSpec()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(wallpaper.filePath)
            require(file.exists()) { "Wallpaper file not found" }

            val manager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.isSetWallpaperAllowed) {
                error("Wallpaper changes are restricted on this device")
            }

            val (screenW, screenH) = resolvePhysicalScreenSize()
            val targetMaxDim = max(screenW, screenH).coerceIn(1920, 3200)

            val decoded = decodeFileWithOrientation(file, maxDim = targetMaxDim)
                ?: error("Could not decode wallpaper bitmap")

            val fittedBitmap = cropToScreenWithFraming(
                source = decoded,
                screenWidth = screenW,
                screenHeight = screenH,
                cropSpec = cropSpec
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    when (target) {
                        ApplyTarget.HOME_SCREEN -> {
                            manager.setBitmap(fittedBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        }
                        ApplyTarget.LOCK_SCREEN -> {
                            manager.setBitmap(fittedBitmap, null, true, WallpaperManager.FLAG_LOCK)
                        }
                        ApplyTarget.BOTH_SCREENS,
                        ApplyTarget.ASK_ALWAYS -> {
                            // Apply explicitly to Home Screen and Lock Screen so neither screen is skipped on OEM devices
                            manager.setBitmap(fittedBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                            runCatching {
                                manager.setBitmap(fittedBitmap, null, true, WallpaperManager.FLAG_LOCK)
                            }
                        }
                    }
                } else {
                    manager.setBitmap(fittedBitmap)
                }
                Unit
            } finally {
                if (fittedBitmap !== decoded && !fittedBitmap.isRecycled) {
                    fittedBitmap.recycle()
                }
                if (!decoded.isRecycled) {
                    decoded.recycle()
                }
            }
        }
    }

    /**
     * Decodes a wallpaper file while respecting EXIF orientation (API 28+ ImageDecoder handles
     * EXIF rotation & HEIF automatically) and preventing OutOfMemoryError on 48MP+ photos.
     */
    private fun decodeFileWithOrientation(file: File, maxDim: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoded = runCatching {
                val source = ImageDecoder.createSource(file)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    if (w > maxDim || h > maxDim) {
                        val scale = maxDim.toFloat() / max(w, h).toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            }.getOrNull()
            if (decoded != null) return decoded
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > maxDim ||
            bounds.outHeight / sampleSize > maxDim
        ) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }

    /**
     * Crops the wallpaper to match the device display aspect using a single target bitmap + Matrix
     * (half the memory and 2x faster than double-allocating createScaledBitmap + createBitmap).
     */
    private fun cropToScreenWithFraming(
        source: Bitmap,
        screenWidth: Int,
        screenHeight: Int,
        cropSpec: WallpaperCropSpec
    ): Bitmap {
        if (screenWidth <= 0 || screenHeight <= 0 || source.width <= 0 || source.height <= 0) {
            return source
        }
        val targetWidth = screenWidth.coerceAtMost(2160)
        val targetHeight = screenHeight.coerceAtMost(3840)
        val userZoom = cropSpec.scale.coerceIn(1f, 3f)

        val baseScale = max(
            targetWidth.toFloat() / source.width.toFloat(),
            targetHeight.toFloat() / source.height.toFloat()
        )
        val totalScale = baseScale * userZoom

        val scaledWidth = source.width * totalScale
        val scaledHeight = source.height * totalScale

        val maxShiftX = ((scaledWidth - targetWidth) / 2f).coerceAtLeast(0f)
        val maxShiftY = ((scaledHeight - targetHeight) / 2f).coerceAtLeast(0f)

        val shiftX = (cropSpec.normalizedOffsetX * targetWidth).coerceIn(-maxShiftX, maxShiftX)
        val shiftY = (cropSpec.normalizedOffsetY * targetHeight).coerceIn(-maxShiftY, maxShiftY)

        val dx = (targetWidth - scaledWidth) / 2f + shiftX
        val dy = (targetHeight - scaledHeight) / 2f + shiftY

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = Matrix().apply {
            setScale(totalScale, totalScale)
            postTranslate(dx, dy)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun formatFriendlyTitle(rawName: String?, fallbackIndex: Int): String {
        val base = rawName
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        return if (base.isNotEmpty() && !base.all { it.isDigit() }) {
            base.take(36)
        } else {
            "My Wallpaper ${fallbackIndex + 1}"
        }
    }
}
