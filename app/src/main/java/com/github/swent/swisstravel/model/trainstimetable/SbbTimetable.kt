@file:Suppress("DEPRECATION")

package com.github.swent.swisstravel.model.trainstimetable

import android.util.Log
import com.github.swent.swisstravel.BuildConfig
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.RouteSegment
import java.time.Duration
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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
class SbbTimetable(private val baseUrl: String = "https://api.opentransportdata.swiss/") :
    TrainTimetable {

  private val ojpToken = BuildConfig.OPEN_TRANSPORT_DATA_TOKEN

  private val api: OjpApiService by lazy {
    val logging =
        HttpLoggingInterceptor { message -> Log.d("OJP_API", message) }
            .setLevel(HttpLoggingInterceptor.Level.BODY)
    val client = OkHttpClient.Builder().addInterceptor(logging).build()

    val serializer = Persister(AnnotationStrategy())

    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        // Pass the namespace-aware serializer to the factory
        .addConverterFactory(SimpleXmlConverterFactory.create(serializer))
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
   * @return An int indicating the duration of the fastest route in seconds. Returns -1 on failure.
   */
  override suspend fun getFastestRoute(from: Location, to: Location): Int {
    val requestBody = createOjpTripRequest(from, to).toRequestBody("application/xml".toMediaType())
    return try {
      val response = api.getTrip("Bearer $ojpToken", requestBody)

      val durationString =
          response.ojpResponse
              ?.serviceDelivery
              ?.ojpTripDelivery
              ?.tripResults
              ?.firstOrNull()
              ?.trip
              ?.duration

      if (durationString != null) {
        Duration.parse(durationString).seconds.toInt()
      } else {
        Log.d("SbbTimetable", "Duration string was null in the response.")
        -1
      }
    } catch (e: Exception) {
      // Log the full exception to get more details on the parsing error
      Log.e("SbbTimetable", "Error fetching or parsing timetable", e)
      -1
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
        val duration = getFastestRoute(locations[i], locations[j])
        durationMatrix[i][j] = duration.toDouble()

        // Delay to respect the API rate limit (50 calls/min -> 1 call every 1.2s)
        delay(API_LIMIT)
      }
    }

    return durationMatrix
  }
}
