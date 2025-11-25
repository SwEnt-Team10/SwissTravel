package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.content.Context
import com.github.swent.swisstravel.model.trainstimetable.SbbTimetable
import com.github.swent.swisstravel.model.trainstimetable.TrainTimetable
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TransportMode
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.api.matrix.v1.models.MatrixResponse
import com.mapbox.geojson.Point
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Done with the help of AI
class DurationMatrixHybridTest {

  private lateinit var context: Context
  private lateinit var durationMatrixHybrid: DurationMatrixHybrid

  @Before
  fun setup() {
    context = mockk()
    every { context.getString(any()) } returns "FAKE"
    // Spy so we can override private methods (AI)
    durationMatrixHybrid = spyk(DurationMatrixHybrid(context), recordPrivateCalls = true)
  }

  @Test
  fun `fetchDurationsFromStart returns empty map when ends is empty`() = runBlocking {
    val start = Location(Coordinate(1.0, 2.0), "loc1")
    val result = durationMatrixHybrid.fetchDurationsFromStart(start, emptyList(), TransportMode.CAR)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `fetchDurationsFromStart returns nulls when Mapbox call fails`() = runBlocking {
    val start = Location(Coordinate(48.0, 8.0), "loc1")
    val ends =
        listOf(Location(Coordinate(48.1, 8.1), "loc2"), Location(Coordinate(48.2, 8.2), "loc3"))

    val fakeMatrix = mockk<MapboxMatrix>()
    val fakeCall = mockk<Call<MatrixResponse>>()

    // Mock enqueueCall to immediately trigger onFailure
    every { fakeMatrix.enqueueCall(any()) } answers
        {
          val callback = firstArg<Callback<MatrixResponse>>()
          callback.onFailure(fakeCall, RuntimeException("Simulated network failure"))
        }

    // Override private buildClient to return our mock
    every { durationMatrixHybrid["buildClient"](any<List<Point>>(), any<TransportMode>()) } returns
        fakeMatrix

    val result = durationMatrixHybrid.fetchDurationsFromStart(start, ends, TransportMode.CAR)

    // Assert that all durations are null because the request "failed"
    assertEquals(ends.size, result.size)
    result.values.forEach { assertNull(it) }
  }

  @Test
  fun `fetchDurationsFromStart returns correct mapping for fake durations`() = runBlocking {
    val start = Location(Coordinate(1.0, 2.0), "loc1")
    val ends =
        listOf(Location(Coordinate(3.0, 4.0), "loc2"), Location(Coordinate(5.0, 6.0), "loc3"))

    val durations: MutableList<Array<Double>> =
        mutableListOf(
            arrayOf(0.0, 10.0, 20.0) // row 0: start -> end0, end1
            )

    val fakeBody = mockk<MatrixResponse>()
    every { fakeBody.code() } returns "Ok"
    every { fakeBody.durations() } returns durations

    val fakeMatrix = mockk<MapboxMatrix>()
    val fakeCall = mockk<Call<MatrixResponse>>()

    // Mock enqueueCall to return our fake response
    every { fakeMatrix.enqueueCall(any()) } answers
        {
          val callback = firstArg<Callback<MatrixResponse>>()
          callback.onResponse(fakeCall, Response.success(fakeBody))
        }

    // Override private buildClient to return our mock
    every { durationMatrixHybrid["buildClient"](any<List<Point>>(), any<TransportMode>()) } returns
        fakeMatrix

    val result = durationMatrixHybrid.fetchDurationsFromStart(start, ends, TransportMode.CAR)

    assertEquals(2, result.size)
    assertEquals(10.0, result[Pair(start.coordinate, ends[0].coordinate)])
    assertEquals(20.0, result[Pair(start.coordinate, ends[1].coordinate)])
  }

  @Test
  fun `fetchDurationsFromStart returns mocked durations for public transport`() = runBlocking {
    val start = Location(Coordinate(1.0, 2.0), "loc1")
    val ends =
        listOf(Location(Coordinate(3.0, 4.0), "loc2"), Location(Coordinate(5.0, 6.0), "loc3"))

    // Mock a train timetable
    val fakeTrainTimetable = mockk<SbbTimetable>()

    // Use coEvery for suspend functions
    coEvery { fakeTrainTimetable.getFastestRoute(any(), any()) } answers
        {
          val to = secondArg<Location>()
          when (to.name) {
            "loc2" -> 123
            "loc3" -> 456
            else -> 0
          }
        }

    // Create DurationMatrixHybrid with the mocked timetable
    val newMatrixHybrid = DurationMatrixHybrid(context, fakeTrainTimetable)

    val result = newMatrixHybrid.fetchDurationsFromStart(start, ends, TransportMode.TRAIN)

    assertEquals(2, result.size)
    assertEquals(123.0, result[Pair(start.coordinate, ends[0].coordinate)])
    assertEquals(456.0, result[Pair(start.coordinate, ends[1].coordinate)])
  }

  @Test
  fun `fetchDurationsFromStart handles getFastestRoute exception and does not call buildClient`() =
      runBlocking {
        // Arrange
        val fakeTrainTimetable = mockk<TrainTimetable>()

        // Force getFastestRoute to throw
        coEvery { fakeTrainTimetable.getFastestRoute(any(), any()) } throws RuntimeException("boom")

        val durationMatrix =
            spyk(DurationMatrixHybrid(context, fakeTrainTimetable), recordPrivateCalls = true)

        val start = Location(Coordinate(1.0, 1.0), "start")
        val ends =
            listOf(Location(Coordinate(2.0, 2.0), "end1"), Location(Coordinate(3.0, 3.0), "end2"))

        // Act
        val result = durationMatrix.fetchDurationsFromStart(start, ends, TransportMode.TRAIN)

        // Assert: all durations must be null
        assertEquals(2, result.size)
        result.values.forEach { assertNull(it) }

        // Assert: buildClient is *never* called for TRAIN mode
        verify(exactly = 0) {
          durationMatrix["buildClient"](any<List<Point>>(), any<TransportMode>())
        }

        // Assert: getFastestRoute was called twice (once per end)
        coVerify(exactly = 2) { fakeTrainTimetable.getFastestRoute(any(), any()) }
      }
}
