package com.github.swent.swisstravel.model.trip.activity

import android.util.Log
import com.github.swent.swisstravel.BuildConfig
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.toSwissTourismFacet
import com.github.swent.swisstravel.model.user.toSwissTourismFacetFilter
import com.google.firebase.Timestamp
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.emptyList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

const val DESCRIPTION_FALLBACK = "No description"
// Frequency to which activities are shuffled to introduce randomness
const val ACTIVITY_SHUFFLE = 0.5f
// How many activities we should pull from the API to make activity selection more random
const val EXTRA_RANDOM_ACTIVITIES = 0.75f

/**
 * Implementation of a repository for activities. Uses the Swiss Tourism API.
 *
 * @param ioDispatcher The CoroutineDispatcher for executing network requests. Defaults to
 *   Dispatchers.IO.
 */
class ActivityRepositoryMySwitzerland(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ActivityRepository {
  private val blacklistedActivityNames = setOf<String>()
  private val API_KEY = BuildConfig.MYSWITZERLAND_API_KEY
  private val baseHttpUrl: HttpUrl =
      urlBuilder("https://opendata.myswitzerland.io/v1/attractions/", top = true)
  private val destinationHttpUrl: HttpUrl =
      urlBuilder("https://opendata.myswitzerland.io/v1/destinations/")

  /**
   * Builds a URL with the given parameters.
   *
   * @param url The URL to build.
   * @param language The language to use.
   * @param top Whether to get the top activities.
   * @return The built URL.
   */
  private fun urlBuilder(url: String, language: String = "en", top: Boolean = false): HttpUrl {
    val builder =
        url.toHttpUrl()
            .newBuilder()
            .addQueryParameter("lang", language)
            .addQueryParameter("striphtml", "true")
            .addQueryParameter("expand", "true")

    if (top) {
      builder.addQueryParameter("top", "true")
    }

    return builder.build()
  }

  private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
  }

  /**
   * Fetch activities from the given URL. Does a request to the Swiss Tourism API and parses the
   * response.
   *
   * @param url The URL to fetch activities from.
   * @return A list of activities.
   */
  private suspend fun fetchActivitiesFromUrl(url: HttpUrl): List<Activity> {
    return withContext(ioDispatcher) {
      val request =
          Request.Builder()
              .url(url)
              .header("accept", "application/json")
              .header("x-api-key", API_KEY)
              .build()

      try {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IOException("Unexpected HTTP code ${response.code}")
          }

          val body = response.body?.string()
          if (body != null) {
            parseActivitiesFromJson(body)
          } else {
            emptyList()
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
      }
    }
  }

  /**
   * Parse the JSON response from the Swiss Tourism API.
   *
   * @param json The JSON response.
   * @return A list of activities.
   */
  private fun parseActivitiesFromJson(json: String): List<Activity> {
    val root = JSONObject(json)
    val dataArray: JSONArray = root.optJSONArray("data") ?: return emptyList()

    return (0 until dataArray.length())
        .mapNotNull { i -> dataArray.optJSONObject(i) }
        .mapNotNull { jsonObject -> parseSingleActivity(jsonObject) }
  }

  /**
   * Parses a single JSONObject into an Activity.
   *
   * @param item The JSONObject representing one activity.
   * @return An [Activity] object or null if parsing fails.
   */
  private fun parseSingleActivity(item: JSONObject): Activity? {
    val geo = item.optJSONObject("geo") ?: return null
    val lat = geo.optDouble("latitude", Double.NaN)
    val lon = geo.optDouble("longitude", Double.NaN)

    if (lat.isNaN() || lon.isNaN()) return null

    val name = item.optString("name", "Unknown Activity")
    val description = item.optString("abstract", DESCRIPTION_FALLBACK)

    val coordinate = Coordinate(lat, lon)
    val photo = item.optString("photo")
    val location = Location(coordinate, name, photo)

    val imageUrls = parseImageUrls(item.optJSONArray("image"))
    val estimatedTime = parseEstimatedTime(item.optJSONArray("classification"))

    return Activity(
        Timestamp.now(), Timestamp.now(), location, description, imageUrls, estimatedTime)
  }

  /**
   * Extracts image URLs from a JSON array.
   *
   * @param imageArray The JSONArray containing image objects.
   * @return A list of image URL strings.
   */
  private fun parseImageUrls(imageArray: JSONArray?): List<String> {
    if (imageArray == null) return emptyList()
    return (0 until imageArray.length())
        .mapNotNull { j -> imageArray.optJSONObject(j)?.optString("url") }
        .filter { it.isNotBlank() }
  }

  /**
   * Parses the estimated time from the 'classification' JSON array.
   *
   * @param classificationArray The JSONArray for classifications.
   * @return The estimated time in seconds.
   */
  private fun parseEstimatedTime(classificationArray: JSONArray?): Int {
    if (classificationArray == null) return 0

    for (k in 0 until classificationArray.length()) {
      val obj = classificationArray.optJSONObject(k)
      if (obj?.optString("name") == "neededtime") {
        val values = obj.optJSONArray("values")
        val timeName = values?.optJSONObject(0)?.optString("name") ?: ""
        return mapToTime(timeName)
      }
    }
    return 0
  }

  /**
   * Maps the given time string from the SwissTourism API to a time in seconds.
   *
   * @param time The time string to map.
   * @return The time in seconds.
   */
  private fun mapToTime(time: String): Int {
    return when (time) {
      "2to4hourshalfday" -> 3600 * 4
      "4to8hoursfullday" -> 3600 * 8
      "between12hours" -> 3600 * 2
      else -> 0
    }
  }

  /**
   * Compute the URL with the given preferences.
   *
   * @param preferences The preferences to use.
   * @param limit The limit of the number of activities to return.
   * @return The URL to fetch activities from.
   */
  private fun computeUrlWithPreferences(preferences: List<Preference>, limit: Int): HttpUrl {
    if (preferences.isEmpty()) return baseHttpUrl

    val facetsParam = preferences.joinToString(",") { it.toSwissTourismFacet() }

    val facetFilters =
        preferences.joinToString(",") {
          "${it.toSwissTourismFacet()}:${it.toSwissTourismFacetFilter()}"
        }

    // Build manually – no encoding issues
    val url =
        StringBuilder()
            .append(baseHttpUrl)
            .append("&hitsPerPage=")
            .append(limit)
            .append("&facets=")
            .append(facetsParam)
            .append("&facet.filter=")
            .append("[$facetFilters]")
            .toString()

    Log.d("URL", "Final MySwitzerland URL: $url")
    return url.toHttpUrl()
  }

  /**
   * Fetches valid activities with pagination until the desired limit is reached.
   *
   * @param baseUrl The base URL to fetch activities from.
   * @param limit The limit of the number of valid activities to return.
   * @return A list of valid activities up to the specified limit.
   */
  private suspend fun fetchValidActivitiesPaginated(baseUrl: HttpUrl, limit: Int): List<Activity> {

    val validResults = mutableListOf<Activity>()
    var page = 0

    while (validResults.size < limit) {
      // Randomly increase the number of activities pulled to introduce randomness
      val totalActivityPull = getActivityNumberToPull(limit, Math.random() < ACTIVITY_SHUFFLE)
      val url =
          baseUrl
              .newBuilder()
              .setQueryParameter("page", page.toString())
              .setQueryParameter("hitsPerPage", totalActivityPull.toString())
              .build()

      val pageActivities = fetchActivitiesFromUrl(url)

      if (pageActivities.isEmpty()) break // no more pages

      val filtered =
          pageActivities.filter {
            it.isValid(
                blacklistedActivityNames = blacklistedActivityNames,
                invalidDescription = DESCRIPTION_FALLBACK)
          }

      validResults.addAll(filtered)

      page++
    }
    // Shuffle the results to introduce randomness since there can be many valid activities
    validResults.shuffle()
    return validResults.take(limit)
  }

  /**
   * This is public for testing purposes only and is not dangerous to have in public. Determine the
   * number of activities to pull from the API, adding randomness.
   *
   * @param limit The base limit of activities to pull.
   * @return The adjusted number of activities to pull.
   */
  fun getActivityNumberToPull(limit: Int, random: Boolean): Int {
    return if (random) {
      limit + (limit * EXTRA_RANDOM_ACTIVITIES).toInt()
    } else {
      limit
    }
  }

  /**
   * Get the most popular activities.
   *
   * @param limit The limit of the number of activities to return.
   * @param page The page number for pagination.
   * @return A list of the most popular activities.
   */
  override suspend fun getMostPopularActivities(limit: Int, page: Int): List<Activity> {
    return fetchValidActivitiesPaginated(baseHttpUrl, limit)
  }

  /**
   * Get activities near the given coordinate.
   *
   * @param coordinate The coordinate to get activities near.
   * @param radiusMeters The radius in meters to search for activities.
   * @param limit The limit of the number of activities to return.
   * @return A list of activities near the given coordinate.
   */
  override suspend fun getActivitiesNear(
      coordinate: Coordinate,
      radiusMeters: Int,
      limit: Int
  ): List<Activity> {
    val baseUrl =
        baseHttpUrl
            .newBuilder()
            .addQueryParameter(
                "geo.dist", "${coordinate.latitude},${coordinate.longitude},$radiusMeters")
            .build()

    return fetchValidActivitiesPaginated(baseUrl, limit)
  }

  /**
   * Get activities by the given preferences.
   *
   * @param preferences The preferences to use.
   * @param limit The limit of the number of activities to return.
   * @return A list of activities by the given preferences.
   */
  override suspend fun getActivitiesByPreferences(
      preferences: List<Preference>,
      limit: Int
  ): List<Activity> {
    return fetchValidActivitiesPaginated(computeUrlWithPreferences(preferences, limit), limit)
  }
  /** Searches for destinations based on a text query. */
  override suspend fun searchDestinations(query: String, limit: Int): List<Activity> {
    val url =
        destinationHttpUrl
            .newBuilder()
            .addQueryParameter("hitsPerPage", limit.toString())
            .addQueryParameter("query", query)
            .build()
    val activities = fetchActivitiesFromUrl(url)
    return activities
  }

  /**
   * Get activities near the given coordinate with the given preferences.
   *
   * @param preferences The preferences to use.
   * @param coordinate The coordinate to get activities near.
   * @param radiusMeters The radius in meters to search for activities.
   * @param limit The limit of the number of activities to return.
   * @return A list of activities near the given coordinate with the given preferences.
   */
  override suspend fun getActivitiesNearWithPreference(
      preferences: List<Preference>,
      coordinate: Coordinate,
      radiusMeters: Int,
      limit: Int
  ): List<Activity> {
    val baseUrl =
        computeUrlWithPreferences(preferences, limit)
            .newBuilder()
            .addQueryParameter(
                "geo.dist", "${coordinate.latitude},${coordinate.longitude},$radiusMeters")
            .build()

    return fetchValidActivitiesPaginated(baseUrl, limit)
  }
}
