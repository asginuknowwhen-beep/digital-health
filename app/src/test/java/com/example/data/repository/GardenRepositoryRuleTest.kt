package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GardenDesign
import com.example.data.model.HardscapeZone
import com.example.data.model.PlacedPlant
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleGarden(title: String = "Botanical Courtyard"): GardenDesign = GardenDesign(
        title = title,
        summary = "Sunlit courtyard with limestone pavers and fragrant Mediterranean herbs.",
        style = "Mediterranean",
        widthFeet = 24.0,
        lengthFeet = 18.0,
        sunlight = "Full Sun",
        soilType = "Sandy Loam",
        climateZone = "Zone 8-9",
        hardscapeZones = listOf(
            HardscapeZone(
                id = "zone_path",
                type = "pathway",
                label = "Limestone Walkway",
                x = 0.45,
                y = 0.1,
                width = 0.12,
                height = 0.8,
                materialNote = "Stepping stones with thyme",
            )
        ),
        plants = listOf(
            PlacedPlant(
                id = "plant_lav",
                commonName = "English Lavender",
                botanicalName = "Lavandula angustifolia",
                category = "Perennial",
                sunNeed = "Full Sun",
                waterNeed = "Low",
                matureSpreadFeet = 3.0,
                matureHeightFeet = 2.5,
                bloomSeason = "Summer",
                companionNote = "Pairs with rosemary",
                x = 0.25,
                y = 0.35,
                colorHex = "#7A6A9B",
                localVisualPath = "",
            )
        ),
    )

    @Test
    fun createGarden_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GardenRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGarden(sampleGarden())
        }
        assertTrue("Expected createGarden to succeed: ${result.exceptionOrNull()}", result.isSuccess)
        val gardenId = result.getOrThrow()
        assertTrue(gardenId.isNotBlank())

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getGardenById(uid, gardenId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(gardenId, fetched!!.id)
        assertEquals("Botanical Courtyard", fetched.title)
        assertEquals(1, fetched.plants.size)
        assertEquals("English Lavender", fetched.plants.first().commonName)
    }

    @Test
    fun observeGardens_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GardenRepository(firestore, auth)
        val gardenId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGarden(sampleGarden("Realtime Terrace")).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGardens(uid).first { list -> list.any { it.id == gardenId } }
        }
        assertTrue(emitted.any { it.id == gardenId && it.title == "Realtime Terrace" })
    }

    @Test
    fun updateAndDeleteGarden_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GardenRepository(firestore, auth)
        val gardenId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGarden(sampleGarden("Initial Plot")).getOrThrow()
        }

        val existing = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getGardenById(uid, gardenId).getOrThrow()!!
        }
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateGarden(existing.copy(title = "Refined Sanctuary"))
        }
        assertTrue("Expected updateGarden to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getGardenById(uid, gardenId).getOrThrow()
        }
        assertEquals("Refined Sanctuary", updated?.title)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteGarden(gardenId)
        }
        assertTrue(deleteResult.isSuccess)

        val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getGardenById(uid, gardenId).getOrThrow()
        }
        assertNull(afterDelete)
    }

    @Test
    fun getGardenById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GardenRepository(firestore, auth)
        val gardenId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createGarden(sampleGarden("Alice Secret Garden")).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = GardenRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getGardenById(aliceUid, gardenId)
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun observeGardens_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = GardenRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeGardens(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull("Expected unauthenticated query to fail", caught)
        val firestoreException = caught!!.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.garden@test.com"
        const val BOB_EMAIL = "bob.garden@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
