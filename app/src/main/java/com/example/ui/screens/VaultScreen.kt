package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.WallpaperEntity
import com.example.ui.VaultUiState
import com.example.ui.WallpaperCollectionGroup
import com.example.ui.WallxViewModel
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassStyle
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.WallpaperGridCard
import com.example.ui.components.wallxBackdrop
import com.example.ui.theme.ChromeSilver
import com.example.ui.theme.JetBlack
import com.example.ui.theme.PureWhite
import java.io.File

@Composable
fun VaultScreen(
    state: VaultUiState,
    isOwner: Boolean,
    onAddWallpapersClick: () -> Unit,
    onWallpaperClick: (Long) -> Unit,
    onToggleFavorite: (WallpaperEntity) -> Unit,
    onSelectTag: (String?) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    collectionsGridState: LazyGridState = rememberLazyGridState(),
    gridState: LazyGridState = rememberLazyGridState()
) {
    var openedCollectionKey by rememberSaveable { mutableStateOf<String?>(null) }

    val collections = state.collections

    val activeCollection = remember(collections, openedCollectionKey) {
        openedCollectionKey?.let { key -> collections.firstOrNull { it.key == key } }
    }

    // Keep ViewModel tag filter synced with opened collection so Preview swipes & Add target stay inside this collection
    LaunchedEffect(activeCollection?.key) {
        val tagFilter = activeCollection?.key?.takeIf { it != WallxViewModel.CUSTOM_COLLECTION_KEY }
        onSelectTag(tagFilter)
    }

    BackHandler(enabled = activeCollection != null) {
        openedCollectionKey = null
        onSelectTag(null)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Isolated static GPU layer for backdrop so scrolling grids never re-rasterize radial gradients
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .wallxBackdrop()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Clean Header: App Profile Logo + WALLX wordmark only (NO search box, NO wallpaper count)
            VaultHeader(onProfileClick = onProfileClick)

            AnimatedVisibility(
                visible = state.isImporting,
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.importing_wallpapers),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }

            if (state.isLoading && state.allWallpapers.isEmpty()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else if (state.allWallpapers.isEmpty()) {
                VaultEmptyState(
                    isOwner = isOwner,
                    onAddWallpapersClick = onAddWallpapersClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else if (activeCollection == null) {
                // Compact small-size Collection Tiles (iPhone 16 Pro, PORSCHE, WALLPAPER)
                LazyVerticalGrid(
                    state = collectionsGridState,
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = if (isOwner) 176.dp else 128.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("collections_grid")
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "collections_section_header") {
                        Text(
                            text = stringResource(R.string.vault_collections_section),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    items(
                        items = collections,
                        key = { it.key },
                        contentType = { "collection_tile" }
                    ) { collection ->
                        val openClick = remember(collection.key) {
                            { openedCollectionKey = collection.key }
                        }
                        SmallCollectionTileCard(
                            collection = collection,
                            onOpenCollection = openClick,
                            modifier = Modifier.testTag("collection_tile_${collection.key.lowercase().replace(' ', '_')}")
                        )
                    }
                }
            } else {
                // Opened Collection Header + smooth 2-column grid of wallpapers in that collection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassIconButton(
                        onClick = {
                            openedCollectionKey = null
                            onSelectTag(null)
                        },
                        size = 44.dp,
                        style = LiquidGlassStyle.FROST_WHITE,
                        modifier = Modifier.testTag("pack_bag_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeCollection.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeCollection.items.size == 1) {
                                stringResource(R.string.pack_bag_subtitle_singular)
                            } else {
                                stringResource(R.string.pack_bag_subtitle_plural, activeCollection.items.size)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (isOwner) 176.dp else 128.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("vault_grid")
                ) {
                    items(
                        items = activeCollection.items,
                        key = { it.id },
                        contentType = { "wallpaper_card" }
                    ) { wallpaper ->
                        val cardClick = remember(wallpaper.id, onWallpaperClick) {
                            { onWallpaperClick(wallpaper.id) }
                        }
                        val favoriteClick = remember(wallpaper.id, wallpaper.isFavorite, onToggleFavorite) {
                            { onToggleFavorite(wallpaper) }
                        }
                        WallpaperGridCard(
                            wallpaper = wallpaper,
                            showResolutionBadge = true,
                            onClick = cardClick,
                            onToggleFavorite = favoriteClick
                        )
                    }
                }
            }
        }

        // Floating Add Wallpaper Button — ONLY visible for the App Owner
        if (isOwner && state.allWallpapers.isNotEmpty()) {
            LiquidGlassButton(
                onClick = onAddWallpapersClick,
                style = LiquidGlassStyle.MINT_PRIMARY,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 98.dp)
                    .height(52.dp)
                    .testTag("add_wallpaper_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_wallpaper),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_add_short),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Compact small-size Collection Tile Card supporting 1, 2, or 3+ wallpaper previews
 * and adapting seamlessly to Light Mode (White/Silver/Black) & Dark Mode (Obsidian/Silver/White).
 */
@Composable
private fun SmallCollectionTileCard(
    collection: WallpaperCollectionGroup,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = collection.title
    val wallpapers = collection.items
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(22.dp)

    LiquidGlassSurface(
        style = LiquidGlassStyle.FROST_WHITE,
        shape = cardShape,
        cornerRadius = 22.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCollection)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.06f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                when {
                    wallpapers.size == 1 -> {
                        val item = wallpapers[0]
                        SmallThumbImage(
                            filePath = item.filePath,
                            contentDescription = title,
                            context = context,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    wallpapers.size == 2 -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            SmallThumbImage(
                                filePath = wallpapers[0].filePath,
                                contentDescription = title,
                                context = context,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            SmallThumbImage(
                                filePath = wallpapers[1].filePath,
                                contentDescription = null,
                                context = context,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    wallpapers.size >= 3 -> {
                        val coverPrimary = wallpapers[0]
                        val coverSecondary = wallpapers[1]
                        val coverTertiary = wallpapers.getOrNull(5) ?: wallpapers[2]

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            SmallThumbImage(
                                filePath = coverPrimary.filePath,
                                contentDescription = title,
                                context = context,
                                modifier = Modifier
                                    .weight(1.25f)
                                    .fillMaxHeight()
                            )

                            Column(
                                modifier = Modifier
                                    .weight(0.85f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                SmallThumbImage(
                                    filePath = coverSecondary.filePath,
                                    contentDescription = null,
                                    context = context,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                                SmallThumbImage(
                                    filePath = coverTertiary.filePath,
                                    contentDescription = null,
                                    context = context,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Compact monochrome 8K corner badge
                Text(
                    text = stringResource(R.string.intro_quality_tag),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = PureWhite,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(JetBlack.copy(alpha = 0.85f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }

            // Compact title & count footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (wallpapers.size == 1) {
                            stringResource(R.string.pack_bag_short_count_singular)
                        } else {
                            stringResource(R.string.pack_bag_short_count_plural, wallpapers.size)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = wallpapers.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallThumbImage(
    filePath: String,
    contentDescription: String?,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val request = remember(filePath) {
        ImageRequest.Builder(context)
            .data(File(filePath))
            .memoryCacheKey("${filePath}_tile_120")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .size(120, 180)
            .precision(Precision.INEXACT)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun VaultHeader(
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val logoRequest = remember(context) {
        ImageRequest.Builder(context)
            .data(R.drawable.ic_wallx_icon)
            .memoryCacheKey("wallx_header_logo_128")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .size(128, 128)
            .precision(Precision.INEXACT)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Uploaded W App Profile Logo Badge decoded off main thread
        Surface(
            shape = CircleShape,
            color = JetBlack,
            border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.outline),
            onClick = onProfileClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("home_app_profile_logo")
        ) {
            AsyncImage(
                model = logoRequest,
                contentDescription = stringResource(R.string.cd_app_profile_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column {
            Text(
                text = buildAnnotatedString {
                    append("WALL")
                    withStyle(SpanStyle(color = ChromeSilver)) {
                        append("X")
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.vault_logo_subtitle),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("vault_logo_subtitle")
            )
        }
    }
}

@Composable
private fun VaultEmptyState(
    isOwner: Boolean,
    onAddWallpapersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 112.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vault_empty),
                    contentDescription = stringResource(R.string.cd_empty_illustration),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.vault_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    if (isOwner) R.string.vault_empty_message else R.string.vault_empty_message_viewer
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (isOwner) {
                Spacer(modifier = Modifier.height(28.dp))

                LiquidGlassButton(
                    onClick = onAddWallpapersClick,
                    style = LiquidGlassStyle.MINT_PRIMARY,
                    contentPadding = PaddingValues(horizontal = 30.dp, vertical = 16.dp),
                    modifier = Modifier
                        .height(58.dp)
                        .testTag("empty_add_wallpapers_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.action_add_wallpapers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
