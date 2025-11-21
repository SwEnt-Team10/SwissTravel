package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.content.Context
import com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.activity.Activity
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Done with the help of AI
class ProgressiveRouteOptimizerLocalTest {

  private lateinit var cacheManager: DurationCacheLocal
  private lateinit var matrixHybrid: DurationMatrixHybrid
  private lateinit var optimizer: ProgressiveRouteOptimizer
  private val mockContext = mockk<Context>(relaxed = true)

  // Swiss cities
  private val GVA = Location(Coordinate(46.2, 6.15), "Genève")
  private val LSN = Location(Coordinate(46.521111, 6.631111), "Lausanne")
  private val MTX = Location(Coordinate(46.433333, 6.916667), "Montreux")
  private val ITK = Location(Coordinate(46.683333, 7.85), "Interlaken")
  private val BSL = Location(Coordinate(47.566944, 7.583056), "Bâle")
  private val ZRH = Location(Coordinate(47.377778, 8.541111), "Zurich")
  private val BRN = Location(Coordinate(46.949167, 7.447222), "Berne")
  private val ZRT = Location(Coordinate(46.02, 7.746944), "Zermatt")

  private val allLocations = listOf(GVA, LSN, MTX, ITK, BSL, ZRH, BRN, ZRT)

  // --- Mapbox JSON matrices ---
  // These are true responses generated using the playground, it can be reused if needed
  private val matrix1 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[8.541518,47.37779]},
        {"location":[7.583029,47.566941]},
        {"location":[7.447222,46.949168]},
        {"location":[6.630845,46.520984]},
        {"location":[6.916694,46.433298]},
        {"location":[7.850478,46.68315]}
      ],
      "durations":[
        [0,5349.8,6552.9,10342,9542,6644.1]
      ],
      "sources":[{"location":[8.541518,47.37779]}]
    }"""
  private val matrix2 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[7.583029,47.566941]},
        {"location":[7.447222,46.949168]},
        {"location":[6.630845,46.520984]},
        {"location":[6.916694,46.433298]},
        {"location":[7.850478,46.68315]},
        {"location":[7.751462,46.026536]}
      ],
      "durations":[
        [0,5590.3,9379.4,8579.4,7399.8,13477.2]
      ],
      "sources":[{"location":[7.583029,47.566941]}]
    }"""
  private val matrix3 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[7.447222,46.949168]},
        {"location":[6.630845,46.520984]},
        {"location":[6.916694,46.433298]},
        {"location":[7.850478,46.68315]},
        {"location":[7.751462,46.026536]}
      ],
      "durations":[
        [0,4989.4,4189.4,3216,9293.4]
      ],
      "sources":[{"location":[7.447222,46.949168]}]
    }"""
  private val matrix4 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[7.850478,46.68315]},
        {"location":[6.916694,46.433298]},
        {"location":[7.751462,46.026536]},
        {"location":[6.630845,46.520984]}
      ],
      "durations":[
        [0,6379.1,8381.7,7179.1]
      ],
      "sources":[{"location":[7.850478,46.68315]}]
    }"""
  private val matrix5 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[7.751462,46.026536]},
        {"location":[6.630845,46.520984]},
        {"location":[6.916694,46.433298]}
      ],
      "durations":[
        [0,8779.7,7453.9]
      ],
      "sources":[{"location":[7.751462,46.026536]}]
    }"""
  private val matrix6 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[6.916694,46.433298]},
        {"location":[6.630845,46.520984]}
      ],
      "durations":[
        [0,2463.7]
      ],
      "sources":[{"location":[6.916694,46.433298]}]
    }"""
  private val matrix7 =
      """{
      "code": "Ok",
      "destinations": [
        {"location":[6.630845,46.520984]},
        {"location":[6.149249,46.199901]}
      ],
      "durations":[
        [0,4335.7]
      ],
      "sources":[{"location":[6.630845,46.520984]}]
    }"""

  private lateinit var mapboxQueue:
      MutableList<(Coordinate, List<Coordinate>) -> Map<Pair<Coordinate, Coordinate>, Double?>>

  @Before
  fun setup() {
    cacheManager = mockk(relaxed = true)
    matrixHybrid = mockk(relaxed = true)

    coEvery { cacheManager.getDuration(any(), any(), any()) } returns null
    coEvery { cacheManager.saveDuration(any(), any(), any(), any()) } returns Unit

    // Prepare queue of real Mapbox matrices
    mapboxQueue = mutableListOf()
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix1) }
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix2) }
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix3) }
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix4) }
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix5) }
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix6) }
    mapboxQueue += { start, ends -> parseMatrixJson(start, ends, matrix7) }

    // mock fetchDurationsFromStart to use the next matrix in order and not mapbox
    coEvery { matrixHybrid.fetchDurationsFromStart(any(), any(), any()) } answers
        {
          val start = firstArg<Coordinate>()
          val ends = secondArg<List<Coordinate>>()
          mapboxQueue.removeAt(0).invoke(start, ends)
        }

    optimizer =
        ProgressiveRouteOptimizer(
            cacheManager = cacheManager,
            matrixHybrid = matrixHybrid,
            k = 5,
            penaltyConfig =
                ProgressiveRouteOptimizer.PenaltyConfig(
                    zigzagMultiplier = 0.0,
                    activityDiffMultiplier = 0.0,
                    centerDistanceMultiplier = 0.0))
  }

  // --- Helper to parse matrix JSON into Map<Pair<Coordinate,Coordinate>,Double?> ---
  private fun parseMatrixJson(
      start: Coordinate,
      ends: List<Coordinate>,
      json: String
  ): Map<Pair<Coordinate, Coordinate>, Double?> {
    val obj = JSONObject(json)
    val durationsArray = obj.getJSONArray("durations").getJSONArray(0)
    val map = mutableMapOf<Pair<Coordinate, Coordinate>, Double?>()
    for (i in ends.indices) {
      // Some matrices have fewer columns than ends; guard with optDouble
      val value = durationsArray.optDouble(i + 1, Double.NaN)
      map[start to ends[i]] = if (value.isNaN()) null else value
    }
    return map
  }

  @Test
  fun `full swiss cities with real cache and mapbox matrices`() = runBlocking {
    // Create temporary real cache
    val tempFile = File.createTempFile("duration_cache_test", ".json")
    tempFile.deleteOnExit()
    val realCache = DurationCacheLocal(mockContext, cacheFile = tempFile)

    val optimizerWithCacheAndMatrices =
        ProgressiveRouteOptimizer(
            cacheManager = realCache,
            matrixHybrid = matrixHybrid,
            k = 5,
            penaltyConfig =
                ProgressiveRouteOptimizer.PenaltyConfig(
                    zigzagMultiplier = 0.0,
                    activityDiffMultiplier = 0.0,
                    centerDistanceMultiplier = 0.0))

    val activities =
        listOf(
            Activity(
                startDate = mockk(),
                endDate = mockk(),
                location = LSN,
                description = "Lunch",
                estimatedTime = 60,
                imageUrls = emptyList()),
            Activity(
                startDate = mockk(),
                endDate = mockk(),
                location = MTX,
                description = "Museum",
                estimatedTime = 90,
                imageUrls = emptyList()))

    val start = ZRH
    val end = GVA

    val result =
        optimizerWithCacheAndMatrices.optimize(
            start = start,
            end = end,
            allLocations = allLocations,
            activities = activities,
            mode = TransportMode.CAR)

    println("Ordered Route: ${result.orderedLocations.map { it.name }}")
    println("Segment Durations: ${result.segmentDuration}")
    println("Total Duration: ${result.totalDuration}")
    println("Cache file exists: ${tempFile.exists()}")
    assert(tempFile.exists())

    val expectedOrder = listOf(ZRH, BSL, BRN, ITK, ZRT, MTX, LSN, GVA)
    assertEquals(expectedOrder.map { it.name }, result.orderedLocations.map { it.name })

    tempFile.delete()
    assert(!tempFile.exists())
  }
}
