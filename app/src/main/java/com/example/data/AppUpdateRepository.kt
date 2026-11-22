package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class AppRelease(
    @DocumentId val id: String = "",
    val versionCode: Int = 1,
    val versionName: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String? = null,
    val forceUpdate: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class AppUpdateRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    /**
     * Real-time observer on /app_config/latest_release so the app immediately detects
     * new updates the moment the owner publishes one.
     */
    fun observeLatestRelease(): Flow<AppRelease?> =
        db.collection(COLLECTION_APP_CONFIG)
            .document(DOC_LATEST_RELEASE)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        AppRelease::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }

    /**
     * Explicit direct check against Firestore server (with cache fallback if offline).
     */
    suspend fun checkLatestReleaseOnce(): Result<AppRelease?> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_LATEST_RELEASE"
        return try {
            requireNotNull(auth.currentUser?.uid?.takeIf { it.isNotBlank() }) {
                "Sign in required to check for updates."
            }
            val docRef = db.collection(COLLECTION_APP_CONFIG).document(DOC_LATEST_RELEASE)
            val snapshot = try {
                docRef.get(Source.SERVER).await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                docRef.get().await()
            }
            val release = if (!snapshot.exists()) {
                null
            } else {
                snapshot.toObject(
                    AppRelease::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            Result.success(release)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Owner-only release publisher writing strictly rule-validated fields to /app_config/latest_release.
     */
    suspend fun publishRelease(
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String?,
        forceUpdate: Boolean
    ): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_LATEST_RELEASE"
        return try {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
                ?: error("User must be signed in before publishing an app update.")
            val safeVersionCode = versionCode.coerceIn(1, 100_000)
            val safeVersionName = versionName.trim().take(32)
            require(safeVersionName.isNotEmpty()) { "Version name is required." }
            val safeNotes = releaseNotes.trim().take(500)
            require(safeNotes.isNotEmpty()) { "Release notes are required." }
            val safeUrl = downloadUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(2048)

            val payload = mutableMapOf<String, Any>(
                "versionCode" to safeVersionCode,
                "versionName" to safeVersionName,
                "releaseNotes" to safeNotes,
                "forceUpdate" to forceUpdate,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeUrl != null) {
                payload["downloadUrl"] = safeUrl
            }

            db.collection(COLLECTION_APP_CONFIG)
                .document(DOC_LATEST_RELEASE)
                .set(payload)
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private companion object {
        const val COLLECTION_APP_CONFIG = "app_config"
        const val DOC_LATEST_RELEASE = "latest_release"
    }
}
