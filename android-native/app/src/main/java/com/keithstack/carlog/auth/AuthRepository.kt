package com.keithstack.carlog.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class AccountInfo(val isAnonymous: Boolean, val displayName: String?, val email: String?)

/**
 * Firebase auth: starts anonymous, can be upgraded to Google Sign-In.
 *
 * Linking (rather than a fresh sign-in) keeps the same uid, so existing
 * Firestore data under `users/{uid}` carries over — this is what makes the
 * anonymous-auth durability gap fixable later without a data migration.
 *
 * If Firebase Auth can't be reached (no real project configured yet, or no
 * network), falls back to a locally-persisted random id so the app still
 * works fully offline against Firestore's local cache.
 */
class AuthRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val prefs = context.getSharedPreferences("carlog_auth", Context.MODE_PRIVATE)

    private val _accountState = MutableStateFlow(auth.currentUser.toAccountInfo())
    val accountState: StateFlow<AccountInfo?> = _accountState.asStateFlow()

    init {
        auth.addAuthStateListener { a -> _accountState.value = a.currentUser.toAccountInfo() }
    }

    private fun FirebaseUser?.toAccountInfo(): AccountInfo? =
        this?.let { AccountInfo(it.isAnonymous, it.displayName, it.email) }

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: fallbackUid()
        } catch (e: Exception) {
            fallbackUid()
        }
    }

    /**
     * Links the current anonymous user to this Google credential, preserving its uid and data.
     *
     * Firebase's AuthStateListener only fires on a sign-in/sign-out transition, not when an
     * already-signed-in user is linked to a new credential — so [_accountState] is updated
     * explicitly here rather than relying on the listener in `init`.
     */
    suspend fun linkGoogleIdToken(idToken: String): Result<Unit> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        return try {
            if (current != null && current.isAnonymous) {
                try {
                    current.linkWithCredential(credential).await()
                } catch (e: FirebaseAuthUserCollisionException) {
                    // This Google account is already tied to a different Firebase user
                    // (e.g. signed in from another device before) — switch to that one.
                    auth.signInWithCredential(credential).await()
                }
            } else {
                auth.signInWithCredential(credential).await()
            }
            _accountState.value = auth.currentUser.toAccountInfo()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        _accountState.value = auth.currentUser.toAccountInfo()
    }

    private fun fallbackUid(): String {
        prefs.getString(KEY_FALLBACK_UID, null)?.let { return it }
        val id = "local-" + UUID.randomUUID()
        prefs.edit().putString(KEY_FALLBACK_UID, id).apply()
        return id
    }

    companion object {
        private const val KEY_FALLBACK_UID = "fallback_uid"
    }
}
