package com.github.swent.swisstravel.model.trip.activity

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Implementation of a [WikiImageApi]* */
class WikiImageRepository(private val api: WikiImageApi) {

  /**
   * Common search params for the WikiImageApi. (getImageByTitle)
   *
   * @param title Title of the page
   */
  private fun titleParams(title: String) =
      mapOf(
          "action" to "query",
          "titles" to title,
          "prop" to "pageimages",
          "piprop" to "thumbnail",
          "pithumbsize" to "800",
          "format" to "json",
          "formatversion" to "2",
          "origin" to "*")

  /**
   * Common search parameters for the WikiImageApi. (getImagesByTitle)
   *
   * @param query Search query
   * @param limit Maximum number of results
   */
  private fun searchParams(query: String, limit: Int) =
      mapOf(
          "action" to "query",
          "generator" to "search",
          "gsrsearch" to query,
          "gsrlimit" to limit.toString(),
          "prop" to "pageimages",
          "piprop" to "thumbnail",
          "pithumbsize" to "800",
          "format" to "json",
          "formatversion" to "2",
          "origin" to "*")

  /**
   * Function to get the image for a given name.
   *
   * @param name Name
   * @return URL of the image or null if not found
   */
  suspend fun getImageByName(name: String): String? {
    return try {
      val resp = api.getImageForTitle(titleParams(name))
      val pages = resp.query?.pages ?: emptyList()

      pages.firstOrNull()?.thumbnail?.source
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Function to get the images for a given name.
   *
   * @param name Name
   * @param maxImages Maximum number of images to return
   * @return List of URLs of the images
   */
  suspend fun getImagesByName(name: String, maxImages: Int = 3): List<String> {
    return try {
      val resp = api.searchImages(searchParams("$name Switzerland", maxImages))
      val pages = resp.query?.pages ?: emptyList()

      pages.take(maxImages).mapNotNull { it.thumbnail?.source }.reversed()
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

  /** Retrofit interface for the WikiImageApi. */
  companion object {
    fun default(): WikiImageRepository {
      val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

      val client =
          OkHttpClient.Builder()
              .addInterceptor { chain ->
                val newReq =
                    chain
                        .request()
                        .newBuilder()
                        .header("User-Agent", "SwissTravelApp/1.0 (swisstravel.epfl@proton.me)")
                        .build()
                chain.proceed(newReq)
              }
              .addInterceptor(
                  HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
              .build()

      val retrofit =
          Retrofit.Builder()
              .baseUrl("https://en.wikipedia.org/")
              .client(client)
              .addConverterFactory(MoshiConverterFactory.create(moshi))
              .build()

      val api = retrofit.create(WikiImageApi::class.java)
      return WikiImageRepository(api)
    }
  }
}
