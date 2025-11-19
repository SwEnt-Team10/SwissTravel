@file:Suppress("DEPRECATION")

package com.github.swent.swisstravel.model.trainstimetable

import android.util.Log
import com.github.swent.swisstravel.BuildConfig
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import com.github.swent.swisstravel.model.trip.TransportMode
import com.google.firebase.Timestamp
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

const val API_LIMIT = 1_200L

/**
 * An implementation of the [TrainTimetable] interface that fetches data from the Swiss Open
 * Transport Data platform (OJP).
 *
 * This class was written with the help of Gemini.
 *
 * This class uses Retrofit to make XML-based API calls to the OJP 2.0 endpoint. It is responsible
 * for building requests, parsing responses, and converting them into the data models used by the
 * application, such as [RouteSegment] and a travel duration matrix.
 */
class SbbTimetable() : TrainTimetable {

  private val ojpToken = BuildConfig.OPEN_TRANSPORT_DATA_TOKEN

  private val api: OjpApiService by lazy {
    Retrofit.Builder()
        .baseUrl("https://api.opentransportdata.swiss/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(SimpleXmlConverterFactory.create(Persister(AnnotationStrategy())))
        .build()
        .create(OjpApiService::class.java)
  }

  /**
   * Fetches the fastest public transport route between two locations using the OJP API.
   *
   * It constructs an OJP TripRequest, sends it to the server, and parses the response to create a
   * list of [RouteSegment] objects representing the journey.
   *
   * @param from The starting [Location].
   * @param to The destination [Location].
   * @return A list of [RouteSegment] for the fastest trip, or `null` if the API call fails or no
   *   route is found.
   */
  override suspend fun getFastestRoute(from: Location, to: Location): List<RouteSegment>? {
    val requestBody = createOjpTripRequest(from, to).toRequestBody("application/xml".toMediaType())
    return try {
      val response = api.getTrip("Bearer $ojpToken", requestBody)
      response.ojpDeliver?.ojpTripDelivery?.tripResults?.firstOrNull()?.trip?.tripLegs?.map { leg ->
        // This conversion logic is crucial.
        val startLocation =
            Location(
                name = leg.legStart?.locationName?.text ?: "Unknown Start",
                coordinate =
                    leg.legStart?.geoPosition?.let { Coordinate(it.latitude, it.longitude) }
                        ?: from.coordinate)
        val endLocation =
            Location(
                name = leg.legEnd?.locationName?.text ?: "Unknown End",
                coordinate =
                    leg.legEnd?.geoPosition?.let { Coordinate(it.latitude, it.longitude) }
                        ?: to.coordinate)

        val startTime =
            Timestamp(Date.from(ZonedDateTime.parse(leg.legStart?.depArrTime).toInstant()))
        val endTime = Timestamp(Date.from(ZonedDateTime.parse(leg.legEnd?.depArrTime).toInstant()))
        val durationMinutes =
            Duration.between(startTime.toDate().toInstant(), endTime.toDate().toInstant())
                .toMinutes()
                .toInt()

        val transportMode =
            when (leg.service?.mode?.name?.text?.lowercase()) {
              "train" -> TransportMode.TRAIN
              "bus" -> TransportMode.BUS
              "tram" -> TransportMode.TRAM
              "car" -> TransportMode.CAR
              "walk" -> TransportMode.WALKING
              // Add other modes as needed
              else -> TransportMode.UNKNOWN // Default or from other leg info
            }

        RouteSegment(
            from = startLocation,
            to = endLocation,
            durationMinutes = durationMinutes,
            transportMode = transportMode,
            startDate = startTime,
            endDate = endTime)
      }
    } catch (e: Exception) {
      Log.d("SbbTimetable", "Error fetching timetable: ${e.message}")
      null
    }
  }

  /**
   * Computes a duration matrix for a given list of locations.
   *
   * This method calculates the travel time in seconds for all pairs of locations (from i to j). It
   * executes the API calls sequentially with a delay between each call to respect the OJP API rate
   * limit (50 calls/min). The diagonal of the matrix (i to i) is always 0.
   *
   * @param locations The list of [Location] objects to compute the matrix for.
   * @return A 2D array (`Array<DoubleArray>`) where `matrix[i][j]` is the travel duration in
   *   seconds from `locations[i]` to `locations[j]`. Returns -1.0 for failed API calls.
   */
  override suspend fun getDurationMatrix(locations: List<Location>): Array<DoubleArray> {
    val n = locations.size
    val durationMatrix =
        Array(n) { i -> DoubleArray(n) { j -> if (i == j) 0.0 else Double.POSITIVE_INFINITY } }

    // Iterate through all pairs sequentially to respect rate limiting
    for (i in 0 until n) {
      for (j in 0 until n) {
        if (i == j) continue

        // Fetch duration for the current pair
        val duration = getDurationInSeconds(locations[i], locations[j])
        durationMatrix[i][j] = duration

        // Delay to respect the API rate limit (50 calls/min -> 1 call every 1.2s)
        delay(API_LIMIT)
      }
    }

    return durationMatrix
  }

  /**
   * Calculates the total travel duration in seconds for the fastest route between two locations.
   *
   * This helper function reuses [getFastestRoute] to find the trip and then calculates the total
   * duration from the start of the first segment to the end of the last segment.
   *
   * @param from The starting [Location].
   * @param to The destination [Location].
   * @return The total travel time in seconds as a [Double]. If no route is found or an error
   *   occurs, it returns -1.0.
   */
  private suspend fun getDurationInSeconds(from: Location, to: Location): Double {
    val fastestRoute = getFastestRoute(from, to)
    return if (fastestRoute != null && fastestRoute.isNotEmpty()) {
      val startInstant = fastestRoute.first().startDate.toDate().toInstant()
      val endInstant = fastestRoute.last().endDate.toDate().toInstant()
      Duration.between(startInstant, endInstant).seconds.toDouble()
    } else {
      -1.0 // Return -1.0 for Double on failure
    }
  }
}
