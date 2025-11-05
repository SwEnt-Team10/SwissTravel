package com.github.swent.swisstravel.model.trip.activity

import android.util.Log
import com.github.swent.swisstravel.BuildConfig
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.toSwissTourismFacet
import com.github.swent.swisstravel.model.user.toSwissTourismFacetFilter
import com.google.firebase.Timestamp
import com.mapbox.maps.extension.style.expressions.dsl.generated.image
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.collections.emptyList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Implementation of a repository for activities. Uses the Swiss Tourism API. */
class ActivityRepositoryMySwitzerland : ActivityRepository {

  private val API_KEY = BuildConfig.MYSWITZERLAND_API_KEY
  private val baseHttpUrl: HttpUrl =
      urlBuilder("https://opendata.myswitzerland.io/v1/attractions/", "en")
  private val destinationHttpUrl: HttpUrl =
      urlBuilder("https://opendata.myswitzerland.io/v1/destinations/", "en")

  private fun urlBuilder(url: String, language: String): HttpUrl {
    val newUrl: HttpUrl =
        url.toHttpUrl()
            .newBuilder()
            .addQueryParameter("lang", language)
            .addQueryParameter("page", "0")
            .addQueryParameter("striphtml", "true")
            .addQueryParameter("expand", "true")
            .build()
    return newUrl
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
    return withContext(Dispatchers.IO) {
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

    val activities = mutableListOf<Activity>()

    for (i in 0 until dataArray.length()) {
      val item = dataArray.getJSONObject(i)
      val name = item.optString("name", "Unknown Activity")
      val description = item.optString("abstract", "No description")

      val geo = item.optJSONObject("geo")
      if (geo != null) {
        val lat = geo.optDouble("latitude", Double.NaN)
        val lon = geo.optDouble("longitude", Double.NaN)

        if (!lat.isNaN() && !lon.isNaN()) {
            val imageArray = item.optJSONArray("image")
            val imageUrls = mutableListOf<String>()

            if (imageArray != null) {
                for (j in 0 until imageArray.length()) {
                    val imgObj = imageArray.optJSONObject(j)
                    val url = imgObj?.optString("url")
                    if (!url.isNullOrBlank()) {
                        imageUrls.add(url)
                    }
                }
            }
          val coordinate = Coordinate(lat, lon)
          val location = Location(coordinate, name, imageUrls.firstOrNull())


          // Dummy start/end times for now
          // TODO add start/end times
          val start = Timestamp.now()
          val end = Timestamp(start.seconds + 3600, 0)

          activities.add(Activity(start, end, location, description, imageUrls))
        }
      }
    }

    return activities
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
            .append(facetFilters)
            .toString()

    Log.d("URL", "Final MySwitzerland URL: $url")
    return url.toHttpUrl()
  }

  /**
   * Get the most popular activities.
   *
   * @param limit The limit of the number of activities to return.
   * @return A list of the most popular activities.
   */
  override suspend fun getMostPopularActivities(limit: Int, page: Int): List<Activity> {
    val url =
        baseHttpUrl
            .newBuilder()
            .addQueryParameter("hitsPerPage", limit.toString())
            .setQueryParameter("page", page.toString())
            .addQueryParameter("top", "true")
            .build()
    return fetchActivitiesFromUrl(url)
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
    val url =
        baseHttpUrl
            .newBuilder()
            .addQueryParameter("hitsPerPage", limit.toString())
            .addQueryParameter(
                "geo.dist", "${coordinate.latitude},${coordinate.longitude},$radiusMeters")
            .build()

    return fetchActivitiesFromUrl(url)
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
    return fetchActivitiesFromUrl(computeUrlWithPreferences(preferences, limit))
  }

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
}
