package com.github.swent.swisstravel.ui.tripcreation

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object RandomTripGenerator {

  /**
   * Generates a random trip's destinations based on settings.
   *
   * @param context The context.
   * @param settings The current trip settings.
   * @param seed The seed for the random number generator.
   * @return A triple containing the start location, end location, and a list of intermediate
   *   destinations.
   */
  fun generateRandomDestinations(
      context: Context,
      settings: TripSettings,
      seed: Int? = null
  ): Triple<Location, Location, List<Location>> {
    val grandTour =
        context.resources.getStringArray(R.array.grand_tour).map {
          val parts = it.split(";")
          Location(Coordinate(parts[1].toDouble(), parts[2].toDouble()), parts[0], "")
        }

    val random = seed?.let { Random(it) } ?: Random

    val start: Location
    val end: Location
    val availableCities: MutableList<Location>

    // Use the arrival location from settings if it exists. Should always exist.
    // Otherwise, pick a random start location.
    if (settings.arrivalDeparture.arrivalLocation != null) {
      start = settings.arrivalDeparture.arrivalLocation
      availableCities = grandTour.filter { it.name != start.name }.toMutableList()
    } else {
      availableCities = grandTour.toMutableList()
      start = availableCities.removeAt(random.nextInt(availableCities.size))
    }

    end = availableCities.removeAt(random.nextInt(availableCities.size))

    Log.d("RandomTripGenerator", "Start: $start, End: $end")

    val tripDurationDays =
        if (settings.date.startDate != null && settings.date.endDate != null) {
          ChronoUnit.DAYS.between(settings.date.startDate, settings.date.endDate).toInt() + 1
        } else {
          DEFAULT_DURATION
        }

    // Rule: roughly one new city every 2 days. Minimum 0, max 3.
    val numIntermediateDestinations = (tripDurationDays / 2).coerceAtMost(3).coerceAtLeast(0)

    val intermediateDestinations =
        if (numIntermediateDestinations > 0) {
          availableCities.shuffled(random).take(numIntermediateDestinations)
        } else {
          emptyList()
        }
    return Triple(start, end, intermediateDestinations)
  }
}
