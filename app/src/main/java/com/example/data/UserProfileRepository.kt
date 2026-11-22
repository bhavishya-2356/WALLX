package com.example.data

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class UserProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection(COLLECTION_USERS)
            .document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }

    suspend fun syncSignedInUser(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "$COLLECTION_USERS/$uid"
        return try {
            val signedInUid = auth.currentUser?.uid
                ?: error("User must be signed in with Google before syncing profile.")
            check(signedInUid == uid) { "Signed-in user changed; refusing cross-user write." }

            val sanitizedName = (user.displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: user.email?.substringBefore('@')?.trim()?.takeIf { it.isNotEmpty() }
                ?: "WALLX Member").take(100)
            val sanitizedEmail = user.email?.trim()?.takeIf { it.isNotEmpty() }?.take(254)
            val sanitizedPhoto = user.photoUrl?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.take(2048)

            val docRef = db.collection(COLLECTION_USERS).document(uid)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to sanitizedName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (sanitizedEmail != null) createPayload["email"] = sanitizedEmail
                if (sanitizedPhoto != null) createPayload["photoUrl"] = sanitizedPhoto
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to sanitizedName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (sanitizedEmail != null) updatePayload["email"] = sanitizedEmail
                if (sanitizedPhoto != null) updatePayload["photoUrl"] = sanitizedPhoto
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private companion object {
        const val COLLECTION_USERS = "users"
    }
}
