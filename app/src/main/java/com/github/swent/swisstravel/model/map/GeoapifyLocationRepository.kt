package com.github.swent.swisstravel.model.map

import com.github.swent.swisstravel.BuildConfig
import com.github.swent.swisstravel.model.trip.Location
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

class GeoapifyLocationRepository(
    private val client: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val apiKey: String = BuildConfig.GEOAPIFY_API_KEY,
    private val baseUrl: String = "https://api.geoapify.com/v1/geocode/autocomplete?apiKey=$apiKey"
) : LocationRepository {

  override suspend fun search(query: String): List<Location> {
    TODO("Not yet implemented")
  }
}
