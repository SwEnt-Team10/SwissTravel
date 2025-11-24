package com.github.swent.swisstravel.algorithm.cache

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
 * @param cacheFile The file to which we write the cache entries, this is used mainly for testing
 * @param ioDispatcher The coroutine dispatcher for I/O operations, defaults to Dispatchers.IO.
 */
class DurationCacheLocal(
    private val context: Context,
    private val maxCacheSize: Int = 50000,
    private val cacheFile: File? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DurationCache() {
  private val json = Json { prettyPrint = true }
  private val mutex = Mutex()

  // Use provided file or default
  private val actualCacheFile: File by lazy {
    cacheFile ?: File(context.cacheDir, "duration_cache.json")
  }

  private var cache: MutableMap<String, CacheEntry> = mutableMapOf()
  private var isLoaded = false

  /**
   * Loads the cache from the file if it has not been loaded yet. Uses a mutex to ensure
   * thread-safety. If the cache file exists and is not empty, it decodes the JSON into the
   * in-memory cache.
   */
  private suspend fun loadIfNeeded() {
    mutex.withLock {
      if (isLoaded) return
      if (actualCacheFile.exists()) {
        try {
          val text = withContext(ioDispatcher) { actualCacheFile.readText() }
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

  /**
   * Persists the in-memory cache to the file. Unsafe because it does not use a mutex; should only
   * be called when safe to write. Logs an error if saving fails.
   */
  private suspend fun persistUnsafe() {
    try {
      val text = json.encodeToString(cache)
      withContext(ioDispatcher) { actualCacheFile.writeText(text) }
    } catch (e: Exception) {
      Log.e("DurationCacheLocal", "Failed to save cache", e)
    }
  }

  /**
   * Enforces the maximum cache size using a Least Recently Used (LRU) policy. Removes the oldest
   * entries if the cache exceeds [maxCacheSize].
   *
   * @return true if any entries were evicted, false otherwise.
   */
  private fun enforceLRUUnsafe(): Boolean {
    if (cache.size <= maxCacheSize) return false
    val excess = cache.size - maxCacheSize
    val oldest = cache.entries.sortedBy { it.value.lastUpdateTimestamp }.take(excess)
    oldest.forEach { cache.remove(it.key) }
    Log.d("DurationCacheLocal", "Evicted $excess entries (LRU)")
    return true
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

  override suspend fun enforceLRU(): Boolean {
    var result = false
    mutex.withLock { result = enforceLRUUnsafe() }
    return result
  }
}
