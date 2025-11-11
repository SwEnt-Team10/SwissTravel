package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.content.Context
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.activity.Activity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProgressiveRouteOptimizerE2ETest {

  private lateinit var cacheManager: DurationCacheManager
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

  @Before
  fun setup() {
    cacheManager = mockk<DurationCacheManager>(relaxed = true)
    matrixHybrid = mockk<DurationMatrixHybrid>(relaxed = true)

    // All cache reads return null
    coEvery { cacheManager.getDuration(any(), any(), any()) } returns null
    coEvery { cacheManager.saveDuration(any(), any(), any(), any()) } returns Unit

    // Matrix durations computed from haversine
    coEvery { matrixHybrid.fetchDurationsFromStart(any(), any(), any()) } answers
        {
          val startCoord = firstArg<Coordinate>()
          val ends = secondArg<List<Coordinate>>()
          val map = mutableMapOf<Pair<Coordinate, Coordinate>, Double?>()
          for (endCoord in ends) {
            val dist = startCoord.haversineDistanceTo(endCoord)
            map[Pair(startCoord, endCoord)] = dist * 1000.0
          }
          map
        }

    optimizer =
        ProgressiveRouteOptimizer(
            context = mockContext,
            cacheManager = cacheManager,
            matrixHybrid = matrixHybrid,
            k = 3,
            penaltyConfig =
                ProgressiveRouteOptimizer.PenaltyConfig(
                    zigzagMultiplier = 0.0,
                    activityDiffMultiplier = 0.0,
                    centerDistanceMultiplier = 0.0))
  }

  @Test
  fun `full swiss cities test`() = runBlocking {
    // Optional activities
    val activities =
        listOf(
            Activity(
                startDate = mockk(),
                endDate = mockk(),
                location = LSN,
                description = "Lunch",
                estimatedTime = 60),
            Activity(
                startDate = mockk(),
                endDate = mockk(),
                location = MTX,
                description = "Museum",
                estimatedTime = 90))

    val start = ZRH
    val end = GVA

    val result =
        optimizer.optimize(
            start = start,
            end = end,
            allLocations = allLocations,
            activities = activities,
            mode = TransportMode.CAR)

    // Print results for debugging
    println("Ordered Route: ${result.orderedLocations.map { it.name }}")
    println("Segment Durations: ${result.segmentDuration}")
    println("Total Duration: ${result.totalDuration}")

    // Expected order roughly matches previous precise algorithm
    val expectedOrder = listOf(ZRH, BSL, BRN, ITK, ZRT, MTX, LSN, GVA)
    assertEquals(expectedOrder.map { it.name }, result.orderedLocations.map { it.name })
  }
}
