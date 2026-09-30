package com.keithstack.carlog.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Anonymous Firebase auth. The uid persists in the app's local Firebase
 * install state across restarts, but NOT across an uninstall.
 *
 * Upgrade path if that durability gap matters later: call
 * `auth.currentUser.linkWithCredential(GoogleAuthProvider.getCredential(...))`
 * on the existing anonymous user after a Google Sign-In — this upgrades the
 * same uid in place, so existing Firestore data under `users/{uid}` is kept,
 * and it becomes recoverable by signing in again after a reinstall.
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

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: fallbackUid()
        } catch (e: Exception) {
            fallbackUid()
        }
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
