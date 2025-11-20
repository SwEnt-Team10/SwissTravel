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

  private fun mockJsonResponse(json: String, requestSlot: CapturingSlot<Request>? = null) {
    val responseBody = json.toResponseBody(null)

    if (requestSlot != null) {
      every { client.newCall(capture(requestSlot)) } returns call
    } else {
      every { client.newCall(any()) } returns call
    }

    every { call.execute() } answers
        {
          val dummyRequest = Request.Builder().url("https://example.com").build()
          Response.Builder()
              .request(dummyRequest)
              .protocol(Protocol.HTTP_1_1)
              .code(200)
              .message("OK")
              .body(responseBody)
              .build()
        }
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

  @Test
  fun airportQueryUsesFullModeWithLayerAddressPoi() = runBlocking {
    val jsonResponse = "[]"
    val requestSlot = slot<Request>()
    mockJsonResponse(jsonResponse, requestSlot)

    repository.search("geneve airport")

    val url = requestSlot.captured.url
    assertEquals("address,poi", url.queryParameter("layer"))
    assertEquals(null, url.queryParameter("featureType"))
  }

  @Test
  fun airportIsRankedAboveNearbyStreetWhenAirportKeywordPresent() = runBlocking {
    val jsonResponse =
        """
      [
        {
          "lat":"46.2381",
          "lon":"6.1089",
          "display_name":"Aéroport international de Genève, Route de Ferney, Le Grand-Saconnex, Genève, 1218, Schweiz/Suisse/Svizzera/Svizra",
          "name":"Aéroport international de Genève",
          "category":"aeroway",
          "type":"aerodrome",
          "importance":0.4,
          "address":{
            "road":"Route de Ferney",
            "city":"Le Grand-Saconnex",
            "postcode":"1218",
            "state":"Genève"
          }
        },
        {
          "lat":"46.21",
          "lon":"6.10",
          "display_name":"Route de Ferney, 1218, Genève, Schweiz/Suisse/Svizzera/Svizra",
          "name":"Route de Ferney",
          "category":"highway",
          "type":"secondary",
          "importance":0.9,
          "address":{
            "road":"Route de Ferney",
            "city":"Genève",
            "postcode":"1218",
            "state":"Genève"
          }
        }
      ]
      """
            .trimIndent()

    mockJsonResponse(jsonResponse)

    val results = repository.search("geneve airport")

    assertEquals("Aéroport international de Genève, Le Grand-Saconnex", results[0].name)
  }

  @Test
  fun cityAndStreetHaveCompactLabels() = runBlocking {
    val jsonResponse =
        """
      [
        {
          "lat":"46.5198",
          "lon":"6.6327",
          "display_name":"Lausanne, District de Lausanne, Vaud, Schweiz/Suisse/Svizzera/Svizra",
          "name":"Lausanne",
          "class":"place",
          "type":"city",
          "importance":0.7,
          "address":{
            "city":"Lausanne",
            "state":"Vaud"
          }
        },
        {
          "lat":"46.5300",
          "lon":"6.6100",
          "display_name":"Chemin de Gachet 1, Lausanne, Vaud, Schweiz/Suisse/Svizzera/Svizra",
          "name":"Chemin de Gachet 1",
          "class":"building",
          "type":"yes",
          "importance":0.3,
          "address":{
            "road":"Chemin de Gachet",
            "house_number":"1",
            "city":"Lausanne",
            "postcode":"1000"
          }
        }
      ]
      """
            .trimIndent()

    mockJsonResponse(jsonResponse)

    val results = repository.search("lausanne")

    assertEquals("Lausanne, Vaud", results[0].name)
    assertEquals("Chemin de Gachet 1, 1000 Lausanne", results[1].name)
  }
}
