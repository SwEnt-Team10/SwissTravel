package com.github.swent.swisstravel.model.trip.activity

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location

/**
 * Configuration for a major city, used for adding city-based activities.
 *
 * @param location The geographical location of the city center.
 * @param radius The radius in km to consider an activity as being in/near the city.
 * @param maxDays The maximum number of days/cityActivities to schedule for this city.
 */
data class CityConfig(val location: Location, val radius: Int, val maxDays: Double)

class MajorSwissCities() {
  /**
   * Parses the list of major Swiss cities from the application resources.
   *
   * This method reads the string array `R.array.swiss_major_cities`, parses each entry into a
   * [CityConfig] object (containing location, radius, and max days).
   *
   * @param context The context used to access application resources.
   */
  fun getMajorSwissCities(context: Context): List<CityConfig> {
    val cityArray = context.resources.getStringArray(R.array.swiss_major_cities)
    val majorSwissCitiesList =
        cityArray.mapNotNull { entry ->
          val parts = entry.split(";")
          if (parts.size >= 5) {
            try {
              val name = parts[0].trim()
              val lat = parts[1].trim().toDouble()
              val lon = parts[2].trim().toDouble()
              val radius = parts[3].trim().toInt()
              val maxDays = parts[4].trim().toDouble()
              CityConfig(Location(Coordinate(lat, lon), name), radius, maxDays)
            } catch (e: Exception) {
              Log.e("TripAlgorithm", "Failed to parse City Config location: $entry", e)
              null
            }
          } else {
            Log.w("TripAlgorithm", "Invalid City Config entry format: $entry")
            null
          }
        }
    return majorSwissCitiesList
  }
}
