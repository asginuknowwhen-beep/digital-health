package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.GardenDesign
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GardenRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun gardensCollection(userId: String) =
        db.collection("users").document(userId).collection("gardens")

    fun observeGardens(userId: String): Flow<List<GardenDesign>> = flow {
        val path = "users/$userId/gardens"
        emitAll(
            gardensCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GardenDesign::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeGarden(userId: String, gardenId: String): Flow<GardenDesign?> = flow {
        val docRef = gardensCollection(userId).document(gardenId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        GardenDesign::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun getGardenById(ownerUserId: String, gardenId: String): Result<GardenDesign?> {
        val docRef = gardensCollection(ownerUserId).document(gardenId)
        return try {
            val snapshot = docRef.get().await()
            val design = if (snapshot.exists()) {
                snapshot.toObject(
                    GardenDesign::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            } else {
                null
            }
            Result.success(design)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun createGarden(design: GardenDesign): Result<String> {
        val uid = requireUserId()
        val docRef = gardensCollection(uid).document()
        val payload = mapOf(
            "userId" to uid,
            "title" to design.title.trim().take(120),
            "summary" to design.summary.trim().take(1000),
            "style" to design.style.trim().take(80),
            "widthFeet" to design.widthFeet.coerceIn(2.0, 500.0),
            "lengthFeet" to design.lengthFeet.coerceIn(2.0, 500.0),
            "sunlight" to design.sunlight.trim().take(60),
            "soilType" to design.soilType.trim().take(60),
            "climateZone" to design.climateZone.trim().take(60),
            "hardscapeZones" to design.hardscapeZones.take(20).map { it.toMap() },
            "plants" to design.plants.take(60).map { it.toMap() },
            "conceptVisualPath" to design.conceptVisualPath.take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateGarden(design: GardenDesign): Result<Unit> {
        val uid = requireUserId()
        require(design.id.isNotBlank()) { "Garden ID must not be blank for update" }
        val docRef = gardensCollection(uid).document(design.id)
        val updates = mapOf<String, Any>(
            "title" to design.title.trim().take(120),
            "summary" to design.summary.trim().take(1000),
            "style" to design.style.trim().take(80),
            "widthFeet" to design.widthFeet.coerceIn(2.0, 500.0),
            "lengthFeet" to design.lengthFeet.coerceIn(2.0, 500.0),
            "sunlight" to design.sunlight.trim().take(60),
            "soilType" to design.soilType.trim().take(60),
            "climateZone" to design.climateZone.trim().take(60),
            "hardscapeZones" to design.hardscapeZones.take(20).map { it.toMap() },
            "plants" to design.plants.take(60).map { it.toMap() },
            "conceptVisualPath" to design.conceptVisualPath.take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteGarden(gardenId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = gardensCollection(uid).document(gardenId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }
}
