package com.github.swent.swisstravel.utils

import com.github.swent.swisstravel.model.trip.TRIPS_COLLECTION_PATH
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.trip.TripsRepositoryFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class FirestoreSwissTravelTest : SwissTravelTest() {
  override fun createInitializedRepository(): TripsRepository {
    return TripsRepositoryFirestore(db = FirebaseEmulator.firestore, auth = FirebaseEmulator.auth)
  }

  suspend fun getTripsCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(TRIPS_COLLECTION_PATH)
        .whereEqualTo("ownerId", user.uid)
        .get()
        .await()
        .size()
  }

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

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
