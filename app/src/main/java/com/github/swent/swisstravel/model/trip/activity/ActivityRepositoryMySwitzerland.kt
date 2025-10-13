package com.github.swent.swisstravel.model.trip.activity

import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.UserPreference
import com.github.swent.swisstravel.model.user.toSwissTourismFacet
import com.github.swent.swisstravel.model.user.toSwissTourismFacetFilter
import com.google.firebase.Timestamp
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.collections.emptyList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ActivityRepositoryMySwitzerland : ActivityRepository {

  private val API_KEY = java.util.Properties().getProperty("MY_SWITZERLAND_API_KEY") ?: ""

  private val baseUrl =
      "https://opendata.myswitzerland.io/v1/attractions/?lang=en&page=0&striphtml=true"

  private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
  }

  private suspend fun fetchActivitiesFromUrl(url: String): List<Activity> {
    assert(API_KEY.isNotEmpty()) { "API Key for MySwitzerland is not set." }
    withContext(Dispatchers.IO) {
      val request =
          Request.Builder()
              .url(url)
              .header("accept", "application/json")
              .header("x-api-key", API_KEY)
              .build()

      try {
        val response = client.newCall(request).execute()
        response.use {
          if (!response.isSuccessful) {
            throw Exception("Unexpected code $response")
          }

          val body = response.body?.string()
          if (body != null) {
            return@withContext parseActivitiesFromJson(body)
          } else {
            return@withContext emptyList()
          }
        }
      } catch (e: IOException) {
        throw e
      }
    }
    return emptyList()
  }

  private fun parseActivitiesFromJson(json: String): List<Activity> {
    val root = JSONObject(json) // Top-level object
    val dataArray: JSONArray = root.optJSONArray("data") ?: return emptyList()

    val activities = mutableListOf<Activity>()

    for (i in 0 until dataArray.length()) {
      val item = dataArray.getJSONObject(i)

      // Extract the name (title)
      val name = item.optString("name", "Unknown Activity")

      // Extract the geo info
      val geo = item.optJSONObject("geo")
      if (geo != null) {
        val lat = geo.optDouble("latitude", Double.NaN)
        val lon = geo.optDouble("longitude", Double.NaN)

        if (!lat.isNaN() && !lon.isNaN()) {
          val coordinate = Coordinate(lat, lon)
          val location = Location(coordinate, name)

          // Dummy start/end times for now
          // NOTE STILL TODO
          val start = Timestamp.now()
          val end = Timestamp(start.seconds + 3600, 0)

          activities.add(Activity(start, end, location))
        }
      }
    }

    return activities
  }

  override suspend fun getMostPopularActivities(limit: Int): List<Activity> {
    val url = "$baseUrl&hitsPerPage=$limit&top=true"
    return fetchActivitiesFromUrl(url)
  }

  override suspend fun getActivitiesNear(
      coordinate: Coordinate,
      radiusMeters: Int,
      limit: Int
  ): List<Activity> {
    val url = "$baseUrl&hitsPerPage=$limit&geo.dist=$coordinate.lat,$coordinate.lon,$radiusMeters"
    return fetchActivitiesFromUrl(url)
  }

  override suspend fun getActivitiesByPreferences(
      preferences: List<UserPreference>,
      limit: Int
  ): List<Activity> {
    var url =
        "$baseUrl&hitsPerPage=$limit&facets=${preferences.joinToString(",") { it.toSwissTourismFacet() }}"
    var facetFilterString = "&facet.filter="
    for (preference in preferences) {
      facetFilterString +=
          "${preference.toSwissTourismFacet()}:${preference.toSwissTourismFacetFilter()},"
    }
    url += facetFilterString
    return fetchActivitiesFromUrl(url)
  }
}
