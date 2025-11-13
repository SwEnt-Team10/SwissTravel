package com.github.swent.swisstravel.utils

import com.github.swent.swisstravel.model.trip.TRIPS_COLLECTION_PATH
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

/**
 * Base class for tests that require a Firestore-backed [TripsRepository].
 *
 * This uses the local Firebase emulator for Firestore and Auth, ensuring tests do not depend on
 * network connectivity or production data.
 *
 * Provides utility functions to inspect and clean up test data.
 */
open class FirestoreSwissTravelTest : SwissTravelTest() {
  override fun createInitializedRepository(): TripsRepository {
    return TripsRepositoryFirestore(db = FirebaseEmulator.firestore, auth = FirebaseEmulator.auth)
  }

  /**
   * Returns the number of trips belonging to the currently authenticated user.
   *
   * If no user is logged in, returns 0.
   */
  suspend fun getTripsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(TRIPS_COLLECTION_PATH)
        .whereEqualTo("ownerId", user.uid)
        .get()
        .await()
        .size()
  }

  /**
   * Deletes all trips belonging to the currently authenticated test user from the emulator
   * Firestore database.
   *
   * Also asserts that the collection is fully cleared.
   */
  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val trips =
        FirebaseEmulator.firestore
            .collection(TRIPS_COLLECTION_PATH)
            .whereEqualTo("ownerId", user.uid)
            .get()
            .await()

    val batch = FirebaseEmulator.firestore.batch()
    trips.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getTripsCount() == 0) {
      "Test collection is not empty after clearing, count: ${getTripsCount()}"
    }
  }

  /**
   * Called before each test.
   *
   * Delegates to the parent setup, which handles authentication and repository initialization.
   */
  @Before
  override fun setUp() {
    super.setUp()
  }

  /**
   * Called after each test.
   *
   * Ensures all Firestore test data is cleared and resets the emulator state.
   */
  @After
  override fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
