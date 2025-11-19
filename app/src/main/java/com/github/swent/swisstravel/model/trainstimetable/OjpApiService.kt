package com.github.swent.swisstravel.model.trainstimetable

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Defines the API for interacting with the Open Journey Planner (OJP) endpoint. This interface is
 * used by Retrofit to generate the network layer for fetching trip data.
 *
 * This interface was written with the help of Gemini.
 */
fun interface OjpApiService {
  /**
   * Fetches trip information based on an OJP XML request.
   *
   * This function sends a POST request to the `ojp20` endpoint with the specified XML body and
   * authorization token. It's a suspend function, designed to be called from a coroutine.
   *
   * @param token The bearer token for API authorization, passed in the "Authorization" header.
   * @param body The XML request body, typically an OJP TripRequest.
   * @return An [OjpResponse] object parsed from the XML response.
   */
  @Headers("Content-Type: application/xml")
  @POST("ojp20")
  suspend fun getTrip(@Header("Authorization") token: String, @Body body: RequestBody): OjpResponse
}
