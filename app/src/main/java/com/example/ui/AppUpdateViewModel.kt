package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppRelease
import com.example.data.AppUpdateRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AppUpdateUiState(
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val latestRelease: AppRelease? = null,
    val isChecking: Boolean = false,
    val isPublishing: Boolean = false,
    val showUpdateDialog: Boolean = false
) {
    val hasUpdate: Boolean
        get() = latestRelease != null && latestRelease.versionCode > currentVersionCode
}

sealed interface AppUpdateEvent {
    data class UpToDate(val currentVersionName: String) : AppUpdateEvent
    data class UpdateFound(val versionName: String) : AppUpdateEvent
    data class CheckFailed(val message: String) : AppUpdateEvent
    data class PublishSucceeded(val versionName: String) : AppUpdateEvent
    data class PublishFailed(val message: String) : AppUpdateEvent
}

class AppUpdateViewModel(
    private val repository: AppUpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppUpdateEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AppUpdateEvent> = _events.asSharedFlow()

    private var lastAutoPromptedVersionCode: Int = -1

    init {
        viewModelScope.launch {
            delay(1500)
            repository.observeLatestRelease()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (error is CancellationException && firestoreError == null) {
                        throw error
                    }
                }
                .collect { release ->
                    val shouldAutoPrompt = release != null &&
                        release.versionCode > BuildConfig.VERSION_CODE &&
                        release.versionCode != lastAutoPromptedVersionCode
                    if (shouldAutoPrompt && release != null) {
                        lastAutoPromptedVersionCode = release.versionCode
                    }
                    _uiState.update { current ->
                        current.copy(
                            latestRelease = release,
                            showUpdateDialog = if (shouldAutoPrompt) true else current.showUpdateDialog
                        )
                    }
                }
        }
    }

    fun checkForUpdateNow() {
        if (_uiState.value.isChecking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            val result = repository.checkLatestReleaseOnce()
            _uiState.update { it.copy(isChecking = false) }
            result.onSuccess { release ->
                val isNewer = release != null && release.versionCode > BuildConfig.VERSION_CODE
                _uiState.update {
                    it.copy(
                        latestRelease = release,
                        showUpdateDialog = isNewer
                    )
                }
                if (isNewer && release != null) {
                    lastAutoPromptedVersionCode = release.versionCode
                    _events.emit(AppUpdateEvent.UpdateFound(release.versionName))
                } else {
                    _events.emit(AppUpdateEvent.UpToDate(BuildConfig.VERSION_NAME))
                }
            }.onFailure { error ->
                _events.emit(
                    AppUpdateEvent.CheckFailed(
                        error.localizedMessage ?: "Unable to check for updates"
                    )
                )
            }
        }
    }

    fun openUpdateDialog() {
        if (_uiState.value.hasUpdate) {
            _uiState.update { it.copy(showUpdateDialog = true) }
        } else {
            checkForUpdateNow()
        }
    }

    fun dismissUpdateDialog() {
        val isForced = _uiState.value.latestRelease?.forceUpdate == true && _uiState.value.hasUpdate
        if (!isForced) {
            _uiState.update { it.copy(showUpdateDialog = false) }
        }
    }

    fun publishUpdate(
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String?,
        forceUpdate: Boolean
    ) {
        if (_uiState.value.isPublishing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true) }
            val result = repository.publishRelease(
                versionCode = versionCode,
                versionName = versionName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                forceUpdate = forceUpdate
            )
            _uiState.update { it.copy(isPublishing = false) }
            result.onSuccess {
                _events.emit(AppUpdateEvent.PublishSucceeded(versionName.trim()))
            }.onFailure { error ->
                _events.emit(
                    AppUpdateEvent.PublishFailed(
                        error.localizedMessage ?: "Failed to publish update"
                    )
                )
            }
        }
    }
}
