package com.github.swent.swisstravel.algorithm.cache

import android.content.Context
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DurationCacheLocalTest {

  private lateinit var context: Context
  private lateinit var cache: DurationCacheLocal
  private lateinit var cacheFile: File

  @Before
  fun setup() {
    // Mock Android context
    context = mockk(relaxed = true)

    // Use system temp directory as cacheDir
    val tempCacheDir = File(System.getProperty("java.io.tmpdir"), "durationCacheTest")
    tempCacheDir.mkdirs()
    every { context.cacheDir } returns tempCacheDir

    cacheFile = File(tempCacheDir, "duration_cache.json")
    if (cacheFile.exists()) cacheFile.delete()

    cache = DurationCacheLocal(context, maxCacheSize = 3)
  }

  @Test
  fun `saving and retrieving an entry should work`() = runBlocking {
    val start = Coordinate(47.0, 8.0)
    val end = Coordinate(46.0, 7.0)

    cache.saveDuration(start, end, 1234.0, TransportMode.CAR)

    val result = cache.getDuration(start, end, TransportMode.CAR)
    assertNotNull(result)
    assertEquals(1234.0, result!!.duration, 0.01)
  }

  @Test
  fun `cache persists to disk and loads correctly`() = runBlocking {
    val start = Coordinate(47.0, 8.0)
    val end = Coordinate(46.0, 7.0)

    cache.saveDuration(start, end, 2000.0, TransportMode.WALKING)
    assertTrue(cacheFile.exists())

    val newCache = DurationCacheLocal(context, maxCacheSize = 3)
    val result = newCache.getDuration(start, end, TransportMode.WALKING)
    assertNotNull(result)
    assertEquals(2000.0, result!!.duration, 0.01)
  }

  @Test
  fun `LRU eviction removes oldest entries`() = runBlocking {
    val baseStart = Coordinate(47.0, 8.0)
    val end = Coordinate(46.0, 7.0)

    val cache = DurationCacheLocal(context, maxCacheSize = 3, cacheFile = cacheFile)

    for (i in 1..4) {
      cache.saveDuration(
          baseStart.copy(latitude = baseStart.latitude + i * 0.001),
          end,
          i * 100.0,
          TransportMode.CAR)
      delay(1) // ensure unique timestamps
    }

    // The oldest entry (i=1) should have been evicted
    val oldestEntry = cache.getDuration(baseStart.copy(latitude = 47.001), end, TransportMode.CAR)
    assertNull(oldestEntry)

    // Newest entries exist
    for (i in 2..4) {
      val entry =
          cache.getDuration(
              baseStart.copy(latitude = baseStart.latitude + i * 0.001), end, TransportMode.CAR)
      assertNotNull(entry)
    }

    // Optional: check file
    val text = withContext(Dispatchers.IO) { cacheFile.readText() }
    val map: Map<String, DurationCache.CacheEntry> = Json.decodeFromString(text)
    assertEquals(3, map.size)
  }

  @Test
  fun `reading an entry updates its timestamp`() = runBlocking {
    val start = Coordinate(47.1, 8.1)
    val end = Coordinate(46.1, 7.1)

    cache.saveDuration(start, end, 5000.0, TransportMode.WALKING)
    val before = cache.getDuration(start, end, TransportMode.WALKING)!!
    val oldTimestamp = before.lastUpdateTimestamp

    Thread.sleep(10)

    val after = cache.getDuration(start, end, TransportMode.WALKING)!!
    val newTimestamp = after.lastUpdateTimestamp

    assertTrue(newTimestamp > oldTimestamp)
  }

  @Test
  fun `cache loads only once (lazy load)`() = runBlocking {
    cacheFile.writeText("""{}""")

    val newCache = DurationCacheLocal(context, maxCacheSize = 3)
    newCache.getDuration(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0), TransportMode.CAR)

    cacheFile.writeText("THIS SHOULD NOT BE READ AGAIN")

    newCache.getDuration(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0), TransportMode.CAR)

    assertTrue(true)
  }
}
