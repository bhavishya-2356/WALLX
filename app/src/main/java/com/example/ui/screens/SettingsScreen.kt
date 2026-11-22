package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.ThemeMode
import com.example.ui.AppUpdateUiState
import com.example.ui.UserProfileUiState
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassStyle
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.PublishUpdateDialog
import com.example.ui.components.wallxBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val SettingsCardShape = RoundedCornerShape(22.dp)

@Composable
fun SettingsScreen(
    userProfile: UserProfileUiState,
    isOwner: Boolean,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    userRating: Int,
    updateState: AppUpdateUiState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onNotificationsPermissionDenied: () -> Unit,
    onSubmitRating: (Int) -> Unit,
    onCheckForUpdateClick: () -> Unit,
    onPublishUpdate: (
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String?,
        forceUpdate: Boolean
    ) -> Unit,
    onAboutUsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    var showRateDialog by rememberSaveable { mutableStateOf(false) }
    var showPublishUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var pendingPublishedCode by remember { mutableStateOf<Int?>(null) }

    val hasAlreadyRated = userRating > 0

    LaunchedEffect(updateState.isPublishing, updateState.latestRelease?.versionCode, pendingPublishedCode) {
        val targetCode = pendingPublishedCode
        if (
            showPublishUpdateDialog &&
            !updateState.isPublishing &&
            targetCode != null &&
            updateState.latestRelease?.versionCode == targetCode
        ) {
            showPublishUpdateDialog = false
            pendingPublishedCode = null
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleNotifications(true)
        } else {
            onNotificationsPermissionDenied()
        }
    }

    val handleNotificationToggle: (Boolean) -> Unit = { targetEnabled ->
        if (!targetEnabled) {
            onToggleNotifications(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                onToggleNotifications(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onToggleNotifications(true)
        }
    }

    val shareSubject = stringResource(R.string.share_app_subject)
    val shareText = stringResource(R.string.share_app_text)
    val shareChooserTitle = stringResource(R.string.share_app_chooser_title)

    val launchShareSheet: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, shareSubject)
            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(shareIntent, shareChooserTitle)
        runCatching { context.startActivity(chooser) }
    }

    val latestRelease = updateState.latestRelease
    val updateSubtitle = when {
        updateState.isChecking -> stringResource(R.string.settings_checking_updates)
        updateState.hasUpdate && latestRelease != null -> stringResource(
            R.string.settings_check_update_available,
            latestRelease.versionName
        )
        else -> stringResource(
            R.string.settings_check_update_current,
            updateState.currentVersionName
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .wallxBackdrop()
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 122.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            item(key = "settings_header", contentType = "header") {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // PART 1: ACCOUNT
            item(key = "sec_account", contentType = "section_label") {
                SettingsSectionLabel(text = stringResource(R.string.settings_section_account))
            }

            item(key = "card_profile", contentType = "profile_card") {
                SignedInProfileCard(
                    profile = userProfile,
                    onSignOutClick = onSignOutClick,
                    modifier = Modifier.testTag("settings_user_profile_card")
                )
            }

            // PART 2: APPEARANCE (Light Mode & Dark Mode Switcher)
            item(key = "sec_appearance", contentType = "section_label") {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_section_appearance),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item(key = "card_appearance", contentType = "theme_card") {
                SeparateSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.settings_appearance_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.settings_appearance_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        SwipeableLiquidGlassThemeSwitch(
                            themeMode = themeMode,
                            onSelectThemeMode = onSelectThemeMode
                        )
                    }
                }
            }

            // PART 3: NOTIFICATIONS (separate standalone card)
            item(key = "sec_notifications", contentType = "section_label") {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_section_notifications),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item(key = "card_notifications", contentType = "toggle_card") {
                SeparateSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleNotificationToggle(!notificationsEnabled) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_notifications_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsLeadingIconBadge(
                            icon = if (notificationsEnabled) {
                                Icons.Filled.NotificationsActive
                            } else {
                                Icons.Filled.NotificationsOff
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_notifications_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(
                                    if (notificationsEnabled) {
                                        R.string.settings_notifications_on
                                    } else {
                                        R.string.settings_notifications_off
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = handleNotificationToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("settings_notifications_switch")
                        )
                    }
                }
            }

            // PART 4: APP UPDATES (each action in its own separate card)
            item(key = "sec_updates", contentType = "section_label") {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_section_updates),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item(key = "card_check_update", contentType = "action_card") {
                SeparateSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !updateState.isChecking,
                                onClick = onCheckForUpdateClick
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_check_update_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsLeadingIconBadge(icon = Icons.Filled.SystemUpdate)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_check_update_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = updateSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (updateState.hasUpdate) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (updateState.hasUpdate) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (updateState.isChecking) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            if (updateState.hasUpdate) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_update_badge_new),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.cd_check_updates),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            if (isOwner) {
                item(key = "card_publish_update", contentType = "action_card") {
                    SeparateSettingsActionCard(
                        icon = Icons.Filled.Publish,
                        title = stringResource(R.string.settings_publish_update_title),
                        subtitle = stringResource(R.string.settings_publish_update_subtitle),
                        onClick = { showPublishUpdateDialog = true },
                        modifier = Modifier.testTag("settings_publish_update_row")
                    )
                }
            }

            // PART 5: SUPPORT & SHARE (Rate App one-time only + Share App as separate standalone cards)
            item(key = "sec_community", contentType = "section_label") {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_section_community),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item(key = "card_rate_app", contentType = "action_card") {
                SeparateSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRateDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_rate_app_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsLeadingIconBadge(icon = Icons.Filled.Star)

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_rate_app_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasAlreadyRated) {
                                    stringResource(R.string.settings_rate_app_rated, userRating)
                                } else {
                                    stringResource(R.string.settings_rate_app_subtitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (hasAlreadyRated) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "$userRating/5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.settings_rate_app_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            item(key = "card_share_app", contentType = "action_card") {
                SeparateSettingsActionCard(
                    icon = Icons.Filled.Share,
                    title = stringResource(R.string.settings_share_app_title),
                    subtitle = stringResource(R.string.settings_share_app_subtitle),
                    onClick = launchShareSheet,
                    modifier = Modifier.testTag("settings_share_app_row")
                )
            }

            // PART 6: INFORMATION (About Us standalone card)
            item(key = "sec_info", contentType = "section_label") {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_section_info),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item(key = "card_about_us", contentType = "action_card") {
                SeparateSettingsActionCard(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_section_about),
                    subtitle = stringResource(R.string.about_us_subtitle),
                    onClick = onAboutUsClick,
                    modifier = Modifier.testTag("settings_about_us_row")
                )
            }
        }
    }

    // Always opens when tapped; locks editing once a one-time rating has already been submitted
    if (showRateDialog) {
        AnimatedRateAppDialog(
            existingRating = userRating,
            onDismiss = { showRateDialog = false },
            onSubmit = { chosenStars ->
                showRateDialog = false
                onSubmitRating(chosenStars)
            }
        )
    }

    if (showPublishUpdateDialog && isOwner) {
        PublishUpdateDialog(
            currentVersionCode = updateState.currentVersionCode,
            latestRelease = updateState.latestRelease,
            isPublishing = updateState.isPublishing,
            onDismiss = {
                showPublishUpdateDialog = false
                pendingPublishedCode = null
            },
            onPublish = { versionCode, versionName, releaseNotes, downloadUrl, forceUpdate ->
                pendingPublishedCode = versionCode
                onPublishUpdate(versionCode, versionName, releaseNotes, downloadUrl, forceUpdate)
            }
        )
    }
}

