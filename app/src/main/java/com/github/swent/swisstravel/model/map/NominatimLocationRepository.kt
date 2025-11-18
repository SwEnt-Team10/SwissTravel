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
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val lower = trimmed.lowercase()
        val hasDigit = lower.any { it.isDigit() }

        val airportKeywords = listOf("airport", "aéroport", "flughafen", "airp", "aero")
        val looksLikeAirport = airportKeywords.any { lower.contains(it) }

        // City mode
        val cityMode = !hasDigit && trimmed.length <= 3 && !looksLikeAirport

        val urlBuilder =
            HttpUrl.Builder()
                .scheme("https")
                .host(baseUrl.toHttpUrl().host)
                .addPathSegment("search")
                .addQueryParameter("q", trimmed)
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("countrycodes", "ch")
                .addQueryParameter("addressdetails", "1")
                .addQueryParameter("limit", "10")

        if (cityMode) {
          urlBuilder.addQueryParameter("featureType", "settlement")
        } else {
          urlBuilder.addQueryParameter("layer", "address,poi")
        }

        val url = urlBuilder.build()

        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "SwissTravel/1.0 (swisstravel.epfl@proton.me)")
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) throw IOException("Unexpected code $response")
          val body = response.body?.string() ?: return@withContext emptyList()
          parseBody(body, trimmed)
        }
      }

  /**
   * Parses the JSON response body from the Nominatim API into a list of Location objects. It uses a
   * custom ranking system to determine the relevance of each location.
   *
   * @param body The JSON response body as a string.
   * @param query The original query used for the search.
   * @return A list of Location objects parsed from the response.
   */
  // done with the help of AI
  private fun parseBody(body: String, query: String): List<Location> {
    val jsonArray = JSONArray(body)
    if (jsonArray.length() == 0) return emptyList()

    val lowerQuery = query.lowercase()
    val airportKeywords = listOf("airport", "aéroport", "flughafen", "airp", "aero")

    data class ScoredLocation(val score: Double, val location: Location)

    val scored = mutableListOf<ScoredLocation>()

    for (i in 0 until jsonArray.length()) {
      val obj = jsonArray.getJSONObject(i)

      val lat = obj.getDouble("lat")
      val lon = obj.getDouble("lon")

      val displayName = obj.optString("display_name")
      val mainName = obj.optString("name", displayName)

      val address = obj.optJSONObject("address")

      fun a(key: String): String = address?.optString(key)?.takeIf { it.isNotBlank() } ?: ""

      val houseNumber = a("house_number")
      val road = a("road")
      val postcode = a("postcode")
      val city = a("city").ifBlank { a("town").ifBlank { a("village").ifBlank { a("hamlet") } } }
      val state = a("state")
      val amenity = a("amenity")

      val type = obj.optString("type")
      val classType = obj.optString("class").ifBlank { obj.optString("category") }
      val importance = obj.optDouble("importance", 0.0)

      // ---------- label ----------
      val label: String =
          when {
            // City / town / village → "Lausanne, Vaud"
            classType == "place" &&
                type in setOf("city", "town", "village", "hamlet", "suburb") -> {
              if (state.isNotBlank()) "$city, $state" else city.ifBlank { displayName }
            }

            // Airport → "Aéroport international de Genève, Le Grand-Saconnex"
            classType == "aeroway" && type == "aerodrome" -> {
              val municipality = city.ifBlank { a("municipality") }
              listOf(mainName, municipality)
                  .filter { it.isNotBlank() }
                  .joinToString(", ")
                  .ifBlank { displayName }
            }

            // Other POIs / addresses → keep amenity/name + compact address
            else -> {
              val streetPart =
                  listOf(road, houseNumber).filter { it.isNotBlank() }.joinToString(" ").trim()

              val cityPart =
                  listOf(postcode, city).filter { it.isNotBlank() }.joinToString(" ").trim()

              val primary =
                  when {
                    amenity.isNotBlank() -> amenity
                    mainName.isNotBlank() && !mainName.equals(road, ignoreCase = true) -> mainName
                    else -> ""
                  }

              val parts = mutableListOf<String>()
              if (primary.isNotBlank()) parts += primary

              // 🔴 avoid duplicating when streetPart == primary (Chemin de Gachet 1 case)
              val streetDiffersFromPrimary =
                  streetPart.isNotBlank() && !streetPart.equals(primary, ignoreCase = true)

              if (streetDiffersFromPrimary) parts += streetPart
              if (cityPart.isNotBlank()) parts += cityPart

              parts.joinToString(", ").ifBlank { displayName }
            }
          }

      // ---------- scoring ----------
      var score = importance * 10.0

      if (classType == "place" && type in setOf("city", "town")) {
        score += 60.0
      } else if (classType == "place") {
        score += 40.0
      }

      if (classType == "aeroway" && type == "aerodrome") {
        score += 30.0
        if (airportKeywords.any { lowerQuery.contains(it) }) {
          score += 60.0
        }
      }

      if (classType == "amenity") score += 10.0

      val lowerName = mainName.lowercase()
      if (lowerName.startsWith(lowerQuery)) {
        score += 20.0
      } else if (lowerName.contains(lowerQuery)) {
        score += 10.0
      }

      scored +=
          ScoredLocation(
              score = score, location = Location(coordinate = Coordinate(lat, lon), name = label))
    }

    return scored.sortedByDescending { it.score }.map { it.location }
  }
}
