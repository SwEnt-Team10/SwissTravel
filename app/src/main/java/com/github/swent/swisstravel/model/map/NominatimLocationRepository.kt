package com.github.swent.swisstravel.model.map

import android.util.Log
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import kotlinx.coroutines.withContext

class NominatimLocationRepository(private val client: OkHttpClient) : LocationRepository {

    private fun parseBody(body: String): List<Location> {
        val jsonArray = JSONArray(body)

        return List(jsonArray.length()) { i ->
            val jsonObject = jsonArray.getJSONObject(i)
            val lat = jsonObject.getDouble("lat")
            val lon = jsonObject.getDouble("lon")
            val name = jsonObject.getString("display_name")
            Location(lat, lon, name)
        }
    }

    override suspend fun search(query: String): List<Location> =
        withContext(Dispatchers.IO) {
            // Using HttpUrl.Builder to properly construct the URL with query parameters.
            val url =
                HttpUrl.Builder()
                    .scheme("https")
                    .host("nominatim.openstreetmap.org")
                    .addPathSegment("search")
                    .addQueryParameter("q", query)
                    .addQueryParameter("format", "json")
                    .build()

            // Create the request with a custom User-Agent and optional Referer
            val request =
                Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "YourAppName/1.0 (your-email@example.com)") // Set a proper User-Agent
                    .header("Referer", "https://yourapp.com") // Optionally add a Referer
                    .build()

            try {
                val response = client.newCall(request).execute()
                response.use {
                    if (!response.isSuccessful) {
                        Log.d("NominatimLocationRepository", "Unexpected code $response")
                        throw Exception("Unexpected code $response")
                    }

                    val body = response.body?.string()
                    if (body != null) {
                        Log.d("NominatimLocationRepository", "Body: $body")
                        return@withContext parseBody(body)
                    } else {
                        Log.d("NominatimLocationRepository", "Empty body")
                        return@withContext emptyList()
                    }
                }
            } catch (e: IOException) {
                Log.e("NominatimLocationRepository", "Failed to execute request", e)
                throw e
            }
        }
}