@Composable
private fun SwipeableLiquidGlassThemeSwitch(
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetIndex = if (themeMode == ThemeMode.DARK) 1 else 0
    val coroutineScope = rememberCoroutineScope()
    val thumbPos = remember { Animatable(targetIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(targetIndex.toFloat()) }

    val latestTargetIndex by rememberUpdatedState(targetIndex)
    val latestOnSelect by rememberUpdatedState(onSelectThemeMode)

    val slideSpec = remember {
        tween<Float>(durationMillis = 170, easing = FastOutSlowInEasing)
    }

    LaunchedEffect(targetIndex) {
        if (!isDragging) {
            thumbPos.animateTo(targetIndex.toFloat(), slideSpec)
            dragPos = targetIndex.toFloat()
        }
    }

    val highlightedIndex by remember {
        derivedStateOf {
            val active = if (isDragging) dragPos else thumbPos.value
            active.roundToInt().coerceIn(0, 1)
        }
    }

    val density = LocalDensity.current
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val slotWidthPx = (trackWidthPx / 2f).coerceAtLeast(1f)
    val slotWidthDp = with(density) { slotWidthPx.toDp() }

    LiquidGlassSurface(
        style = LiquidGlassStyle.FROST_WHITE,
        shape = CircleShape,
        cornerRadius = 28.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("settings_theme_swipe_toggle")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .onSizeChanged { size ->
                    if (size.width != trackWidthPx) {
                        trackWidthPx = size.width
                    }
                }
                .pointerInput(slotWidthPx) {
                    var startIdx = 0
                    var totalDragPx = 0f

                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            startIdx = latestTargetIndex
                            dragPos = startIdx.toFloat()
                            totalDragPx = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragPx += dragAmount
                            dragPos = (startIdx + totalDragPx / slotWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val current = dragPos
                            val rawIdx = current.roundToInt().coerceIn(0, 1)
                            val flickThreshold = slotWidthPx * 0.16f
                            val finalIdx = if (rawIdx == startIdx && abs(totalDragPx) > flickThreshold) {
                                if (totalDragPx > 0f) 1 else 0
                            } else {
                                rawIdx
                            }
                            val chosenMode = if (finalIdx == 1) ThemeMode.DARK else ThemeMode.LIGHT
                            coroutineScope.launch {
                                thumbPos.snapTo(current)
                                isDragging = false
                                if (finalIdx != latestTargetIndex) {
                                    latestOnSelect(chosenMode)
                                } else {
                                    thumbPos.animateTo(finalIdx.toFloat(), slideSpec)
                                    dragPos = finalIdx.toFloat()
                                }
                            }
                        },
                        onDragCancel = {
                            val current = dragPos
                            val restore = latestTargetIndex.toFloat()
                            coroutineScope.launch {
                                thumbPos.snapTo(current)
                                isDragging = false
                                thumbPos.animateTo(restore, slideSpec)
                            }
                        }
                    )
                }
        ) {
            // Sliding GPU-accelerated Liquid Glass thumb
            if (trackWidthPx > 0) {
                LiquidGlassSurface(
                    style = LiquidGlassStyle.MINT_PRIMARY,
                    shape = CircleShape,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .width(slotWidthDp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val active = if (isDragging) dragPos else thumbPos.value
                            translationX = active * slotWidthPx
                        }
                ) {}
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeSwipeSlot(
                    icon = Icons.Filled.LightMode,
                    label = stringResource(R.string.settings_light_mode),
                    selected = highlightedIndex == 0,
                    onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_light_mode_button")
                )
                ThemeSwipeSlot(
                    icon = Icons.Filled.DarkMode,
                    label = stringResource(R.string.settings_dark_mode),
                    selected = highlightedIndex == 1,
                    onClick = { onSelectThemeMode(ThemeMode.DARK) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_dark_mode_button")
                )
            }
        }
    }
}

