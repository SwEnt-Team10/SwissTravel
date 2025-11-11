package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.tasks.await

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
open class DurationCacheManager(
    private val db: FirebaseFirestore,
    private val roundingPrecision: Int = 3,
    private val cacheCollection: String = "durationCache",
    private val maxCacheSize: Int = 10000
) {

  /**
   * Data class representing a cached duration entry.
   *
   * @param startLat Rounded latitude of the start coordinate.
   * @param startLng Rounded longitude of the start coordinate.
   * @param endLat Rounded latitude of the end coordinate.
   * @param endLng Rounded longitude of the end coordinate.
   * @param duration Cached travel duration in seconds.
   * @param transportMode Mode of transport for the duration.
   * @param lastUpdateTimestamp Timestamp of the last update to this entry.
   */
  data class CacheEntry(
      val startLat: Double = 0.0,
      val startLng: Double = 0.0,
      val endLat: Double = 0.0,
      val endLng: Double = 0.0,
      val duration: Double = -1.0,
      val transportMode: TransportMode = TransportMode.UNKNOWN,
      val lastUpdateTimestamp: Long = System.currentTimeMillis()
  )

  /** Round coordinates to reduce redundant entries (e.g., 3 decimals ≈ 100m) */
  private fun roundCoord(value: Double): Double {
    val factor = 10.0.pow(roundingPrecision)
    return round(value * factor) / factor
  }

  /** Builds a normalized document key for Firestore */
  private fun buildKey(start: Coordinate, end: Coordinate, mode: TransportMode): String {
    val slat = roundCoord(start.latitude)
    val slng = roundCoord(start.longitude)
    val elat = roundCoord(end.latitude)
    val elng = roundCoord(end.longitude)
    return "${mode.name}:$slat,$slng->$elat,$elng"
  }

  /**
   * Retrieves a cached duration between two points.
   *
   * @return The CacheEntry if found, otherwise null.
   */
  open suspend fun getDuration(
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

  /** Saves or updates a duration entry in Firestore. */
  open suspend fun saveDuration(
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
            transportMode = mode,
            lastUpdateTimestamp = System.currentTimeMillis())

    db.collection(cacheCollection)
        .document(key)
        .set(entry, SetOptions.merge())
        .addOnSuccessListener { Log.d("DurationCache", "Saved: $key") }
        .addOnFailureListener { e -> Log.e("DurationCache", "Error saving $key", e) }

    enforceLRU()
  }

  /** Enforces LRU eviction if the cache grows too large. */
  private fun enforceLRU() {
    db.collection(cacheCollection)
        .orderBy("lastUpdateTimestamp")
        .limit(50L)
        .get()
        .addOnSuccessListener { snapshot ->
          if (snapshot.size() > maxCacheSize) {
            val excess = snapshot.size() - maxCacheSize
            snapshot.documents.take(excess).forEach { it.reference.delete() }
            Log.d("DurationCache", "Evicted $excess old entries")
          }
        }
        .addOnFailureListener { e -> Log.e("DurationCache", "Failed to enforce LRU", e) }
  }
}
