package com.github.swent.swisstravel.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue

object FakeHttpClient {

  /**
   * A fake HTTP client that intercepts requests and provides predefined responses for testing
   * location search functionality (Geoapify and MySwitzerland).
   */
  private const val FAKE_BODY = """{"results": []}"""

  private class APIInterceptor(val checkUrl: Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val url = request.url.toString()
      Log.d("MockInterceptor", "Intercepted URL: $url")

      if (checkUrl) {
        assertTrue("Request must use HTTPS", request.url.isHttps)
      }

      val fakeJson =
          when {
            // Geoapify responses (replacing Nominatim)
            "api.geoapify.com" in url.lowercase() -> {
              // Geoapify uses 'text' parameter for the query
              val query = request.url.queryParameter("text")?.lowercase() ?: ""
              when {
                "cafe de paris" in query ->
                    """
                    {
                      "results": [
                        {
                          "formatted": "Café de Paris, Rue du Mont-Blanc 26, 1201 Genève",
                          "lat": 46.2095,
                          "lon": 6.1432
                        }
                      ]
                    }
                    """
                "epfl" in query ->
                    """
                    {
                      "results": [
                        {
                          "formatted": "École Polytechnique Fédérale de Lausanne (EPFL), Route Cantonale, 1015 Lausanne",
                          "lat": 46.5191,
                          "lon": 6.5668
                        }
                      ]
                    }
                    """
                else -> """{"results": []}"""
              }
            }

            // MySwitzerland destinations
            "myswitzerland.io/v1/" in url.lowercase() -> {
              val query = request.url.queryParameter("query")?.lowercase() ?: ""
              when {
                "zermatt" in query ->
                    """
                    {
                      "data": [
                        {
                          "name": "Zermatt",
                          "abstract": "Famous mountain resort town in Switzerland",
                          "geo": {"latitude": 46.0207, "longitude": 7.7491},
                          "image": [{"url": "https://example.com/zermatt1.jpg"}],
                          "photo": "https://example.com/zermatt.jpg",
                          "classification": [{"name": "neededtime", "values": [{"name": "4to8hoursfullday"}]}]
                        }
                      ]
                    }
                    """
                else -> """{"data": []}"""
              }
            }

            // MySwitzerland attractions (Activity Search - Attractions) Made by AI
            "myswitzerland.io/v1/attractions" in url.lowercase() -> {
              val geoDist = request.url.queryParameter("geo.dist") ?: ""
              // Simple matching based on coordinates found in the E2E test
              when {
                "46.2095,6.1432" in geoDist -> // Geneva (Café de Paris)
                """
                        {
                          "data": [
                            {
                              "name": "Geneva Lake Tour",
                              "abstract": "A beautiful tour of Lake Geneva.",
                              "geo": {"latitude": 46.2100, "longitude": 6.1500},
                              "image": [{"url": "https://example.com/geneva.jpg"}],
                              "photo": "https://example.com/geneva.jpg",
                              "classification": [{"name": "neededtime", "values": [{"name": "2to4hourshalfday"}]}]
                            }
                          ]
                        }
                        """
                "46.5191,6.5668" in geoDist -> // Lausanne (EPFL)
                """
                        {
                          "data": [
                            {
                              "name": "Rolex Learning Center",
                              "abstract": "Iconic building at EPFL.",
                              "geo": {"latitude": 46.5180, "longitude": 6.5660},
                              "image": [{"url": "https://example.com/rolex.jpg"}],
                              "photo": "https://example.com/rolex.jpg",
                              "classification": [{"name": "neededtime", "values": [{"name": "between12hours"}]}]
                            }
                          ]
                        }
                        """
                "46.0207,7.7491" in geoDist -> // Zermatt
                """
                        {
                          "data": [
                            {
                              "name": "Matterhorn Viewpoint",
                              "abstract": "Best view of the Matterhorn.",
                              "geo": {"latitude": 46.0100, "longitude": 7.7500},
                              "image": [{"url": "https://example.com/matterhorn.jpg"}],
                              "photo": "https://example.com/matterhorn.jpg",
                              "classification": [{"name": "neededtime", "values": [{"name": "4to8hoursfullday"}]}]
                            }
                          ]
                        }
                        """
                else -> // Fallback for other locations
                """
                        {
                          "data": [
                            {
                              "name": "Generic Swiss Activity",
                              "abstract": "A nice activity in Switzerland.",
                              "geo": {"latitude": 46.8, "longitude": 8.2},
                              "image": [{"url": "https://example.com/swiss.jpg"}],
                              "photo": "https://example.com/swiss.jpg",
                              "classification": [{"name": "neededtime", "values": [{"name": "2to4hourshalfday"}]}]
                            }
                          ]
                        }
                        """
              }
            }

            // Mapbox Matrix API (Route Optimization) Made by AI
            "mapbox" in url.lowercase() && "matrix" in url.lowercase() -> {
              // Extract coordinates count from URL to generate a valid matrix
              // URL structure: .../driving/lon,lat;lon,lat;lon,lat?...
              val coordsPart = url.substringAfter("driving/").substringBefore("?")
              val count = coordsPart.split(";").size

              // Create a dummy matrix where travel time is 3600s (1h) between all points
              // and 0s from a point to itself.
              val matrix = List(count) { i -> List(count) { j -> if (i == j) 0.0 else 3600.0 } }

              // Format as JSON [[0, 3600], [3600, 0]]
              val matrixString =
                  matrix
                      .map { row -> row.joinToString(separator = ",", prefix = "[", postfix = "]") }
                      .joinToString(separator = ",", prefix = "[", postfix = "]")

              """{"code":"Ok", "durations":$matrixString}"""
            }

            // OJP/SBB API (Train Timetable) Made by AI
            "ojp20" in url.lowercase() || "opentransportdata" in url.lowercase() -> {
              // Return a fake OJP XML response with a dummy trip
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <OJP xmlns="http://www.vdv.de/ojp" version="2.0">
                    <OJPResponse>
                        <ServiceDelivery>
                            <ResponseTimestamp>2023-10-27T10:00:00Z</ResponseTimestamp>
                            <ProducerRef>OJPStub</ProducerRef>
                            <OJPTripDelivery>
                                <ResponseTimestamp>2023-10-27T10:00:00Z</ResponseTimestamp>
                                <TripResult>
                                    <Id>TRIP_1</Id>
                                    <Trip>
                                        <TripId>1</TripId>
                                        <Duration>PT1H</Duration>
                                        <StartTime>2023-10-27T12:00:00Z</StartTime>
                                        <EndTime>2023-10-27T13:00:00Z</EndTime>
                                        <Transfers>0</Transfers>
                                        <Distance>10000</Distance>
                                        <TripLeg>
                                            <LegId>1</LegId>
                                            <TimedLeg>
                                                <LegBoard>
                                                    <StopPointName><Text>Start Station</Text></StopPointName>
                                                    <ServiceDeparture><TimetabledTime>2023-10-27T12:00:00Z</TimetabledTime></ServiceDeparture>
                                                </LegBoard>
                                                <LegAlight>
                                                    <StopPointName><Text>End Station</Text></StopPointName>
                                                    <ServiceArrival><TimetabledTime>2023-10-27T13:00:00Z</TimetabledTime></ServiceArrival>
                                                </LegAlight>
                                                <Service>
                                                    <Mode><PtMode>rail</PtMode></Mode>
                                                    <PublishedLineName><Text>IC 1</Text></PublishedLineName>
                                                </Service>
                                            </TimedLeg>
                                        </TripLeg>
                                    </Trip>
                                </TripResult>
                            </OJPTripDelivery>
                        </ServiceDelivery>
                    </OJPResponse>
                </OJP>
                """
            }

            // Other APIs can get a generic response
            else -> FAKE_BODY
          }
      return Response.Builder() // you should change this code to adapt it to the APIs
          .code(200)
          .message("OK")
          .request(request)
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .body(fakeJson.toResponseBody("application/json".toMediaTypeOrNull()))
          .build()
    }
  }

  /**
   * Getter for the client.
   *
   * @param checkUrl Whether to check that the URL is HTTPS.
   * @return The http client.
   */
  fun getClient(checkUrl: Boolean = false): OkHttpClient =
      OkHttpClient.Builder().addInterceptor(APIInterceptor(checkUrl)).build()
}
