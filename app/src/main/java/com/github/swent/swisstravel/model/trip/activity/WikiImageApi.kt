package com.github.swent.swisstravel.model.trip.activity

import retrofit2.http.GET
import retrofit2.http.QueryMap

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
 * @param params The parameters for the API call. They are configured in the repository
 */
interface WikiImageApi {

  /**
   * Function to fetch for an image for a given title.
   *
   * @return WikiResponse The response from the Wiki API.
   */
  @GET("w/api.php")
  suspend fun getImageForTitle(@QueryMap params: Map<String, String>): WikiResponse

  /**
   * Function to search for images for a given query.
   *
   * @return WikiResponse The response from the Wiki API.
   */
  @GET("w/api.php") suspend fun searchImages(@QueryMap params: Map<String, String>): WikiResponse
}