@Composable
private fun ThemeSwipeSlot(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SeparateSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LiquidGlassSurface(
        style = LiquidGlassStyle.FROST_WHITE,
        shape = SettingsCardShape,
        cornerRadius = 22.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingsLeadingIconBadge(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier.size(44.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun SeparateSettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SeparateSettingsCard {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsLeadingIconBadge(icon = icon)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AnimatedRateAppDialog(
    existingRating: Int,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    val isLocked = existingRating > 0
    var selectedStars by rememberSaveable(existingRating) { mutableIntStateOf(existingRating) }
    var selectionPulseId by remember { mutableIntStateOf(if (existingRating > 0) 1 else 0) }

    val moodText = when (selectedStars) {
        1 -> stringResource(R.string.rate_mood_1)
        2 -> stringResource(R.string.rate_mood_2)
        3 -> stringResource(R.string.rate_mood_3)
        4 -> stringResource(R.string.rate_mood_4)
        5 -> stringResource(R.string.rate_mood_5)
        else -> stringResource(R.string.rate_mood_prompt)
    }

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            style = LiquidGlassStyle.FROST_WHITE,
            shape = RoundedCornerShape(28.dp),
            cornerRadius = 28.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rate_app_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isLocked) R.string.rate_dialog_already_title else R.string.rate_dialog_title
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isLocked) {
                        stringResource(R.string.rate_dialog_already_subtitle, existingRating)
                    } else {
                        stringResource(R.string.rate_dialog_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Interactive 5-Star Animated Selector (locked display when already rated once)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (starIndex in 1..5) {
                        AnimatedRatingStar(
                            starIndex = starIndex,
                            isFilled = starIndex <= selectedStars,
                            pulseId = selectionPulseId,
                            enabled = !isLocked,
                            onSelect = {
                                if (!isLocked) {
                                    selectedStars = starIndex
                                    selectionPulseId++
                                }
                            }
                        )
                    }
                }

                // Animated Rating Mood Pill
                AnimatedContent(
                    targetState = Pair(selectedStars, moodText),
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically { it / 3 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically { -it / 3 })
                    },
                    label = "rating_mood_transition"
                ) { (stars, label) ->
                    Surface(
                        shape = CircleShape,
                        color = if (stars > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = if (stars > 0) "$stars / 5  ·  $label" else label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (stars > 0) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (isLocked) {
                    LiquidGlassButton(
                        onClick = onDismiss,
                        style = LiquidGlassStyle.MINT_PRIMARY,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("rate_done_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_done),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = onDismiss,
                            style = LiquidGlassStyle.FROST_WHITE,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("rate_cancel_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        LiquidGlassButton(
                            onClick = { onSubmit(selectedStars) },
                            enabled = selectedStars > 0,
                            style = LiquidGlassStyle.MINT_PRIMARY,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .testTag("rate_submit_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_submit_rating),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedRatingStar(
    starIndex: Int,
    isFilled: Boolean,
    pulseId: Int,
    enabled: Boolean = true,
    onSelect: () -> Unit
) {
    val starScale = remember { Animatable(if (isFilled) 1.08f else 1f) }
    val starRotation = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(if (isFilled) 1f else 0f) }

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.outline

    val starTint by animateColorAsState(
        targetValue = if (isFilled) selectedColor else unselectedColor,
        animationSpec = tween(220),
        label = "star_tint_$starIndex"
    )

    // Staggered spring pop + joyful wiggle + silver halo burst when chosen
    LaunchedEffect(pulseId, isFilled) {
        if (isFilled && pulseId > 0) {
            delay((starIndex - 1) * 45L)
            launch {
                starScale.snapTo(0.76f)
                starScale.animateTo(
                    targetValue = 1.12f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                starRotation.snapTo(-18f)
                starRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                glowAlpha.animateTo(1f, tween(220))
            }
        } else if (!isFilled) {
            launch { starScale.animateTo(1f, tween(160)) }
            launch { starRotation.animateTo(0f, tween(160)) }
            launch { glowAlpha.animateTo(0f, tween(160)) }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = selectedColor),
                onClick = onSelect
            )
            .testTag("rate_star_$starIndex"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    val g = glowAlpha.value
                    alpha = g * 0.35f
                    scaleX = 0.65f + 0.35f * g
                    scaleY = 0.65f + 0.35f * g
                }
                .drawWithCache {
                    val halo = Brush.radialGradient(
                        colors = listOf(unselectedColor, Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f
                    )
                    onDrawBehind {
                        drawCircle(brush = halo)
                    }
                }
        )

        Icon(
            imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = stringResource(R.string.cd_rate_star, starIndex),
            tint = starTint,
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = starScale.value
                    scaleY = starScale.value
                    rotationZ = starRotation.value
                }
        )
    }
}

@Composable
private fun SignedInProfileCard(
    profile: UserProfileUiState,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = extractInitials(profile.displayName)

    SeparateSettingsCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        if (!profile.photoUrl.isNullOrBlank()) {
                            val avatarRequest = remember(profile.photoUrl) {
                                ImageRequest.Builder(context)
                                    .data(profile.photoUrl)
                                    .allowHardware(true)
                                    .crossfade(false)
                                    .build()
                            }
                            AsyncImage(
                                model = avatarRequest,
                                contentDescription = stringResource(R.string.cd_profile_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = profile.displayName.ifBlank { stringResource(R.string.profile_default_name) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("profile_display_name")
                    )

                    if (!profile.email.isNullOrBlank()) {
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("profile_email")
                        )
                    }
                }
            }

            LiquidGlassButton(
                onClick = onSignOutClick,
                style = LiquidGlassStyle.FROST_WHITE,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.cd_sign_out),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun extractInitials(displayName: String): String {
    val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "WX"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
