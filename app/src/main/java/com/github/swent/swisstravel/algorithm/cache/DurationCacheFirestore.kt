package com.github.swent.swisstravel.algorithm.cache

import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val CACHE_COLLECTION_PATH = "durationCache"
// Done with the help of AI
/**
 * Manages cached travel durations between coordinates, stored in Firebase Firestore.
 *
 * Each cache entry corresponds to a (start, end, transportMode) tuple:
 * - Coordinates are rounded to reduce redundant entries.
 * - The cache is shared among all users.
 * - When the cache exceeds a given size, LRU (Least Recently Used) entries are deleted.
 *
 * @param db Firestore database instance.
 * @param roundingPrecision Number of decimal places to round coordinates to.
 * @param cacheCollection Firestore collection name for storing cache entries.
 * @param maxCacheSize Maximum number of entries allowed in the cache.
 */
class DurationCacheFirestore(
    private val db: FirebaseFirestore,
    private val cacheCollection: String = CACHE_COLLECTION_PATH,
    private val maxCacheSize: Int = 10000
) : DurationCache() {
  private var currentSize = 0

  override suspend fun getDuration(
      start: Coordinate,
      end: Coordinate,
      mode: TransportMode
  ): CacheEntry? {
    val key = buildKey(start, end, mode)
    val doc = db.collection(cacheCollection).document(key).get().await()
    return if (doc.exists()) {
      val entry = doc.toObject(CacheEntry::class.java)
      entry?.copy(lastUpdateTimestamp = System.currentTimeMillis())
    } else {
      null
    }
  }

  override suspend fun saveDuration(
      start: Coordinate,
      end: Coordinate,
      duration: Double,
      mode: TransportMode
  ) {
    val key = buildKey(start, end, mode)
    val entry =
        CacheEntry(
            startLat = roundCoord(start.latitude),
            startLng = roundCoord(start.longitude),
            endLat = roundCoord(end.latitude),
            endLng = roundCoord(end.longitude),
            duration = duration,
            transportMode = mode.name,
            lastUpdateTimestamp = System.currentTimeMillis())

    try {
      db.collection(cacheCollection).document(key).set(entry).await()
      Log.d("DurationCache", "Saved: $key")
      currentSize +=
          4 // Adding more to make sure that the cache will be looked at more frequently since there
      // are multiple users that can be using it at the same time
      // Check if we should try to evict
      if (currentSize >= maxCacheSize) {
        // Current size is updated so that if we had to enforce LRU, it becomes the maxCacheSize
        // and if it didn't had to use LRU, it fetches the
        currentSize =
            if (enforceLRU()) {
              maxCacheSize
            } else {
              db.collection(cacheCollection).get().await().size()
            }
      }
    } catch (e: Exception) {
      Log.e("DurationCache", "Error saving $key", e)
    }
  }

  override suspend fun enforceLRU(): Boolean {
    try {
      val snapshot =
          db.collection(cacheCollection)
              .get() // Get all documents first
              .await()

      val excess = snapshot.size() - maxCacheSize
      if (excess > 0) {
        // Sort in memory by timestamp (oldest first)
        val toDelete =
            snapshot.documents
                .sortedBy { doc -> doc.getLong("lastUpdateTimestamp") ?: 0L }
                .take(excess)

        // Delete the oldest entries
        toDelete.forEach { doc -> doc.reference.delete().await() }
        Log.d("DurationCacheFirestore", "Evicted $excess old entries")
        return true
      }
      return false
    } catch (e: Exception) {
      Log.e("DurationCacheFirestore", "Failed to enforce LRU", e)
    }
    return false
  }
}
