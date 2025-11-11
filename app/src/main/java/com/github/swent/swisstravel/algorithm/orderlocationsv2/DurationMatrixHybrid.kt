package com.github.swent.swisstravel.algorithm.orderlocationsv2

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.TransportMode
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.api.matrix.v1.models.MatrixResponse
import com.mapbox.geojson.Point
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A thin wrapper around Mapbox Matrix-like requests specialized for "one start -> many ends"
 * queries. It groups calls per start coordinate to minimize API calls.
 *
 * NOTE: This expects the Mapbox Matrix builder to behave like your original
 * `DurationMatrix.buildClient`.
 */
open class DurationMatrixHybrid(private val context: Context) {

  /**
   * For a given start coordinate and a list of end coordinates, request durations in a single
   * Matrix call.
   *
   * Returns a map from (start, end) to duration in seconds. If any duration cannot be retrieved,
   * the corresponding value will be null.
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
   * Build a MapboxMatrix client for the provided points and transport mode. Reuses the same pattern
   * you had in DurationMatrix.buildClient.
   */
  private fun buildClient(points: List<Point>, mode: TransportMode): MapboxMatrix {
    val profile =
        when (mode) {
          TransportMode.WALKING -> com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_WALKING
          TransportMode.TRAIN,
          TransportMode.BUS,
          TransportMode.TRAM ->
              com.mapbox.api.directions.v5.DirectionsCriteria
                  .PROFILE_DRIVING // Mapbox doesn't have train/tram; we will need cff api here
          TransportMode.CAR -> com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING
          else -> com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING
        }

    return MapboxMatrix.builder()
        .accessToken(context.getString(com.github.swent.swisstravel.R.string.mapbox_access_token))
        .profile(profile)
        .clientAppName(context.getString(com.github.swent.swisstravel.R.string.app_name))
        .coordinates(points)
        .build()
  }
}
