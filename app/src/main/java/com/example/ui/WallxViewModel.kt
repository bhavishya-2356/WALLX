package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ApplyTarget
import com.example.data.GalleryLayout
import com.example.data.SettingsPreferences
import com.example.data.SortOrder
import com.example.data.ThemeMode
import com.example.data.WallpaperCropSpec
import com.example.data.WallpaperEntity
import com.example.data.WallpaperRepository
import com.example.data.WallxDatabase
import com.example.data.WallxSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@androidx.compose.runtime.Immutable
data class WallpaperCollectionGroup(
    val key: String,
    val title: String,
    val items: List<WallpaperEntity>
)

@androidx.compose.runtime.Immutable
data class VaultUiState(
    val allWallpapers: List<WallpaperEntity> = emptyList(),
    val filteredWallpapers: List<WallpaperEntity> = emptyList(),
    val collections: List<WallpaperCollectionGroup> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val isApplying: Boolean = false,
    val totalStorageBytes: Long = 0L
)

class WallxViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WallxDatabase.getInstance(application)
    private val repository = WallpaperRepository(application, database.wallpaperDao())
    private val settingsPreferences = SettingsPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val _isImporting = MutableStateFlow(false)
    private val _isApplying = MutableStateFlow(false)
    private val _isSyncingPack = MutableStateFlow(true)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    companion object {
        private const val CURRENT_BUILT_IN_PACK_VERSION = 13
        const val CUSTOM_COLLECTION_KEY = "__custom__"
    }

    init {
        viewModelScope.launch {
            try {
                if (settingsPreferences.getBuiltInPackVersion() < CURRENT_BUILT_IN_PACK_VERSION) {
                    repository.seedBuiltInCollections()
                    settingsPreferences.setBuiltInPackVersion(CURRENT_BUILT_IN_PACK_VERSION)
                }
            } finally {
                _isSyncingPack.value = false
            }
        }
    }

    val settings: StateFlow<WallxSettings> = settingsPreferences.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WallxSettings()
    )

    val favorites: StateFlow<List<WallpaperEntity>> = repository.favoriteWallpapers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val filterParams = combine(
        _searchQuery,
        _selectedTag,
        _sortOrder,
        _isImporting,
        _isApplying
    ) { query, tag, sort, importing, applying ->
        FilterState(query, tag, sort, importing, applying)
    }

    val vaultState: StateFlow<VaultUiState> = combine(
        repository.allWallpapers,
        filterParams,
        _isSyncingPack
    ) { wallpapers, filter, isSyncingPack ->
        val tags = wallpapers
            .map { it.tag.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

        val activeTag = filter.selectedTag?.takeIf { it in tags }

        val filtered = wallpapers
            .filter { item ->
                val matchesQuery = filter.query.isBlank() ||
                    item.title.contains(filter.query, ignoreCase = true) ||
                    item.tag.contains(filter.query, ignoreCase = true)
                val matchesTag = activeTag == null || item.tag.equals(activeTag, ignoreCase = true)
                matchesQuery && matchesTag
            }
            .let { list ->
                when (filter.sortOrder) {
                    SortOrder.NEWEST -> list.sortedByDescending { it.addedAt }
                    SortOrder.OLDEST -> list.sortedBy { it.addedAt }
                    SortOrder.NAME -> list.sortedBy { it.title.lowercase() }
                }
            }

        val customPackTitle = getApplication<Application>().getString(R.string.pack_custom_title)
        val collections = if (wallpapers.isEmpty()) {
            emptyList()
        } else {
            val grouped = wallpapers.groupBy { item ->
                item.tag.trim().ifEmpty { CUSTOM_COLLECTION_KEY }
            }
            val orderedKeys = buildList {
                if (grouped.containsKey(WallpaperRepository.COLLECTION_IPHONE_16_PRO)) {
                    add(WallpaperRepository.COLLECTION_IPHONE_16_PRO)
                }
                if (grouped.containsKey(WallpaperRepository.COLLECTION_CAR)) {
                    add(WallpaperRepository.COLLECTION_CAR)
                }
                if (grouped.containsKey(WallpaperRepository.COLLECTION_WALLPAPER)) {
                    add(WallpaperRepository.COLLECTION_WALLPAPER)
                }
                if (grouped.containsKey(WallpaperRepository.COLLECTION_NATURE)) {
                    add(WallpaperRepository.COLLECTION_NATURE)
                }
                if (grouped.containsKey(WallpaperRepository.COLLECTION_DEPTH_EFFECT)) {
                    add(WallpaperRepository.COLLECTION_DEPTH_EFFECT)
                }
                if (grouped.containsKey(WallpaperRepository.COLLECTION_SAMSUNG)) {
                    add(WallpaperRepository.COLLECTION_SAMSUNG)
                }
                grouped.keys
                    .filter {
                        it != WallpaperRepository.COLLECTION_IPHONE_16_PRO &&
                            it != WallpaperRepository.COLLECTION_CAR &&
                            it != WallpaperRepository.COLLECTION_WALLPAPER &&
                            it != WallpaperRepository.COLLECTION_NATURE &&
                            it != WallpaperRepository.COLLECTION_DEPTH_EFFECT &&
                            it != WallpaperRepository.COLLECTION_SAMSUNG &&
                            it != CUSTOM_COLLECTION_KEY
                    }
                    .sorted()
                    .forEach { add(it) }
                if (grouped.containsKey(CUSTOM_COLLECTION_KEY)) {
                    add(CUSTOM_COLLECTION_KEY)
                }
            }
            orderedKeys.mapNotNull { key ->
                val list = grouped[key] ?: return@mapNotNull null
                WallpaperCollectionGroup(
                    key = key,
                    title = if (key == CUSTOM_COLLECTION_KEY) customPackTitle else key,
                    items = list
                )
            }
        }

        VaultUiState(
            allWallpapers = wallpapers,
            filteredWallpapers = filtered,
            collections = collections,
            availableTags = tags,
            searchQuery = filter.query,
            selectedTag = activeTag,
            sortOrder = filter.sortOrder,
            isLoading = isSyncingPack && wallpapers.isEmpty(),
            isImporting = filter.isImporting,
            isApplying = filter.isApplying,
            totalStorageBytes = wallpapers.sumOf { it.fileSizeBytes }
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(isLoading = true)
    )

    fun observeWallpaper(id: Long): Flow<WallpaperEntity?> = repository.observeWallpaper(id)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun importWallpapers(
        uris: List<Uri>,
        targetCollection: String = _selectedTag.value ?: WallpaperRepository.COLLECTION_WALLPAPER
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val count = repository.importFromUris(uris, targetCollection)
            _isImporting.value = false
            val ctx = getApplication<Application>()
            val msg = when {
                count <= 0 -> ctx.getString(R.string.import_failed)
                count == 1 -> ctx.getString(R.string.imported_single)
                else -> ctx.getString(R.string.imported_multiple, count)
            }
            _messages.emit(msg)
        }
    }

    fun toggleFavorite(wallpaper: WallpaperEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(wallpaper)
        }
    }

    fun updateWallpaperDetails(id: Long, title: String, tag: String) {
        viewModelScope.launch {
            repository.updateDetails(id = id, title = title, tag = tag)
            _messages.emit(getApplication<Application>().getString(R.string.wallpaper_updated))
        }
    }

    fun deleteWallpaper(wallpaper: WallpaperEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWallpaper(wallpaper)
            _messages.emit(getApplication<Application>().getString(R.string.wallpaper_deleted))
            onDeleted()
        }
    }

    fun clearAllWallpapers() {
        viewModelScope.launch {
            repository.clearAllWallpapers()
            _selectedTag.value = null
            _searchQuery.value = ""
            _messages.emit(getApplication<Application>().getString(R.string.vault_cleared))
        }
    }

    fun buildSystemWallpaperIntent(wallpaper: WallpaperEntity): Intent? {
        return repository.createSystemWallpaperIntent(wallpaper)
    }

    fun applyWallpaper(
        wallpaper: WallpaperEntity,
        target: ApplyTarget,
        cropSpec: WallpaperCropSpec = WallpaperCropSpec(),
        onComplete: () -> Unit = {}
    ) {
        if (_isApplying.value) return
        viewModelScope.launch {
            _isApplying.value = true
            val result = repository.applyWallpaper(wallpaper, target, cropSpec)
            _isApplying.value = false
            val ctx = getApplication<Application>()
            val msg = if (result.isSuccess) {
                when (target) {
                    ApplyTarget.HOME_SCREEN -> ctx.getString(R.string.wallpaper_applied_home)
                    ApplyTarget.LOCK_SCREEN -> ctx.getString(R.string.wallpaper_applied_lock)
                    ApplyTarget.BOTH_SCREENS,
                    ApplyTarget.ASK_ALWAYS -> ctx.getString(R.string.wallpaper_applied_both)
                }
            } else {
                ctx.getString(R.string.wallpaper_apply_failed)
            }
            _messages.emit(msg)
            onComplete()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsPreferences.setThemeMode(mode) }
    }

    fun setGalleryLayout(layout: GalleryLayout) {
        viewModelScope.launch { settingsPreferences.setGalleryLayout(layout) }
    }

    fun toggleGalleryLayout() {
        val next = if (settings.value.galleryLayout == GalleryLayout.GRID_TWO_COL) {
            GalleryLayout.SHOWCASE_SINGLE
        } else {
            GalleryLayout.GRID_TWO_COL
        }
        setGalleryLayout(next)
    }

    fun setDefaultApplyTarget(target: ApplyTarget) {
        viewModelScope.launch { settingsPreferences.setDefaultApplyTarget(target) }
    }

    fun setShowResolutionBadge(show: Boolean) {
        viewModelScope.launch { settingsPreferences.setShowResolutionBadge(show) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setNotificationsEnabled(enabled)
            val ctx = getApplication<Application>()
            val msg = ctx.getString(
                if (enabled) R.string.notifications_turned_on else R.string.notifications_turned_off
            )
            _messages.emit(msg)
        }
    }

    fun submitAppRating(stars: Int) {
        if (settings.value.userRating > 0) return
        val clamped = stars.coerceIn(1, 5)
        viewModelScope.launch {
            val saved = settingsPreferences.setUserRatingOnce(clamped)
            if (saved) {
                val ctx = getApplication<Application>()
                _messages.emit(ctx.getString(R.string.rate_app_thank_you, clamped))
            }
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }

    private data class FilterState(
        val query: String,
        val selectedTag: String?,
        val sortOrder: SortOrder,
        val isImporting: Boolean,
        val isApplying: Boolean
    )
}
