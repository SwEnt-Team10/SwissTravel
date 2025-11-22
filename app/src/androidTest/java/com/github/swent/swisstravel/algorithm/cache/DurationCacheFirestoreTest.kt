package com.github.swent.swisstravel.algorithm.cache

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.FirestoreSwissTravelTest
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before

class DurationCacheFirestoreTest : FirestoreSwissTravelTest() {
  private lateinit var firestore: FirebaseFirestore
  private lateinit var cache: DurationCacheFirestore
  val collection = "durationCache"

  @Before
  fun setup() {
    super.setUp()
    runBlocking { FirebaseEmulator.clearFirestoreEmulator() }
    firestore = FirebaseEmulator.firestore
    cache =
        DurationCacheFirestore(
            db = firestore,
            cacheCollection = collection,
            maxCacheSize = 3 // small so we can test eviction
            )
  }

  @After
  override fun tearDown() {
    super.tearDown()
    runBlocking { FirebaseEmulator.clearFirestoreEmulator() }
  }

  @Test
  fun saveAndRetrieveDurations() = runBlocking {
    val start = Coordinate(47.3769, 8.5417) // Zurich
    val end = Coordinate(46.9480, 7.4474) // Bern

    val duration = 3500.0
    val fail = cache.getDuration(start, end, TransportMode.CAR)
    assertNull(fail, "The cache should be empty")

    cache.saveDuration(start, end, duration, TransportMode.CAR)

    val loaded = cache.getDuration(start, end, TransportMode.CAR)
    assertNotNull(loaded, "getDuration didn't return anything")
    assertEquals(duration, loaded.duration, 0.001, "The duration is not the expected one")
  }

  @Test
  fun lruEvictionWorksCorrectly() = runBlocking {
    val start = Coordinate(47.0, 8.0)
    val end = Coordinate(46.0, 7.0)

    // Create 4 entries (limit is 3)
    for (i in 1..4) {
      cache.saveDuration(
          start.copy(latitude = start.latitude + i * 1),
          end,
          duration = i * 100.0,
          mode = TransportMode.WALKING)
    }

    val docs = firestore.collection(collection).get().await()
    println("DEBUG: Document count = ${docs.size()}")
    docs.documents.forEach { doc ->
      println("DEBUG: ${doc.id} -> ${doc.getLong("lastUpdateTimestamp")}")
    }
    assertEquals(3, docs.size(), "There was a problem with the lru")
  }
}
