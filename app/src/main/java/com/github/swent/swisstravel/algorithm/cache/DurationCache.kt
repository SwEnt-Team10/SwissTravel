package com.github.swent.swisstravel.algorithm.cache

import android.annotation.SuppressLint
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import kotlin.math.pow
import kotlin.math.round
import kotlinx.serialization.Serializable

// Done with the help of AI
/** Abstract class defining what a cache for trips durations should look like */
abstract class DurationCache {
  abstract val roundingPrecision: Int

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
  @SuppressLint("UnsafeOptInUsageError")
  @Serializable
  data class CacheEntry(
      val startLat: Double = 0.0,
      val startLng: Double = 0.0,
      val endLat: Double = 0.0,
      val endLng: Double = 0.0,
      val duration: Double = -1.0,
      val transportMode: String = "UNKNOWN",
      val lastUpdateTimestamp: Long = System.currentTimeMillis()
  )

  /** Round coordinates to reduce redundant entries (e.g., 3 decimals ≈ 100m) */
  fun roundCoord(value: Double): Double {
    val factor = 10.0.pow(roundingPrecision)
    return round(value * factor) / factor
  }

  /**
   * Builds a normalized document key for Firestore
   *
   * @param start The start coordinate.
   * @param end The end coordinate.
   * @param mode The transportation mode.
   * @return a key for Firestore
   */
  fun buildKey(start: Coordinate, end: Coordinate, mode: TransportMode): String {
    val slat = roundCoord(start.latitude)
    val slng = roundCoord(start.longitude)
    val elat = roundCoord(end.latitude)
    val elng = roundCoord(end.longitude)
    return "${mode.name}:$slat,$slng->$elat,$elng"
  }

  /**
   * Retrieves a cached duration between two points.
   *
   * @param start The start coordinate.
   * @param end The end coordinate.
   * @param mode The transportation mode.
   * @return The CacheEntry if found, otherwise null.
   */
  abstract suspend fun getDuration(
      start: Coordinate,
      end: Coordinate,
      mode: TransportMode
  ): CacheEntry?

  /**
   * Saves or updates a duration entry in Firestore.
   *
   * @param start start coordinate
   * @param end end coordinate
   * @param duration duration of the trip from start to end in the chosen transportation mode
   * @param mode transportation mode
   */
  abstract suspend fun saveDuration(
      start: Coordinate,
      end: Coordinate,
      duration: Double,
      mode: TransportMode
  )

  /** Enforces LRU eviction if the cache grows too large. */
  abstract suspend fun enforceLRU()
}
