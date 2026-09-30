package com.keithstack.carlog.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed sightings log, scoped under users/{uid}/sightings.
 * Offline persistence is on by default on Android, so reads/writes work
 * immediately even without connectivity or a real Firebase project —
 * they just won't reach a server until one exists.
 */
class SightingsRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("sightings")

    fun observeSightings(uid: String): Flow<List<Sighting>> = callbackFlow {
        val registration = collection(uid)
            .orderBy("ts", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sightings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Sighting::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(sightings)
            }
        awaitClose { registration.remove() }
    }

    /** [sighting.id] must already be set (client-generated) so callers can reference it immediately, e.g. for snackbar undo. */
    suspend fun addSighting(uid: String, sighting: Sighting) {
        collection(uid).document(sighting.id).set(sighting).await()
    }

    /** Re-adds a previously deleted sighting (used by the undo snackbar action), preserving its id. */
    suspend fun restoreSighting(uid: String, sighting: Sighting) {
        collection(uid).document(sighting.id).set(sighting).await()
    }

    suspend fun deleteSighting(uid: String, id: String) {
        collection(uid).document(id).delete().await()
    }

    suspend fun wipeAll(uid: String) {
        val docs = collection(uid).get().await()
        val batch = firestore.batch()
        docs.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    /** Every sighting in [samples] must already have a client-generated id. */
    suspend fun addSample(uid: String, samples: List<Sighting>) {
        val batch = firestore.batch()
        samples.forEach { sample -> batch.set(collection(uid).document(sample.id), sample) }
        batch.commit().await()
    }
}
