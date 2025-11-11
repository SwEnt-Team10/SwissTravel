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
                .addQueryParameter("layer", "poi")
                .addQueryParameter("addressdetails", "1") // adds full address fields
                // .addQueryParameter("namedetails", "1")    // adds multilingual names
                .addQueryParameter("limit", "10") // controls how many results
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

  /**
   * Parses the JSON response body from the Nominatim API into a list of Location objects.
   *
   * @param body The JSON response body as a string.
   * @return A list of Location objects parsed from the response.
   */
  // done with the help of AI
  private fun parseBody(body: String): List<Location> {
    val jsonArray = JSONArray(body)
    return List(jsonArray.length()) { i ->
      val obj = jsonArray.getJSONObject(i)

      val address = obj.optJSONObject("address")
      val amenity = address?.optString("amenity") ?: obj.optString("name", "")
      val road = address?.optString("road").orEmpty()
      val houseNumber = address?.optString("house_number").orEmpty()
      val postcode = address?.optString("postcode").orEmpty()
      val city = address?.optString("city").orEmpty()
      val state = address?.optString("state").orEmpty()

      // Build custom name
      val nameParts = mutableListOf<String>()
      if (amenity.isNotBlank()) nameParts.add(amenity)
      if (road.isNotBlank() || houseNumber.isNotBlank()) {
        nameParts.add("$road $houseNumber".trim())
      }
      if (postcode.isNotBlank() || city.isNotBlank()) {
        nameParts.add("$postcode $city".trim())
      }
      if (state.isNotBlank()) nameParts.add(state)

      val name = nameParts.joinToString(", ")

      Location(
          coordinate = Coordinate(obj.getDouble("lat"), obj.getDouble("lon")),
          name =
              name.ifBlank {
                obj.getString("display_name")
              }) // If the address can't be built, use display_name
    }
  }
}
