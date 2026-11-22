package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OperationType
import com.example.data.UserProfile
import com.example.data.UserProfileRepository
import com.example.data.handleFirestoreError
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@androidx.compose.runtime.Immutable
data class UserProfileUiState(
    val userId: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val isSynced: Boolean = false
)

class UserProfileViewModel(
    private val repository: UserProfileRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String?,
    initialPhotoUrl: String?
) : ViewModel() {

    private val fallbackProfile = UserProfileUiState(
        userId = currentUserId,
        displayName = initialDisplayName.ifBlank { "WALLX Member" },
        email = initialEmail,
        photoUrl = initialPhotoUrl,
        isSynced = false
    )

    val profile: StateFlow<UserProfileUiState> = repository.observeUserProfile(currentUserId)
        .map { remote: UserProfile? ->
            if (remote != null) {
                UserProfileUiState(
                    userId = remote.userId.ifBlank { currentUserId },
                    displayName = remote.displayName.ifBlank { fallbackProfile.displayName },
                    email = remote.email ?: fallbackProfile.email,
                    photoUrl = remote.photoUrl ?: fallbackProfile.photoUrl,
                    isSynced = true
                )
            } else {
                fallbackProfile
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            handleFirestoreError(rootError, OperationType.GET, "users/$currentUserId")
            Log.w(TAG, "Error observing user profile; using FirebaseAuth fallback", rootError)
            emit(fallbackProfile)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = fallbackProfile
        )

    fun syncSignedInUser(user: FirebaseUser) {
        viewModelScope.launch {
            repository.syncSignedInUser(user)
        }
    }

    private companion object {
        const val TAG = "UserProfileVM"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
