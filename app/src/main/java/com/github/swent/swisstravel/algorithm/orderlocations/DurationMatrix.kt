package com.github.swent.swisstravel.algorithm.orderlocations

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Coordinate
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.matrix.v1.MapboxMatrix
import com.mapbox.api.matrix.v1.models.MatrixResponse
import com.mapbox.geojson.Point
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A class that provides methods for retrieving the duration matrix between a list of coordinates.
 *
 * @param context The application context.
 */
open class DurationMatrix(private val context: Context) {

  /**
   * Converts a list of coordinates to a list of Mapbox points.
   *
   * @param coordinates The list of coordinates to convert.
   * @return A list of Mapbox points.
   */
  private fun toPoints(coordinates: List<Coordinate>): List<Point> {
    return coordinates.map { Point.fromLngLat(it.longitude, it.latitude) }
  }

  /**
   * Builds a Mapbox client with the given points.
   *
   * @param points The list of points to build the client with.
   * @return A Mapbox client.
   */
  open fun buildClient(points: List<Point>): MapboxMatrix {
    return MapboxMatrix.builder()
        .accessToken(context.getString(R.string.mapbox_access_token))
        .profile(DirectionsCriteria.PROFILE_DRIVING)
        .clientAppName(context.getString(R.string.app_name))
        .coordinates(points)
        .build()
  }

  /**
   * Retrieves the duration matrix between a list of coordinates.
   *
   * @param coordinates The list of coordinates to retrieve the duration matrix for.
   * @param callback The callback to invoke when the duration matrix is retrieved.
   */
  fun getDurations(coordinates: List<Coordinate>, callback: (Array<DoubleArray>?) -> Unit) {
    // Mapbox api sets limits on the number of points to give.
    if (coordinates.size !in 2..25) {
      callback(null)
      return
    }

    val points = toPoints(coordinates)

    val client = buildClient(points)

    client.enqueueCall(
        object : Callback<MatrixResponse> {
          override fun onResponse(call: Call<MatrixResponse>, response: Response<MatrixResponse>) {
            if (response.isSuccessful) {
              val body = response.body()
              if (body != null && body.code() == "Ok") {
                callback(body.durations()?.map { it.toDoubleArray() }?.toTypedArray())
              } else {
                // The request was successful, but the Matrix API returned an error.
                Log.e("DurationMatrix", "Matrix API error: ${response.message()}")
                callback(null)
              }
            } else {
              // The request was not successful
              Log.e("DurationMatrix", "Request failed with status code: ${response.code()}")
              callback(null)
            }
          }

          override fun onFailure(call: Call<MatrixResponse>, t: Throwable) {
            Log.e("DurationMatrix", "Request execution failed.", t)
            callback(null)
          }
        })
  }
}
