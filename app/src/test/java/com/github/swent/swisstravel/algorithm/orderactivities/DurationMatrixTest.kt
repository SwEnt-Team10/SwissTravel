package com.github.swent.swisstravel.algorithm.orderactivities

import android.content.Context
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.api.matrix.v1.models.MatrixResponse
import com.mapbox.geojson.Point
import io.mockk.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Callback
import retrofit2.Response

class DurationMatrixTest {

  private lateinit var context: Context
  private lateinit var durationMatrix: DurationMatrix
  private val mockMapboxMatrix = mockk<MapboxMatrix>(relaxed = true)

  @Before
  fun setUp() {
    context = mockk(relaxed = true)
    every { context.getString(R.string.mapbox_access_token) } returns "MOCK_TOKEN"
    every { context.getString(R.string.app_name) } returns "SwissTravelTest"

    // Use subclass to override client builder
    durationMatrix =
        object : DurationMatrix(context) {
          override fun buildClient(points: List<Point>): MapboxMatrix {
            return mockMapboxMatrix
          }
        }
  }

  @Test
  fun `getDurations with valid coordinates succeeds`() {
    val coordinates =
        listOf(
            Coordinate(46.5197, 6.6323), // Lausanne
            Coordinate(46.2044, 6.1432) // Geneva
            )

    val mockResponse = mockk<MatrixResponse>()
    val expectedDurations = arrayOf(doubleArrayOf(0.0, 3600.0), doubleArrayOf(3700.0, 0.0))
    // Convert each DoubleArray -> Array<Double>
    val responseDurations = expectedDurations.map { it.toTypedArray() }

    every { mockResponse.durations() } returns responseDurations
    every { mockResponse.code() } returns "Ok"

    val callbackSlot = slot<Callback<MatrixResponse>>()
    every { mockMapboxMatrix.enqueueCall(capture(callbackSlot)) } answers {}

    var actualDurations: Array<DoubleArray>? = null
    durationMatrix.getDurations(coordinates) { actualDurations = it }

    // Trigger callback success
    callbackSlot.captured.onResponse(mockk(), Response.success(mockResponse))

    assertArrayEquals(expectedDurations, actualDurations)
  }

  @Test
  fun `getDurations with invalid coordinate count returns null`() {
    val coordinates = listOf(Coordinate(46.5197, 6.6323))

    var result: Array<DoubleArray>? = emptyArray()
    durationMatrix.getDurations(coordinates) { result = it }

    assertNull(result)
  }

  @Test
  fun `getDurations handles API failure`() {
    val coordinates = listOf(Coordinate(46.5197, 6.6323), Coordinate(46.2044, 6.1432))

    val callbackSlot = slot<Callback<MatrixResponse>>()
    every { mockMapboxMatrix.enqueueCall(capture(callbackSlot)) } answers {}

    var result: Array<DoubleArray>? = emptyArray()
    durationMatrix.getDurations(coordinates) { result = it }

    // Simulate failure
    callbackSlot.captured.onFailure(mockk(), Throwable("API Call Failed"))

    assertNull(result)
  }

  @Test
  fun `getDurations handles unsuccessful HTTP response`() {
    val coordinates = listOf(Coordinate(46.5197, 6.6323), Coordinate(46.2044, 6.1432))

    val callbackSlot = slot<Callback<MatrixResponse>>()
    every { mockMapboxMatrix.enqueueCall(capture(callbackSlot)) } answers {}

    var result: Array<DoubleArray>? = emptyArray()
    durationMatrix.getDurations(coordinates) { result = it }

    // Trigger error HTTP response
    callbackSlot.captured.onResponse(
        mockk(), Response.error(401, "Unauthorized".toResponseBody(null)))

    assertNull(result)
  }

  @Test
  fun `getDurations handles response with non-Ok code`() {
    val coordinates = listOf(Coordinate(46.5197, 6.6323), Coordinate(46.2044, 6.1432))
    val mockResponse = mockk<MatrixResponse>()
    every { mockResponse.code() } returns "NoRoute"

    val callbackSlot = slot<Callback<MatrixResponse>>()
    every { mockMapboxMatrix.enqueueCall(capture(callbackSlot)) } answers {}

    var result: Array<DoubleArray>? = emptyArray()
    durationMatrix.getDurations(coordinates) { result = it }

    // Trigger success HTTP but non-"Ok" Matrix code
    callbackSlot.captured.onResponse(mockk(), Response.success(mockResponse))

    assertNull(result)
  }
}
