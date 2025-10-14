package com.github.swent.swisstravel.model.geocoding

import com.github.swent.swisstravel.model.map.NominatimLocationRepository
import com.github.swent.swisstravel.model.trip.Location
import io.mockk.*
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NominatimLocationRepositoryTest {

  private lateinit var client: OkHttpClient
  private lateinit var call: Call
  private lateinit var repository: NominatimLocationRepository

  @Before
  fun setUp() {
    client = mockk()
    call = mockk()
    repository = NominatimLocationRepository(client)
  }

  @Test
  fun searchReturnsParsedLocations() = runBlocking {
    val jsonResponse =
        """
            [
                {"lat":46.9481,"lon":7.4474,"display_name":"Bern, Switzerland"},
                {"lat":47.3769,"lon":8.5417,"display_name":"Zurich, Switzerland"}
            ]
        """
            .trimIndent()

    val request = mockk<Request>()
    val responseBody = jsonResponse.toResponseBody(null)
    val response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

    every { client.newCall(any()).execute() } returns response

    val results: List<Location> = repository.search("Switzerland")

    assertEquals(2, results.size)
    assertEquals("Bern, Switzerland", results[0].name)
    assertEquals(46.9481, results[0].coordinate.latitude, 0.0001)
    assertEquals(7.4474, results[0].coordinate.longitude, 0.0001)
    assertEquals("Zurich, Switzerland", results[1].name)
  }
}
