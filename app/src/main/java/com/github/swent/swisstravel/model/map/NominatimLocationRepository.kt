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
import org.json.JSONObject

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
      val addressFields = extractAddressFields(address)

      val type = obj.optString("type")
      val classType = obj.optString("class").ifBlank { obj.optString("category") }
      val importance = obj.optDouble("importance", 0.0)

      val label =
          buildLabel(
              classType = classType,
              type = type,
              mainName = mainName,
              displayName = displayName,
              addressFields = addressFields)

      val score =
          computeScore(
              classType = classType,
              type = type,
              importance = importance,
              mainName = mainName,
              lowerQuery = lowerQuery,
              airportKeywords = airportKeywords)

      scored +=
          ScoredLocation(
              score = score, location = Location(coordinate = Coordinate(lat, lon), name = label))
    }

    return scored.sortedByDescending { it.score }.map { it.location }
  }

  /**
   * Private data class representing the fields of an address.
   *
   * @property houseNumber The house number.
   * @property road The road name.
   * @property postcode The postcode.
   * @property city The city name.
   * @property state The state name.
   * @property amenity The amenity name.
   * @property municipality The municipality name.
   */
  private data class AddressFields(
      val houseNumber: String,
      val road: String,
      val postcode: String,
      val city: String,
      val state: String,
      val amenity: String,
      val municipality: String
  )

  /**
   * Helper function to extract address fields from a JSONObject.
   *
   * @param address The JSONObject representing the address.
   */
  private fun extractAddressFields(address: JSONObject?): AddressFields {
    fun a(key: String): String = address?.optString(key)?.takeIf { it.isNotBlank() } ?: ""

    val city = a("city").ifBlank { a("town").ifBlank { a("village").ifBlank { a("hamlet") } } }

    return AddressFields(
        houseNumber = a("house_number"),
        road = a("road"),
        postcode = a("postcode"),
        city = city,
        state = a("state"),
        amenity = a("amenity"),
        municipality = a("municipality"))
  }

  /**
   * Helper function to build a label for a location.
   *
   * @param classType The class type of the location.
   * @param type The type of the location.
   * @param mainName The main name of the location.
   * @param displayName The display name of the location.
   * @param addressFields The address fields of the location.
   */
  private fun buildLabel(
      classType: String,
      type: String,
      mainName: String,
      displayName: String,
      addressFields: AddressFields
  ): String {
    return when {
      isCityLike(classType, type) ->
          buildCityLabel(addressFields.city, addressFields.state, displayName)
      isAirport(classType, type) ->
          buildAirportLabel(
              mainName = mainName,
              city = addressFields.city,
              municipality = addressFields.municipality,
              displayName = displayName)
      else ->
          buildGenericLabel(
              mainName = mainName, displayName = displayName, addressFields = addressFields)
    }
  }

  /**
   * Helper to test if a location is a city.
   *
   * @param classType The class type of the location.
   * @param type The type of the location.
   */
  private fun isCityLike(classType: String, type: String): Boolean {
    val cityTypes = setOf("city", "town", "village", "hamlet", "suburb")
    return classType == "place" && type in cityTypes
  }

  /**
   * Helper to test if a location is an airport.
   *
   * @param classType The class type of the location.
   * @param type The type of the location.
   */
  private fun isAirport(classType: String, type: String): Boolean {
    return classType == "aeroway" && type == "aerodrome"
  }

  /**
   * Helper function to build a city label.
   *
   * @param city The city name.
   * @param state The state name.
   * @param displayName The display name of the location.
   */
  private fun buildCityLabel(city: String, state: String, displayName: String): String {
    if (city.isBlank()) return displayName
    return if (state.isNotBlank()) "$city, $state" else city
  }

  /**
   * Helper function to build an airport label.
   *
   * @param mainName The main name of the location.
   * @param city The city name.
   * @param municipality The municipality name.
   * @param displayName The display name of the location.
   */
  private fun buildAirportLabel(
      mainName: String,
      city: String,
      municipality: String,
      displayName: String
  ): String {
    val area = city.ifBlank { municipality }

    val parts = listOf(mainName, area).filter { it.isNotBlank() }

    return parts.joinToString(", ").ifBlank { displayName }
  }

  /**
   * Helper function to build a generic label.
   *
   * @param mainName The main name of the location.
   * @param displayName The display name of the location.
   * @param addressFields The address fields of the location.
   */
  private fun buildGenericLabel(
      mainName: String,
      displayName: String,
      addressFields: AddressFields
  ): String {
    val streetPart =
        listOf(addressFields.road, addressFields.houseNumber)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

    val cityPart =
        listOf(addressFields.postcode, addressFields.city)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

    val primary =
        computePrimaryName(
            amenity = addressFields.amenity, mainName = mainName, road = addressFields.road)

    val parts = mutableListOf<String>()
    if (primary.isNotBlank()) parts += primary

    val streetDiffersFromPrimary =
        streetPart.isNotBlank() && !streetPart.equals(primary, ignoreCase = true)

    if (streetDiffersFromPrimary) parts += streetPart
    if (cityPart.isNotBlank()) parts += cityPart

    return parts.joinToString(", ").ifBlank { displayName }
  }

  /**
   * Helper function to compute the primary name for a location.
   *
   * @param amenity The amenity name.
   * @param mainName The main name of the location.
   * @param road The road name.
   */
  private fun computePrimaryName(amenity: String, mainName: String, road: String): String =
      when {
        amenity.isNotBlank() -> amenity
        mainName.isNotBlank() && !mainName.equals(road, ignoreCase = true) -> mainName
        else -> ""
      }

  /**
   * Helper function to compute the score for a location.
   *
   * @param classType The class type of the location.
   * @param type The type of the location.
   * @param importance The importance of the location.
   * @param mainName The main name of the location.
   * @param lowerQuery The lowercased query.
   * @param airportKeywords The airport keywords.
   */
  private fun computeScore(
      classType: String,
      type: String,
      importance: Double,
      mainName: String,
      lowerQuery: String,
      airportKeywords: List<String>
  ): Double {
    var score = importance * 10.0

    score += cityScoreBonus(classType, type)
    score += airportScoreBonus(classType, type, lowerQuery, airportKeywords)
    score += amenityScoreBonus(classType)
    score += textMatchBonus(mainName, lowerQuery)

    return score
  }

  /**
   * Helper function to compute the city score bonus for a location.
   *
   * @param classType The class type of the location.
   * @param type The type of the location.
   */
  private fun cityScoreBonus(classType: String, type: String): Double {
    return when {
      classType == "place" && type in setOf("city", "town") -> 60.0
      classType == "place" -> 40.0
      else -> 0.0
    }
  }

  /**
   * Helper function to compute the airport score bonus for a location.
   *
   * @param classType The class type of the location.
   * @param type The type of the location.
   * @param lowerQuery The lowercased query.
   * @param airportKeywords The airport keywords.
   */
  private fun airportScoreBonus(
      classType: String,
      type: String,
      lowerQuery: String,
      airportKeywords: List<String>
  ): Double {
    if (!isAirport(classType, type)) return 0.0
    var bonus = 30.0
    if (airportKeywords.any { lowerQuery.contains(it) }) {
      bonus += 60.0
    }
    return bonus
  }

  /**
   * Helper function to compute the amenity score bonus for a location.
   *
   * @param classType The class type of the location.
   */
  private fun amenityScoreBonus(classType: String): Double =
      if (classType == "amenity") 10.0 else 0.0

  /**
   * Helper function to compute the text match bonus for a location.
   *
   * @param mainName The main name of the location.
   * @param lowerQuery The lowercased query.
   */
  private fun textMatchBonus(mainName: String, lowerQuery: String): Double {
    val lowerName = mainName.lowercase()
    return when {
      lowerName.startsWith(lowerQuery) -> 20.0
      lowerName.contains(lowerQuery) -> 10.0
      else -> 0.0
    }
  }
}
