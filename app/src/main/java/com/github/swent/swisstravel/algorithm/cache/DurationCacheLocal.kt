package com.github.swent.swisstravel.algorithm.cache

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

// Done with the help of AI
/**
 * Local implementation of [DurationCache] that stores duration entries on disk inside the
 * application's internal cache directory.
 *
 * The cache is:
 * - Persisted as a JSON file (`duration_cache.json`)
 * - Loaded lazily on first access
 * - Fully managed in memory during runtime
 * - Thread-safe through the use of a [Mutex]
 * - Maintained using simple LRU eviction when the maximum size is exceeded
 *
 * This cache is designed to provide fast, offline access to previously computed route durations
 * without requiring network or Firestore access.
 *
 * @param context Android context used to access the app's internal storage.
 * @param maxCacheSize Maximum number of entries allowed before LRU eviction.
 */
class DurationCacheLocal(private val context: Context, private val maxCacheSize: Int = 50000) :
    DurationCache() {

  override val roundingPrecision: Int
    get() = 3

  private val json = Json { prettyPrint = true }
  private val mutex = Mutex()

  private val cacheFile: File by lazy { File(context.cacheDir, "duration_cache.json") }

  /** In-memory cache map */
  private var cache: MutableMap<String, CacheEntry> = mutableMapOf()

  /** True once cache has been loaded from disk */
  private var isLoaded = false

  /** Load cache from disk (lazy, only once) */
  private suspend fun loadIfNeeded() {
    mutex.withLock {
      if (isLoaded) return
      if (cacheFile.exists()) {
        try {
          val text = cacheFile.readText()
          if (text.isNotEmpty()) {
            cache = json.decodeFromString(text)
            Log.d("DurationCacheLocal", "Loaded ${cache.size} entries")
          }
        } catch (e: Exception) {
          Log.e("DurationCacheLocal", "Failed to load cache", e)
        }
      }
      isLoaded = true
    }
  }

  /** Persist cache to disk. Caller **must hold the mutex** to avoid deadlocks. */
  private fun persistUnsafe() {
    try {
      val text = json.encodeToString(cache)
      cacheFile.writeText(text)
    } catch (e: Exception) {
      Log.e("DurationCacheLocal", "Failed to save cache", e)
    }
  }

  /** LRU eviction. Caller **must hold the mutex**. */
  private fun enforceLRUUnsafe() {
    if (cache.size <= maxCacheSize) return
    val excess = cache.size - maxCacheSize
    val oldest = cache.entries.sortedBy { it.value.lastUpdateTimestamp }.take(excess)
    oldest.forEach { cache.remove(it.key) }
    Log.d("DurationCacheLocal", "Evicted $excess entries (LRU)")
  }

  override suspend fun getDuration(
      start: Coordinate,
      end: Coordinate,
      mode: TransportMode
  ): CacheEntry? {
    loadIfNeeded()

    val key = buildKey(start, end, mode)
    return mutex.withLock {
      val entry = cache[key]
      if (entry != null) {
        // update timestamp
        val updated = entry.copy(lastUpdateTimestamp = System.currentTimeMillis())
        cache[key] = updated
        persistUnsafe()
        updated
      } else null
    }
  }

  override suspend fun saveDuration(
      start: Coordinate,
      end: Coordinate,
      duration: Double,
      mode: TransportMode
  ) {
    loadIfNeeded()

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

    mutex.withLock {
      cache[key] = entry
      enforceLRUUnsafe()
      persistUnsafe()
    }
  }

  override suspend fun enforceLRU() {
    mutex.withLock { enforceLRUUnsafe() }
  }
}
