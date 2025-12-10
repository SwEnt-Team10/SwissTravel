package com.github.swent.swisstravel.model.geocoding

import com.github.swent.swisstravel.model.map.GeoapifyLocationRepository
import com.github.swent.swisstravel.model.trip.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeoapifyLocationRepositoryTest {

  private lateinit var mockWebServer: MockWebServer
  private lateinit var client: OkHttpClient

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()
    client = OkHttpClient()
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  private fun createRepository(): GeoapifyLocationRepository {
    val baseUrl = mockWebServer.url("/v1/geocode/autocomplete").toString()
    return GeoapifyLocationRepository(
        client = client,
        ioDispatcher = Dispatchers.Unconfined, // ok for unit tests
        apiKey = "test_api_key",
        baseUrl = baseUrl)
  }

  @Test
  fun `search returns parsed locations on success`() = runTest {
    // given
    val jsonBody =
        """
            {
              "results": [
                {
                  "formatted": "Zurich, Switzerland",
                  "lat": 47.3769,
                  "lon": 8.5417
                },
                {
                  "formatted": "Bern, Switzerland",
                  "lat": 46.9480,
                  "lon": 7.4474
                }
              ]
            }
        """
            .trimIndent()

    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonBody))

    val repository = createRepository()

    // when
    val result: List<Location> = repository.search("zurich")

    // then
    assertEquals(2, result.size)

    val first = result[0]
    assertEquals("Zurich, Switzerland", first.name)
    assertEquals(47.3769, first.coordinate.latitude, 1e-6)
    assertEquals(8.5417, first.coordinate.longitude, 1e-6)

    val second = result[1]
    assertEquals("Bern, Switzerland", second.name)
    assertEquals(46.9480, second.coordinate.latitude, 1e-6)
    assertEquals(7.4474, second.coordinate.longitude, 1e-6)

    // also verify query params are correct
    val recorded = mockWebServer.takeRequest()
    val url = recorded.requestUrl!!
    assertEquals("test_api_key", url.queryParameter("apiKey"))
    assertEquals("zurich", url.queryParameter("text"))
    assertEquals("countrycode:ch", url.queryParameter("filter"))
    assertEquals("en", url.queryParameter("lang"))
    assertEquals("json", url.queryParameter("format"))
  }

  @Test
  fun `search returns empty list when response is not successful`() = runTest {
    // given
    mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

    val repository = createRepository()

    // when
    val result = repository.search("anything")

    // then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `search returns empty list when body has no results array`() = runTest {
    // given
    val jsonBody = """{ "foo": "bar" }"""
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonBody))

    val repository = createRepository()

    // when
    val result = repository.search("lausanne")

    // then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `search returns empty list on malformed json`() = runTest {
    // given
    val invalidJson = """{ "results": [ { "formatted": "Bad json" }""" // missing closing
    mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(invalidJson))

    val repository = createRepository()

    // when
    val result = repository.search("whatever")

    // then
    assertTrue(result.isEmpty())
  }
}
