package com.github.swent.swisstravel.model.trip.activity

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data class for the response from the Wiki API.
 *
 * @param source The URL of the thumbnail.
 * @param width The width of the thumbnail.
 * @param height The height of the thumbnail.
 */
data class WikiThumbnail(val source: String?, val width: Int?, val height: Int?)

/**
 * Data class for the response from the Wiki API.
 *
 * @param pageid The ID of the page.
 * @param title The title of the page.
 * @param thumbnail The thumbnail of the page.
 */
data class WikiPage(val pageid: Long?, val title: String?, val thumbnail: WikiThumbnail?)

/**
 * Data class for the query from the Wiki API.
 *
 * @param pages The list of pages.
 */
data class WikiQuery(val pages: List<WikiPage>?)

/**
 * Data class for the response from the Wiki API.
 *
 * @param query The query.
 */
data class WikiResponse(val query: WikiQuery?)

/**
 * Interface for the Wiki API.
 *
 * @param action The action to perform.
 * @param titles The title of the page.
 * @param prop The property to get.
 * @param piprop The property to get for the thumbnail.
 * @param thumbSize The size of the thumbnail.
 * @param format The format of the response.
 * @param formatVersion The version of the format.
 * @param origin The origin of the request.
 */
interface WikiImageApi {

  /**
   * Function to fetch for an image for a given title.
   *
   * @return WikiResponse The response from the Wiki API.
   */
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

  /**
   * Function to search for images for a given query.
   *
   * @return WikiResponse The response from the Wiki API.
   */
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
