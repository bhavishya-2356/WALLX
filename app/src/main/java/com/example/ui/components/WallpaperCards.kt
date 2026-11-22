package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.WallpaperEntity
import com.example.ui.theme.BrushedSilverLight
import com.example.ui.theme.JetBlack
import com.example.ui.theme.PureWhite
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val CardCornerShape = RoundedCornerShape(20.dp)
private val CardBottomBarBg = Color(0xB80B0C0E)
private val CardPillBg = Color(0xB80B0C0E)

@Composable
fun WallpaperGridCard(
    wallpaper: WallpaperEntity,
    showResolutionBadge: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(wallpaper.filePath) {
        ImageRequest.Builder(context)
            .data(File(wallpaper.filePath))
            .memoryCacheKey("${wallpaper.filePath}_thumb_200")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .size(200, 340)
            .precision(Precision.INEXACT)
            .allowHardware(true)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.58f)
            .clip(CardCornerShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, CardCornerShape)
            .clickable(onClick = onClick)
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.cd_wallpaper_image, wallpaper.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top-right 48dp accessible favorite button (flat single-pass for zero scroll jank)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleFavorite
                )
                .testTag("favorite_toggle_${wallpaper.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (wallpaper.isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = stringResource(
                    if (wallpaper.isFavorite) R.string.cd_favorite_remove
                    else R.string.cd_favorite_add
                ),
                tint = if (wallpaper.isFavorite) JetBlack else PureWhite,
                modifier = Modifier
                    .size(34.dp)
                    .background(if (wallpaper.isFavorite) PureWhite else CardPillBg, CircleShape)
                    .padding(8.dp)
            )
        }

        // Flat solid-color bottom label strip (zero gradient shader cost during fling)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(CardBottomBarBg)
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(
                text = wallpaper.title,
                style = MaterialTheme.typography.titleSmall,
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showResolutionBadge) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = wallpaper.resolutionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrushedSilverLight,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWallpaperBottomSheet(
    wallpaper: WallpaperEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, tag: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleText by remember(wallpaper.id) { mutableStateOf(wallpaper.title) }
    var tagText by remember(wallpaper.id) { mutableStateOf(wallpaper.tag) }
    val addedDateLabel = remember(wallpaper.addedAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(Date(wallpaper.addedAt))
    }

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            LiquidGlassSurface(
                style = LiquidGlassStyle.FROST_WHITE,
                shape = RoundedCornerShape(18.dp),
                cornerRadius = 18.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpecColumn(
                        label = stringResource(R.string.detail_resolution),
                        value = wallpaper.resolutionLabel
                    )
                    SpecColumn(
                        label = stringResource(R.string.detail_file_size),
                        value = formatBytes(wallpaper.fileSizeBytes)
                    )
                    SpecColumn(
                        label = stringResource(R.string.detail_added_date),
                        value = addedDateLabel
                    )
                }
            }

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it.take(40) },
                label = { Text(stringResource(R.string.field_title_label)) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallpaper_title_input")
            )

            OutlinedTextField(
                value = tagText,
                onValueChange = { tagText = it.take(24) },
                label = { Text(stringResource(R.string.field_tag_label)) },
                placeholder = { Text(stringResource(R.string.field_tag_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallpaper_tag_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassButton(
                    onClick = onDismiss,
                    style = LiquidGlassStyle.FROST_WHITE,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                LiquidGlassButton(
                    onClick = { onSave(titleText, tagText) },
                    style = LiquidGlassStyle.MINT_PRIMARY,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("save_wallpaper_details_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.getDefault(), "%.0f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}
