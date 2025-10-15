package com.github.swent.swisstravel.model.map

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Implementation of [LocationRepository] using the Nominatim API. Note: Nominatim has usage
 * policies that must be followed, including setting a proper User-Agent. See
 * https://operations.osmfoundation.org/policies/nominatim/
 *
 * @param client The OkHttpClient to use for network requests.
 * @param baseUrl The base URL for the Nominatim API. Default is
 *   "https://nominatim.openstreetmap.org".
 */
class NominatimLocationRepository(
    private val client: OkHttpClient,
    private val baseUrl: String = "https://nominatim.openstreetmap.org"
) : LocationRepository {

  override suspend fun search(query: String): List<Location> =
      withContext(Dispatchers.IO) {
        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host(baseUrl.toHttpUrl().host)
                .addPathSegment("search")
                .addQueryParameter("q", query)
                .addQueryParameter("format", "json")
                .addQueryParameter("countrycodes", "ch")
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "SwissTravel/1.0 (swisstravel.epfl@proton.me)")
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected code $response")
          val body = response.body?.string() ?: return@withContext emptyList()
          parseBody(body)
        }
      }

  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)
    return List(jsonArray.length()) { i ->
      val obj = jsonArray.getJSONObject(i)
      Location(
          Coordinate(obj.getDouble("lat"), obj.getDouble("lon")), obj.getString("display_name"))
    }
  }
}
