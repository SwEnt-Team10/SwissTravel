package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.content.Context
import com.github.swent.swisstravel.model.trip.Coordinate
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
    val start = Coordinate(1.0, 2.0)
    val result = durationMatrixHybrid.fetchDurationsFromStart(start, emptyList(), TransportMode.CAR)
    assertTrue(result.isEmpty())
  }

  @Test
  fun `fetchDurationsFromStart returns nulls when Mapbox call fails`() = runBlocking {
    val start = Coordinate(1.0, 2.0)
    val ends = listOf(Coordinate(3.0, 4.0), Coordinate(5.0, 6.0))

    val fakeMatrix = mockk<MapboxMatrix>()
    val fakeCall = mockk<Call<MatrixResponse>>()

    // Mock enqueueCall to simulate failure
    every { fakeMatrix.enqueueCall(any()) } answers
        {
          val callback = firstArg<Callback<MatrixResponse>>()
          callback.onFailure(fakeCall, RuntimeException("Simulated failure"))
        }

    // Override private buildClient to return our mock matrix
    every { durationMatrixHybrid["buildClient"](any<List<Point>>(), any<TransportMode>()) } returns
        fakeMatrix

    val result = durationMatrixHybrid.fetchDurationsFromStart(start, ends, TransportMode.CAR)

    // All values should be null because the call "failed"
    assertEquals(2, result.size)
    result.values.forEach { assertNull(it) }
  }

  @Test
  fun `fetchDurationsFromStart returns correct mapping for fake durations`() = runBlocking {
    val start = Coordinate(1.0, 2.0)
    val ends = listOf(Coordinate(3.0, 4.0), Coordinate(5.0, 6.0))

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

    assertEquals(mapOf(Pair(start, ends[0]) to 10.0, Pair(start, ends[1]) to 20.0), result)
  }

  @Test
  fun `fetchDurationsFromStart triggers onFailure callback`() = runBlocking {
    val start = Coordinate(48.0, 8.0)
    val ends = listOf(Coordinate(48.1, 8.1), Coordinate(48.2, 8.2))

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
    assertEquals(2, result.size)
    result.values.forEach { assertNull(it) }
  }
}
