package com.github.swent.swisstravel.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

object FakeHttpClient {

  /**
   * A fake HTTP client that intercepts requests and provides predefined responses for testing
   * location search functionality.
   */
  // TODO : Modify this value once we have an API to mock
  private const val FAKE_BODY = """{"result":"ok"}"""

  private class APIInterceptor(val checkUrl: Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val url = request.url.toString()
      Log.d("MockInterceptor", "Intercepted URL: $url")
      if (checkUrl) {
        assertTrue("Request must use HTTPS", request.url.isHttps)
        assertEquals("json", request.url.queryParameter("format"))
      }
      // TO-DO : parse the code so that the response looks like the actual API response
      // Only fake responses for Nominatim requests (the following val was done with AI)
      // Fake responses
      val fakeJson =
          when {
            // Nominatim responses
            "nominatim" in url.lowercase() -> {
              val query = request.url.queryParameter("q")?.lowercase() ?: ""
              when {
                "cafe de paris" in query ->
                    """
                    [
                      {
                        "display_name": "Café de Paris, 26 Rue du Mont-Blanc, 1201 Genève, Genève, Suisse",
                        "lat": "46.2095",
                        "lon": "6.1432",
                        "address": {
                          "amenity": "Café de Paris",
                          "house_number": "26",
                          "road": "Rue du Mont-Blanc",
                          "postcode": "1201",
                          "city": "Genève",
                          "state": "Genève",
                          "country": "Suisse",
                          "country_code": "ch"
                        }
                      }
                    ]
                    """
                "epfl" in query ->
                    """
                    [
                      {
                        "display_name": "École Polytechnique Fédérale de Lausanne (EPFL), Route Cantonale, 1015 Lausanne, Vaud, Suisse",
                        "lat": "46.5191",
                        "lon": "6.5668",
                        "address": {
                          "amenity": "École Polytechnique Fédérale de Lausanne (EPFL)",
                          "road": "Route Cantonale",
                          "postcode": "1015",
                          "city": "Lausanne",
                          "state": "Vaud",
                          "country": "Suisse",
                          "country_code": "ch"
                        }
                      }
                    ]
                    """
                else -> "[]"
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

  fun getClient(checkUrl: Boolean = false): OkHttpClient =
      OkHttpClient.Builder().addInterceptor(APIInterceptor(checkUrl)).build()
}
