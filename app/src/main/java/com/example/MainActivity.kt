package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppUpdateRepository
import com.example.data.ApplyTarget
import com.example.data.SettingsPreferences
import com.example.data.ThemeMode
import com.example.data.UserProfileRepository
import com.example.data.WallxSettings
import com.example.ui.AppUpdateEvent
import com.example.ui.AppUpdateViewModel
import com.example.ui.UserProfileViewModel
import com.example.ui.WallxViewModel
import com.example.ui.components.LiquidGlassStyle
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.components.WallxIntroOverlay
import com.example.ui.screens.AboutUsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.LocalWallxDarkTheme
import com.example.ui.theme.WallxTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ROUTE_MAIN_TABS = "main_tabs"
private const val ROUTE_PREVIEW = "preview/{source}/{wallpaperId}"
private const val ROUTE_ABOUT_US = "about_us"
private const val MAIN_TAG = "WallxMain"
private const val OWNER_EMAIL = "panchalrahul18827@gmail.com"

private data class WallxTab(
    val index: Int,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val wallxTabs = listOf(
    WallxTab(
        index = 0,
        labelRes = R.string.nav_vault,
        selectedIcon = Icons.Filled.Collections,
        unselectedIcon = Icons.Outlined.Collections,
        testTag = "nav_tab_vault"
    ),
    WallxTab(
        index = 1,
        labelRes = R.string.nav_favorites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_tab_favorites"
    ),
    WallxTab(
        index = 2,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallxRootApp()
        }
    }
}

