package com.keithstack.carlog.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Freeform per-car-number notes (e.g. "spotted this one with the old moquette"),
 * separate from the sightings log — one doc per car number, scoped under
 * users/{uid}/carNotes.
 */
class CarNotesRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("carNotes")

    fun observeNotes(uid: String): Flow<Map<String, String>> = callbackFlow {
        val registration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val notes = snapshot?.documents?.associate { doc ->
                doc.id to (doc.getString("text") ?: "")
            } ?: emptyMap()
            trySend(notes)
        }
        awaitClose { registration.remove() }
    }

    suspend fun setNote(uid: String, car: String, text: String) {
        if (text.isBlank()) {
            collection(uid).document(car).delete().await()
        } else {
            collection(uid).document(car).set(mapOf("text" to text)).await()
        }
    }
}
