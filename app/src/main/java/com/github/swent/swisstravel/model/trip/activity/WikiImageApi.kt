package com.github.swent.swisstravel.model.trip.activity

import retrofit2.http.GET
import retrofit2.http.Query

data class WikiThumbnail(val source: String?, val width: Int?, val height: Int?)

data class WikiPage(val pageid: Long?, val title: String?, val thumbnail: WikiThumbnail?)

data class WikiQuery(val pages: List<WikiPage>?)

data class WikiResponse(val query: WikiQuery?)

interface WikiImageApi {
  @GET("w/api.php")
  suspend fun getImageForTitle(
      @Query("action") action: String = "query",
      @Query("titles") title: String,
      @Query("prop") prop: String = "pageimages",
      @Query("piprop") piprop: String = "thumbnail",
      @Query("pithumbsize") thumbSize: Int = 800,
      @Query("format") format: String = "json",
      @Query("formatversion") formatVersion: Int = 2,
      @Query("origin") origin: String = "*",
  ): WikiResponse

  @GET("w/api.php")
  suspend fun searchImages(
      @Query("action") action: String = "query",
      @Query("generator") generator: String = "search",
      @Query("gsrsearch") query: String,
      @Query("gsrlimit") limit: Int = 4,
      @Query("prop") prop: String = "pageimages",
      @Query("piprop") piprop: String = "thumbnail",
      @Query("pithumbsize") thumbSize: Int = 800,
      @Query("format") format: String = "json",
      @Query("formatversion") formatVersion: Int = 2,
      @Query("origin") origin: String = "*",
  ): WikiResponse
}