@Composable
fun WallxRootApp(
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showLaunchIntro by rememberSaveable { mutableStateOf(true) }
    var mountMainContent by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rootSettingsPrefs = remember(context) { SettingsPreferences(context.applicationContext) }
    val rootSettings by rootSettingsPrefs.settingsFlow.collectAsStateWithLifecycle(initialValue = WallxSettings())
    val isDarkTheme = rootSettings.themeMode == ThemeMode.DARK

    val onSignOut: () -> Unit = {
        coroutineScope.launch {
            withContext(NonCancellable) {
                try {
                    CredentialManager.create(context)
                        .clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(MAIN_TAG, "CredentialManager clear state skipped: ${e.message}")
                }
                auth.signOut()
            }
        }
    }

    // Dynamic Light Mode (White/Silver/Black) & Dark Mode (Obsidian/Silver/White)
    WallxTheme(darkTheme = isDarkTheme) {
        val view = LocalView.current
        LaunchedEffect(isDarkTheme) {
            if (!view.isInEditMode) {
                val window = (view.context as? ComponentActivity)?.window ?: return@LaunchedEffect
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mount main screen quietly underneath once the intro entrance settles so there is ZERO freeze on reveal
            if (mountMainContent || !showLaunchIntro) {
                val user = currentUser
                if (user == null) {
                    AuthScreen()
                } else {
                    key(user.uid) {
                        WallxAuthenticatedApp(
                            currentUser = user,
                            onSignOut = onSignOut
                        )
                    }
                }
            }

            // Smooth ~2.35s GPU-only startup animation layered on top
            if (showLaunchIntro) {
                WallxIntroOverlay(
                    onMountUnderlay = { mountMainContent = true },
                    onFinished = {
                        mountMainContent = true
                        showLaunchIntro = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WallxAuthenticatedApp(
    currentUser: FirebaseUser,
    onSignOut: () -> Unit,
    viewModel: WallxViewModel = viewModel()
) {
    // Only the app owner can add, edit, or delete wallpapers
    val isOwner = remember(currentUser.email) {
        currentUser.email.equals(OWNER_EMAIL, ignoreCase = true)
    }

    val profileViewModel: UserProfileViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                UserProfileViewModel(
                    repository = UserProfileRepository(db),
                    currentUserId = currentUser.uid,
                    initialDisplayName = currentUser.displayName
                        ?: currentUser.email?.substringBefore('@')
                        ?: app.getString(R.string.profile_default_name),
                    initialEmail = currentUser.email,
                    initialPhotoUrl = currentUser.photoUrl?.toString()
                )
            }
        }
    )

    val updateViewModel: AppUpdateViewModel = viewModel(
        key = "update_${currentUser.uid}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AppUpdateViewModel(AppUpdateRepository(db))
            }
        }
    )

    LaunchedEffect(currentUser.uid) {
        delay(1200)
        profileViewModel.syncSignedInUser(currentUser)
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPreviewRoute = currentRoute?.startsWith("preview/") == true
    val isAboutUsRoute = currentRoute == ROUTE_ABOUT_US
    val isSubScreenRoute = isPreviewRoute || isAboutUsRoute
    val notificationDeniedMsg = stringResource(R.string.notifications_permission_denied)

    // Switch system bar icon contrast ONLY when theme or preview route changes
    val isDarkTheme = LocalWallxDarkTheme.current
    val view = LocalView.current
    LaunchedEffect(isDarkTheme, isPreviewRoute) {
        if (!view.isInEditMode) {
            val window = (view.context as? ComponentActivity)?.window ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(window, view)
            val useLightBars = !isDarkTheme && !isPreviewRoute
            controller.isAppearanceLightStatusBars = useLightBars
            controller.isAppearanceLightNavigationBars = useLightBars
        }
    }

    // Single active tab rendered at a time -> ZERO offscreen overhead, ZERO accidental screen-swipe page changes!
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Hoisted scroll states preserve exact scroll position and eliminate re-measurement lag on tab switch
    val collectionsGridState = rememberLazyGridState()
    val vaultGridState = rememberLazyGridState()
    val favoritesGridState = rememberLazyGridState()
    val settingsListState = rememberLazyListState()

    val selectTab: (Int) -> Unit = remember {
        { targetIndex ->
            selectedTabIndex = targetIndex.coerceIn(0, wallxTabs.lastIndex)
        }
    }

    // System back button returns smoothly from Favorites or Settings to Wallpapers
    BackHandler(enabled = !isSubScreenRoute && selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    // Zero-permission Android Photo Picker (owner only)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (isOwner && uris.isNotEmpty()) {
            viewModel.importWallpapers(uris)
        }
    }

    val fallbackPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (isOwner && uris.isNotEmpty()) {
            viewModel.importWallpapers(uris)
        }
    }

    val launchWallpaperPicker: () -> Unit = {
        if (isOwner) {
            try {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (_: Exception) {
                fallbackPickerLauncher.launch("image/*")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(updateViewModel) {
        updateViewModel.events.collect { event ->
            val msg = when (event) {
                is AppUpdateEvent.UpToDate -> context.getString(R.string.update_up_to_date_msg, event.currentVersionName)
                is AppUpdateEvent.UpdateFound -> context.getString(R.string.update_found_msg, event.versionName)
                is AppUpdateEvent.CheckFailed -> event.message
                is AppUpdateEvent.PublishSucceeded -> context.getString(R.string.publish_update_success, event.versionName)
                is AppUpdateEvent.PublishFailed -> event.message
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    val density = LocalDensity.current
    var useNavigationRail by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val wide = with(density) { size.width.toDp() >= 640.dp }
                if (wide != useNavigationRail) {
                    useNavigationRail = wide
                }
            }
    ) {

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = if (!isSubScreenRoute && !useNavigationRail) 94.dp else 12.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isSubScreenRoute && useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            wallxTabs.forEach { tab ->
                                val selected = selectedTabIndex == tab.index
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectTab(tab.index) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_MAIN_TABS,
                            enterTransition = {
                                fadeIn(tween(70, easing = FastOutSlowInEasing))
                            },
                            exitTransition = {
                                fadeOut(tween(55, easing = FastOutSlowInEasing))
                            },
                            popEnterTransition = {
                                fadeIn(tween(70, easing = FastOutSlowInEasing))
                            },
                            popExitTransition = {
                                fadeOut(tween(55, easing = FastOutSlowInEasing))
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_MAIN_TABS) {
                                when (selectedTabIndex) {
                                    0 -> VaultTabRoute(
                                        viewModel = viewModel,
                                        isOwner = isOwner,
                                        collectionsGridState = collectionsGridState,
                                        gridState = vaultGridState,
                                        onAddWallpapersClick = launchWallpaperPicker,
                                        onWallpaperClick = { id ->
                                            if (!isSubScreenRoute) {
                                                navController.navigate("preview/vault/$id") {
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        onProfileClick = { selectTab(2) }
                                    )

                                    1 -> FavoritesTabRoute(
                                        viewModel = viewModel,
                                        gridState = favoritesGridState,
                                        onWallpaperClick = { id ->
                                            if (!isSubScreenRoute) {
                                                navController.navigate("preview/favorites/$id") {
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        onOpenVault = { selectTab(0) }
                                    )

                                    else -> SettingsTabRoute(
                                        viewModel = viewModel,
                                        profileViewModel = profileViewModel,
                                        updateViewModel = updateViewModel,
                                        isOwner = isOwner,
                                        listState = settingsListState,
                                        notificationDeniedMsg = notificationDeniedMsg,
                                        onAboutUsClick = {
                                            if (!isSubScreenRoute) {
                                                navController.navigate(ROUTE_ABOUT_US) {
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        onSignOutClick = onSignOut
                                    )
                                }
                            }

                            composable(ROUTE_ABOUT_US) {
                                AboutUsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = ROUTE_PREVIEW,
                                arguments = listOf(
                                    navArgument("source") { type = NavType.StringType },
                                    navArgument("wallpaperId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val source = backStackEntry.arguments?.getString("source") ?: "vault"
                                val wallpaperId = backStackEntry.arguments?.getLong("wallpaperId") ?: -1L
                                PreviewRoute(
                                    viewModel = viewModel,
                                    source = source,
                                    wallpaperId = wallpaperId,
                                    isOwner = isOwner,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }

                // Zero-recomposition opaque-fast Swipeable Bottom Bar
                if (!isSubScreenRoute && !useNavigationRail) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        SwipeableLiquidGlassNavigationBar(
                            selectedIndex = selectedTabIndex,
                            onSelectTab = selectTab
                        )
                    }
                }
            }
        }
    }

    // Isolated update prompt observer so root Scaffold/NavHost never recompose on update checks
    UpdateDialogHost(
        updateViewModel = updateViewModel,
        onShowMessage = { msg -> viewModel.showMessage(msg) }
    )
}

@Composable
private fun VaultTabRoute(
    viewModel: WallxViewModel,
    isOwner: Boolean,
    collectionsGridState: LazyGridState,
    gridState: LazyGridState,
    onAddWallpapersClick: () -> Unit,
    onWallpaperClick: (Long) -> Unit,
    onProfileClick: () -> Unit
) {
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    VaultScreen(
        state = vaultState,
        isOwner = isOwner,
        collectionsGridState = collectionsGridState,
        gridState = gridState,
        onAddWallpapersClick = onAddWallpapersClick,
        onWallpaperClick = onWallpaperClick,
        onToggleFavorite = viewModel::toggleFavorite,
        onSelectTag = viewModel::selectTag,
        onProfileClick = onProfileClick
    )
}

@Composable
private fun FavoritesTabRoute(
    viewModel: WallxViewModel,
    gridState: LazyGridState,
    onWallpaperClick: (Long) -> Unit,
    onOpenVault: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    FavoritesScreen(
        favorites = favorites,
        gridState = gridState,
        onWallpaperClick = onWallpaperClick,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenVault = onOpenVault
    )
}

@Composable
private fun SettingsTabRoute(
    viewModel: WallxViewModel,
    profileViewModel: UserProfileViewModel,
    updateViewModel: AppUpdateViewModel,
    isOwner: Boolean,
    listState: LazyListState,
    notificationDeniedMsg: String,
    onAboutUsClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val userProfile by profileViewModel.profile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        userProfile = userProfile,
        isOwner = isOwner,
        themeMode = settings.themeMode,
        notificationsEnabled = settings.notificationsEnabled,
        userRating = settings.userRating,
        updateState = updateState,
        listState = listState,
        onSelectThemeMode = viewModel::setThemeMode,
        onToggleNotifications = viewModel::setNotificationsEnabled,
        onNotificationsPermissionDenied = { viewModel.showMessage(notificationDeniedMsg) },
        onSubmitRating = viewModel::submitAppRating,
        onCheckForUpdateClick = updateViewModel::openUpdateDialog,
        onPublishUpdate = updateViewModel::publishUpdate,
        onAboutUsClick = onAboutUsClick,
        onSignOutClick = onSignOutClick
    )
}

@Composable
private fun PreviewRoute(
    viewModel: WallxViewModel,
    source: String,
    wallpaperId: Long,
    isOwner: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    val previewList = if (source == "favorites") {
        favorites
    } else {
        vaultState.filteredWallpapers.ifEmpty { vaultState.allWallpapers }
    }

    LaunchedEffect(source, previewList.isEmpty()) {
        if (source == "favorites" && previewList.isEmpty()) {
            onBack()
        }
    }

    val handleUpdateDetails = remember(isOwner, viewModel) {
        { id: Long, title: String, tag: String ->
            if (isOwner) viewModel.updateWallpaperDetails(id, title, tag)
        }
    }
    val handleDeleteWallpaper = remember(isOwner, viewModel, previewList.size, onBack) {
        { item: com.example.data.WallpaperEntity ->
            if (isOwner) {
                viewModel.deleteWallpaper(item) {
                    if (previewList.size <= 1) {
                        onBack()
                    }
                }
            }
        }
    }
    val handleApplyDirect = remember(viewModel) {
        { wallpaper: com.example.data.WallpaperEntity, target: ApplyTarget ->
            viewModel.applyWallpaper(wallpaper, target)
        }
    }
    val handleSetLockScreenReal = remember(viewModel, context) {
        { wallpaper: com.example.data.WallpaperEntity ->
            val intent = viewModel.buildSystemWallpaperIntent(wallpaper)
            val launched = intent != null && runCatching { context.startActivity(intent) }.isSuccess
            if (!launched) {
                // Fallback directly to Lock Screen if the device has no native system wallpaper picker
                viewModel.applyWallpaper(wallpaper, ApplyTarget.LOCK_SCREEN)
            }
        }
    }

    PreviewScreen(
        wallpapers = previewList,
        initialWallpaperId = wallpaperId,
        isOwner = isOwner,
        isApplying = vaultState.isApplying,
        onBack = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onUpdateDetails = handleUpdateDetails,
        onDeleteWallpaper = handleDeleteWallpaper,
        onApplyDirect = handleApplyDirect,
        onSetLockScreenReal = handleSetLockScreenReal
    )
}

@Composable
private fun UpdateDialogHost(
    updateViewModel: AppUpdateViewModel,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val release = updateState.latestRelease

    if (updateState.showUpdateDialog && updateState.hasUpdate && release != null) {
        UpdateAvailableDialog(
            release = release,
            currentVersionName = updateState.currentVersionName,
            onUpdateNow = { targetRelease ->
                val url = targetRelease.downloadUrl?.trim()
                if (!url.isNullOrEmpty() && (url.startsWith("https://") || url.startsWith("http://"))) {
                    val launched = runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.isSuccess
                    if (!launched) {
                        onShowMessage(
                            context.getString(R.string.update_link_unavailable, targetRelease.versionName)
                        )
                    } else if (!targetRelease.forceUpdate) {
                        updateViewModel.dismissUpdateDialog()
                    }
                } else {
                    if (!targetRelease.forceUpdate) {
                        updateViewModel.dismissUpdateDialog()
                    }
                    onShowMessage(
                        context.getString(R.string.update_link_unavailable, targetRelease.versionName)
                    )
                }
            },
            onDismiss = updateViewModel::dismissUpdateDialog
        )
    }
}

@Composable
private fun SwipeableLiquidGlassNavigationBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val maxIndex = (wallxTabs.size - 1).toFloat()
    val coroutineScope = rememberCoroutineScope()
    val pillPosition = remember { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val latestOnSelectTab by rememberUpdatedState(onSelectTab)

    val pillAnimSpec = remember {
        tween<Float>(
            durationMillis = 140,
            easing = FastOutSlowInEasing
        )
    }

    // Smoothly animate pill whenever tab changes via tap or BackHandler
    LaunchedEffect(selectedIndex) {
        if (!isDragging) {
            pillPosition.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = pillAnimSpec
            )
            dragPosition = selectedIndex.toFloat()
        }
    }

    val highlightedIndex by remember {
        derivedStateOf {
            val currentPos = if (isDragging) dragPosition else pillPosition.value
            currentPos.roundToInt().coerceIn(0, wallxTabs.lastIndex)
        }
    }

    val density = LocalDensity.current
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val slotWidthPx = (trackWidthPx.toFloat() / wallxTabs.size).coerceAtLeast(1f)
    val slotWidthDp = with(density) { slotWidthPx.toDp() }

    LiquidGlassSurface(
        style = LiquidGlassStyle.FROST_WHITE,
        shape = CircleShape,
        cornerRadius = 36.dp,
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .height(58.dp)
                .onSizeChanged { size ->
                    if (size.width != trackWidthPx) {
                        trackWidthPx = size.width
                    }
                }
                .pointerInput(slotWidthPx) {
                    var dragStartIndex = 0
                    var cumulativeDragPx = 0f

                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragStartIndex = latestSelectedIndex
                                dragPosition = dragStartIndex.toFloat()
                                cumulativeDragPx = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                cumulativeDragPx += dragAmount
                                // Synchronous direct state update = zero coroutine allocation per pixel
                                dragPosition = (dragStartIndex + cumulativeDragPx / slotWidthPx)
                                    .coerceIn(0f, maxIndex)
                            },
                            onDragEnd = {
                                val currentDragPos = dragPosition
                                val rawIndex = currentDragPos.roundToInt()
                                    .coerceIn(0, wallxTabs.lastIndex)
                                val flickThresholdPx = slotWidthPx * 0.20f
                                val finalTarget = if (rawIndex == dragStartIndex && abs(cumulativeDragPx) > flickThresholdPx) {
                                    if (cumulativeDragPx > 0f) {
                                        (dragStartIndex + 1).coerceAtMost(wallxTabs.lastIndex)
                                    } else {
                                        (dragStartIndex - 1).coerceAtLeast(0)
                                    }
                                } else {
                                    rawIndex
                                }
                                coroutineScope.launch {
                                    pillPosition.snapTo(currentDragPos)
                                    isDragging = false
                                    if (finalTarget != latestSelectedIndex) {
                                        // Let LaunchedEffect(selectedIndex) run the single spring settle (no double animation)
                                        latestOnSelectTab(finalTarget)
                                    } else {
                                        pillPosition.animateTo(
                                            targetValue = finalTarget.toFloat(),
                                            animationSpec = pillAnimSpec
                                        )
                                        dragPosition = finalTarget.toFloat()
                                    }
                                }
                            },
                            onDragCancel = {
                                val currentDragPos = dragPosition
                                val restoreTarget = latestSelectedIndex.toFloat()
                                coroutineScope.launch {
                                    pillPosition.snapTo(currentDragPos)
                                    isDragging = false
                                    pillPosition.animateTo(
                                        targetValue = restoreTarget,
                                        animationSpec = pillAnimSpec
                                    )
                                }
                            }
                        )
                    }
            ) {
            // Silky GPU-accelerated sliding Liquid Glass pill
            if (trackWidthPx > 0) {
                LiquidGlassSurface(
                    style = LiquidGlassStyle.MINT_PRIMARY,
                    shape = CircleShape,
                    cornerRadius = 29.dp,
                    modifier = Modifier
                        .width(slotWidthDp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val activePos = if (isDragging) dragPosition else pillPosition.value
                            translationX = activePos * slotWidthPx
                        }
                ) {}
            }

            // Fixed equal-width slots prevent any layout jumping/jitter during drag
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                wallxTabs.forEach { tab ->
                    val selected = highlightedIndex == tab.index
                    SwipeableLiquidGlassNavItem(
                        tab = tab,
                        selected = selected,
                        onClick = { onSelectTab(tab.index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableLiquidGlassNavItem(
    tab: WallxTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentTint = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(tab.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
            contentDescription = stringResource(tab.labelRes),
            tint = contentTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
