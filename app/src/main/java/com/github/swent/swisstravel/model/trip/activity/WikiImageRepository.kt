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
   * Function to get the image for a given name.
   *
   * @param name Name
   * @return URL of the image or null if not found
   */
  suspend fun getImageByName(name: String): String? {
    return try {
      val resp = api.getImageForTitle(title = name)
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
      val resp = api.searchImages(query = name, limit = maxImages)
      val pages = resp.query?.pages ?: emptyList()

      pages.mapNotNull { it.thumbnail?.source }
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
