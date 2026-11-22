package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.ApplyTarget
import com.example.data.WallpaperEntity
import com.example.ui.components.EditWallpaperBottomSheet
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassStyle
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.BrushedSilverLight
import kotlinx.coroutines.delay
import java.io.File

private val PreviewTopScrim = Brush.verticalGradient(
    colors = listOf(Color(0x96000000), Color.Transparent)
)

private val PagerSnapSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 700f
)

private fun buildPreviewImageRequest(context: Context, filePath: String): ImageRequest {
    return ImageRequest.Builder(context)
        .data(File(filePath))
        .memoryCacheKey("${filePath}_preview_8k")
        .placeholderMemoryCacheKey("${filePath}_thumb_200")
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .size(1080, 2280)
        .precision(Precision.INEXACT)
        .allowHardware(true)
        .allowRgb565(false)
        .crossfade(false)
        .build()
}

@Composable
fun PreviewScreen(
    wallpapers: List<WallpaperEntity>,
    initialWallpaperId: Long,
    isOwner: Boolean,
    isApplying: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (WallpaperEntity) -> Unit,
    onUpdateDetails: (id: Long, title: String, tag: String) -> Unit,
    onDeleteWallpaper: (WallpaperEntity) -> Unit,
    onApplyDirect: (WallpaperEntity, ApplyTarget) -> Unit,
    onSetLockScreenReal: (WallpaperEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (wallpapers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val initialIndex = remember(initialWallpaperId, wallpapers.isEmpty()) {
        wallpapers.indexOfFirst { it.id == initialWallpaperId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (wallpapers.size - 1).coerceAtLeast(0)),
        pageCount = { wallpapers.size }
    )

    var setWallpaperTarget by remember { mutableStateOf<WallpaperEntity?>(null) }
    var editingWallpaper by remember { mutableStateOf<WallpaperEntity?>(null) }
    var deletingWallpaper by remember { mutableStateOf<WallpaperEntity?>(null) }

    // Gently pre-warm only the immediate next & previous pages after settle so swiping never contends with CPU/GPU
    val imageLoader = context.imageLoader
    LaunchedEffect(pagerState.settledPage, wallpapers.size) {
        delay(120)
        val current = pagerState.settledPage
        val nextIdx = current + 1
        if (nextIdx in wallpapers.indices) {
            imageLoader.enqueue(buildPreviewImageRequest(context, wallpapers[nextIdx].filePath))
        }
        val prevIdx = current - 1
        if (prevIdx in wallpapers.indices) {
            imageLoader.enqueue(buildPreviewImageRequest(context, wallpapers[prevIdx].filePath))
        }
    }

    val smoothFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapPositionalThreshold = 0.22f,
        snapAnimationSpec = PagerSnapSpec
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Isolated VerticalPager with hardware-layered pages: ZERO mid-swipe recompositions or disk stalls
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            flingBehavior = smoothFlingBehavior,
            key = { page -> wallpapers.getOrNull(page)?.id ?: page.toLong() },
            modifier = Modifier
                .fillMaxSize()
                .testTag("preview_vertical_pager")
        ) { page ->
            val item = wallpapers.getOrNull(page) ?: return@VerticalPager
            val pageRequest = remember(item.filePath) {
                buildPreviewImageRequest(context, item.filePath)
            }

            AsyncImage(
                model = pageRequest,
                contentDescription = stringResource(R.string.cd_wallpaper_image, item.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()
            )
        }

        // Top subtle legibility scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .align(Alignment.TopCenter)
                .background(PreviewTopScrim)
        )

        // Isolated Top Bar reading settledPage -> zero recomposition during drag/fling
        PreviewTopBar(
            wallpapers = wallpapers,
            pagerState = pagerState,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Isolated Bottom Action Dock with clear "Set Wallpaper" button
        PreviewBottomDock(
            wallpapers = wallpapers,
            pagerState = pagerState,
            isOwner = isOwner,
            isApplying = isApplying,
            onToggleFavorite = onToggleFavorite,
            onEditClick = { editingWallpaper = it },
            onDeleteClick = { deletingWallpaper = it },
            onSetWallpaperClick = { setWallpaperTarget = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Set Wallpaper Options Sheet (Home Screen, Lock Screen -> Real Phone Screen, Home & Lock Screen)
    val targetApply = setWallpaperTarget
    if (targetApply != null) {
        SetWallpaperOptionsSheet(
            wallpaper = targetApply,
            onDismiss = { setWallpaperTarget = null },
            onChooseHomeScreen = {
                setWallpaperTarget = null
                onApplyDirect(targetApply, ApplyTarget.HOME_SCREEN)
            },
            onChooseLockScreenReal = {
                setWallpaperTarget = null
                onSetLockScreenReal(targetApply)
            },
            onChooseBothScreens = {
                setWallpaperTarget = null
                onApplyDirect(targetApply, ApplyTarget.BOTH_SCREENS)
            }
        )
    }

    val targetEdit = editingWallpaper
    if (isOwner && targetEdit != null) {
        EditWallpaperBottomSheet(
            wallpaper = targetEdit,
            onDismiss = { editingWallpaper = null },
            onSave = { newTitle, newTag ->
                editingWallpaper = null
                onUpdateDetails(targetEdit.id, newTitle, newTag)
            }
        )
    }

    val targetDelete = deletingWallpaper
    if (isOwner && targetDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingWallpaper = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(
                    text = stringResource(R.string.delete_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                LiquidGlassButton(
                    onClick = {
                        deletingWallpaper = null
                        onDeleteWallpaper(targetDelete)
                    },
                    style = LiquidGlassStyle.CORAL_DANGER,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("confirm_delete_wallpaper_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                LiquidGlassButton(
                    onClick = { deletingWallpaper = null },
                    style = LiquidGlassStyle.FROST_WHITE,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetWallpaperOptionsSheet(
    wallpaper: WallpaperEntity,
    onDismiss: () -> Unit,
    onChooseHomeScreen: () -> Unit,
    onChooseLockScreenReal: () -> Unit,
    onChooseBothScreens: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.set_wallpaper_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${wallpaper.title} · ${wallpaper.resolutionLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            SetWallpaperOptionRow(
                icon = Icons.Filled.Home,
                title = stringResource(R.string.apply_option_home_title),
                subtitle = stringResource(R.string.apply_option_home_subtitle),
                highlighted = false,
                onClick = onChooseHomeScreen,
                modifier = Modifier.testTag("apply_home_screen_option")
            )

            SetWallpaperOptionRow(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.apply_option_lock_title),
                subtitle = stringResource(R.string.apply_option_lock_subtitle),
                highlighted = true,
                onClick = onChooseLockScreenReal,
                modifier = Modifier.testTag("apply_lock_screen_option")
            )

            SetWallpaperOptionRow(
                icon = Icons.Filled.Smartphone,
                title = stringResource(R.string.apply_option_both_title),
                subtitle = stringResource(R.string.apply_option_both_subtitle),
                highlighted = false,
                onClick = onChooseBothScreens,
                modifier = Modifier.testTag("apply_both_screens_option")
            )
        }
    }
}

@Composable
private fun SetWallpaperOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    LiquidGlassSurface(
        style = if (highlighted) LiquidGlassStyle.MINT_PRIMARY else LiquidGlassStyle.FROST_WHITE,
        shape = RoundedCornerShape(22.dp),
        cornerRadius = 22.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LiquidGlassSurface(
                style = if (highlighted) LiquidGlassStyle.DARK_OVERLAY else LiquidGlassStyle.FROST_WHITE,
                shape = CircleShape,
                cornerRadius = 22.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (highlighted) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = secondaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PreviewTopBar(
    wallpapers: List<WallpaperEntity>,
    pagerState: PagerState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe settledPage so the top bar NEVER recomposes mid-drag or mid-fling
    val safePageIndex by remember(wallpapers.size) {
        derivedStateOf { pagerState.settledPage.coerceIn(0, (wallpapers.size - 1).coerceAtLeast(0)) }
    }
    val currentWallpaper = wallpapers[safePageIndex]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LiquidGlassIconButton(
            onClick = onBack,
            size = 48.dp,
            style = LiquidGlassStyle.DARK_OVERLAY,
            contentColor = Color.White,
            modifier = Modifier.testTag("preview_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentWallpaper.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentWallpaper.resolutionLabel,
                style = MaterialTheme.typography.labelSmall,
                color = BrushedSilverLight,
                fontWeight = FontWeight.Bold
            )
        }

        if (wallpapers.size > 1) {
            LiquidGlassSurface(
                style = LiquidGlassStyle.DARK_OVERLAY,
                shape = CircleShape,
                cornerRadius = 16.dp
            ) {
                Text(
                    text = "${safePageIndex + 1}/${wallpapers.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun PreviewBottomDock(
    wallpapers: List<WallpaperEntity>,
    pagerState: PagerState,
    isOwner: Boolean,
    isApplying: Boolean,
    onToggleFavorite: (WallpaperEntity) -> Unit,
    onEditClick: (WallpaperEntity) -> Unit,
    onDeleteClick: (WallpaperEntity) -> Unit,
    onSetWallpaperClick: (WallpaperEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe settledPage for display -> zero recomposition during swipe fling
    val safePageIndex by remember(wallpapers.size) {
        derivedStateOf { pagerState.settledPage.coerceIn(0, (wallpapers.size - 1).coerceAtLeast(0)) }
    }
    val displayedWallpaper = wallpapers[safePageIndex]

    // Helper reads live currentPage only on click so rapid taps act on the exact visible wallpaper
    fun activeWallpaper(): WallpaperEntity {
        val idx = pagerState.currentPage.coerceIn(0, (wallpapers.size - 1).coerceAtLeast(0))
        return wallpapers[idx]
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            style = LiquidGlassStyle.DARK_OVERLAY,
            shape = RoundedCornerShape(34.dp),
            cornerRadius = 34.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassIconButton(
                    onClick = { onToggleFavorite(activeWallpaper()) },
                    size = 48.dp,
                    style = if (displayedWallpaper.isFavorite) {
                        LiquidGlassStyle.MINT_PRIMARY
                    } else {
                        LiquidGlassStyle.DARK_OVERLAY
                    },
                    contentColor = if (displayedWallpaper.isFavorite) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        Color.White
                    },
                    modifier = Modifier.testTag("preview_favorite_button")
                ) {
                    Icon(
                        imageVector = if (displayedWallpaper.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = stringResource(
                            if (displayedWallpaper.isFavorite) R.string.cd_favorite_remove
                            else R.string.cd_favorite_add
                        ),
                        modifier = Modifier.size(21.dp)
                    )
                }

                if (isOwner) {
                    LiquidGlassIconButton(
                        onClick = { onEditClick(activeWallpaper()) },
                        size = 48.dp,
                        style = LiquidGlassStyle.DARK_OVERLAY,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("preview_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit_details),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LiquidGlassIconButton(
                        onClick = { onDeleteClick(activeWallpaper()) },
                        size = 48.dp,
                        style = LiquidGlassStyle.DARK_OVERLAY,
                        contentColor = Color(0xFFFFB4B4),
                        modifier = Modifier.testTag("preview_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_wallpaper),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Clear "Set Wallpaper" button opening Home Screen / Real Lock Screen / Home & Lock Screen options
                LiquidGlassButton(
                    onClick = { onSetWallpaperClick(activeWallpaper()) },
                    enabled = !isApplying,
                    style = LiquidGlassStyle.MINT_PRIMARY,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("preview_set_wallpaper_button")
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.applying_wallpaper),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Wallpaper,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_set_wallpaper),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
