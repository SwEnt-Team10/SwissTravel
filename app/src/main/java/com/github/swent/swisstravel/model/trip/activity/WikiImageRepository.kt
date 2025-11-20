package com.github.swent.swisstravel.model.trip.activity

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WikiImageRepository(private val api: WikiImageApi) {

  suspend fun getImageForActivityName(name: String): String? {
    return try {
      // NEW: use getImageForTitle, not searchImage
      val resp = api.getImageForTitle(title = name)

      // NEW: pages is a List<WikiPage>, not a Map
      val pages = resp.query?.pages ?: emptyList()

      pages.firstOrNull()?.thumbnail?.source
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  suspend fun getImagesForActivityName(name: String, maxImages: Int = 3): List<String> {
    return try {
      val resp = api.searchImages(query = name, limit = maxImages)
      val pages = resp.query?.pages ?: emptyList()

      pages.mapNotNull { it.thumbnail?.source }
    } catch (e: Exception) {
      e.printStackTrace()
      emptyList()
    }
  }

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
                        .header(
                            "User-Agent",
                            "SwissTravelApp/1.0 (https://example.com; youremail@example.com)")
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
