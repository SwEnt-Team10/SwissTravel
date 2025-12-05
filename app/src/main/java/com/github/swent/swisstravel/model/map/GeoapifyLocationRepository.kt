package com.github.swent.swisstravel.model.map

import android.util.Log
import com.github.swent.swisstravel.BuildConfig
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import java.io.IOException
import kotlin.collections.emptyList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GeoapifyLocationRepository(
    private val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val apiKey: String = BuildConfig.GEOAPIFY_API_KEY,
    private val baseUrl: String = "https://api.geoapify.com/v1/geocode/autocomplete"
) : LocationRepository {

  /**
   * Searches for locations matching the given query.
   *
   * @param query The search query.
   * @return A list of matching locations.
   */
  override suspend fun search(query: String): List<Location> {
    return withContext(ioDispatcher) {
      val url =
          baseUrl
              .toHttpUrl()
              .newBuilder()
              .addQueryParameter("apiKey", apiKey)
              .addQueryParameter("text", query)
              .addQueryParameter("filter", "countrycode:ch")
              .addQueryParameter("lang", "en")
              .addQueryParameter("format", "json")
              .build()

      val request = Request.Builder().url(url).get().build()

      try {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected code $response")

          val body = response.body?.string() ?: return@withContext emptyList()
          return@withContext parseBody(body)
        }
      } catch (e: Exception) {
        Log.e("LocationRepository", "Error while searching for locations", e)
        return@withContext emptyList()
      }
    }
  }

  /**
   * Parses the JSON response from the Geoapify API and returns a list of locations.
   *
   * @param body The JSON response body.
   * @return A list of locations.
   */
  private fun parseBody(body: String): List<Location> {
    Log.d("LocationRepository", "Parsing body: $body")
    val json = JSONObject(body)
    val results = json.optJSONArray("results") ?: return emptyList()

    val locations = mutableListOf<Location>()
    for (i in 0 until results.length()) {
      val result = results.optJSONObject(i) ?: continue
      val formatted = result.optString("formatted", "")
      val lat = result.optDouble("lat")
      val lon = result.optDouble("lon")

      val location = Location(name = formatted, coordinate = Coordinate(lat, lon))

      locations += location
    }
    return locations
  }
}
