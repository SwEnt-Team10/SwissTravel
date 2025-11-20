package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_WALKING
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.api.matrix.v1.models.MatrixResponse
import com.mapbox.geojson.Point
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Done with the help of AI
/**
 * A thin wrapper around Mapbox Matrix-like requests specialized for "one start -> many ends"
 * queries. It groups calls per start coordinate to minimize API calls.
 *
 * @param context Application context.
 */
open class DurationMatrixHybrid(private val context: Context) {

  /**
   * Requests travel durations from a single start coordinate to multiple end coordinates using a
   * single matrix API call (e.g., Mapbox Matrix API).
   *
   * This function performs an asynchronous network request and returns a mapping of `(start, end)`
   * coordinate pairs to their corresponding duration in seconds.
   *
   * Behavior:
   * - If the `ends` list is empty, an empty map is returned immediately.
   * - If the API call fails, or the response is invalid/empty, durations will be `null`.
   * - The result map always contains all requested end coordinates, even if the value is `null`.
   *
   * @param start The starting coordinate for the duration calculation.
   * @param ends The list of destination coordinates to request durations for.
   * @param mode The transport mode (CAR, WALKING, TRAIN, BUS, TRAM). Note: some modes may fallback
   *   to driving if unsupported by the matrix provider.
   * @return A map where keys are `(start, end)` coordinate pairs and values are travel durations in
   *   seconds. `null` if duration could not be retrieved.
   */
  open suspend fun fetchDurationsFromStart(
      start: Coordinate,
      ends: List<Coordinate>,
      mode: TransportMode
  ): Map<Pair<Coordinate, Coordinate>, Double?> {
    if (ends.isEmpty()) return emptyMap()

    // Build list of points: [start, end0, end1, ...]
    val points = mutableListOf<Point>()
    points.add(Point.fromLngLat(start.longitude, start.latitude))
    ends.forEach { points.add(Point.fromLngLat(it.longitude, it.latitude)) }

    // Reuse MapboxMatrix builder pattern from your old DurationMatrix
    val client = buildClient(points, mode)

    return suspendCancellableCoroutine { cont ->
      client.enqueueCall(
          object : Callback<MatrixResponse> {
            override fun onResponse(
                call: Call<MatrixResponse>,
                response: Response<MatrixResponse>
            ) {
              try {
                if (!response.isSuccessful) {
                  Log.e(
                      "DurationMatrixHybrid",
                      "Matrix request failed: ${response.code()} ${response.message()}")
                  // return nulls for each pair
                  cont.resume(ends.associateWith { null }.mapKeys { Pair(start, it.key) })
                  return
                }
                val body = response.body()
                if (body == null || body.code() != "Ok") {
                  Log.e(
                      "DurationMatrixHybrid",
                      "Matrix API returned error or empty body: ${response.message()}")
                  cont.resume(ends.associateWith { null }.mapKeys { Pair(start, it.key) })
                  return
                }

                val durations = body.durations()
                if (durations == null || durations.isEmpty()) {
                  cont.resume(ends.associateWith { null }.mapKeys { Pair(start, it.key) })
                  return
                }

                // durations is a square matrix where index 0 corresponds to the start point
                // Extract durations from row 0, columns 1..n (destination indices)
                val row0 = durations[0]
                val result = mutableMapOf<Pair<Coordinate, Coordinate>, Double?>()
                for (i in ends.indices) {
                  // row0 index i+1 corresponds to start -> ends[i]
                  val value = row0.getOrNull(i + 1)
                  result[Pair(start, ends[i])] = value?.toDouble()
                }
                cont.resume(result)
              } catch (e: Exception) {
                cont.resumeWithException(e)
              }
            }

            override fun onFailure(call: Call<MatrixResponse>, t: Throwable) {
              Log.e("DurationMatrixHybrid", "Matrix request failed.", t)
              cont.resume(ends.associateWith { null }.mapKeys { Pair(start, it.key) })
            }
          })
    }
  }

  /**
   * Builds a MapboxMatrix client for a given set of points and transport mode.
   *
   * @param points The list of points (coordinates) to include in the matrix request. The first
   *   point is usually the start, followed by all destinations.
   * @param mode The desired transport mode to compute durations.
   * @return Configured [MapboxMatrix] client.
   */
  private fun buildClient(points: List<Point>, mode: TransportMode): MapboxMatrix {
    val profile =
        when (mode) {
          TransportMode.WALKING -> PROFILE_WALKING
          TransportMode.TRAIN,
          TransportMode.BUS,
          TransportMode.TRAM ->
              PROFILE_DRIVING // Mapbox doesn't have train/tram; we will need open tourism api here
          TransportMode.CAR -> PROFILE_DRIVING
          else -> PROFILE_DRIVING
        }

    return MapboxMatrix.builder()
        .accessToken(context.getString(com.github.swent.swisstravel.R.string.mapbox_access_token))
        .profile(profile)
        .clientAppName(context.getString(com.github.swent.swisstravel.R.string.app_name))
        .coordinates(points)
        .build()
  }
}
