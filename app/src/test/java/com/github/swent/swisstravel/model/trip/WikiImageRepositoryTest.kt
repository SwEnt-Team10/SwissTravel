package com.github.swent.swisstravel.model.trip

import com.github.swent.swisstravel.model.trip.activity.WikiImageApi
import com.github.swent.swisstravel.model.trip.activity.WikiImageRepository
import com.github.swent.swisstravel.model.trip.activity.WikiPage
import com.github.swent.swisstravel.model.trip.activity.WikiQuery
import com.github.swent.swisstravel.model.trip.activity.WikiResponse
import com.github.swent.swisstravel.model.trip.activity.WikiThumbnail
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * A fake implementation of WikiImageApi for unit testing. No network calls, we just return
 * preconfigured responses.
 */
private class FakeWikiImageApi : WikiImageApi {

  var nextGetImageResponse: WikiResponse? = null
  var nextSearchImagesResponse: WikiResponse? = null

  var throwOnGetImage: Boolean = false
  var throwOnSearchImages: Boolean = false
  var lastGetImageParams: Map<String, String>? = null
  var lastSearchImageParams: Map<String, String>? = null

  override suspend fun getImageForTitle(params: Map<String, String>): WikiResponse {
    lastGetImageParams = params

    if (throwOnGetImage) {
      throw RuntimeException("Fake getImageForTitle error")
    }
    return nextGetImageResponse ?: WikiResponse(WikiQuery(emptyList()))
  }

  override suspend fun searchImages(params: Map<String, String>): WikiResponse {
    lastSearchImageParams = params

    if (throwOnSearchImages) {
      throw RuntimeException("Fake searchImages error")
    }
    return nextSearchImagesResponse ?: WikiResponse(WikiQuery(emptyList()))
  }
}

class WikiImageRepositoryTest {

  // ---------- getImageByName tests ----------

  @Test
  fun `getImageByName returns first thumbnail url when available`() = runBlocking {
    val fakeApi = FakeWikiImageApi()
    fakeApi.nextGetImageResponse =
        WikiResponse(
            query =
                WikiQuery(
                    pages =
                        listOf(
                            WikiPage(
                                pageid = 1L,
                                title = "Lausanne Cathedral",
                                thumbnail =
                                    WikiThumbnail(
                                        source = "https://example.com/image1.jpg",
                                        width = 800,
                                        height = 600)),
                            // Even if more pages exist, repo should take the first one
                            WikiPage(
                                pageid = 2L,
                                title = "Other",
                                thumbnail =
                                    WikiThumbnail(
                                        source = "https://example.com/image2.jpg",
                                        width = 800,
                                        height = 600)))))

    val repo = WikiImageRepository(fakeApi)

    val url = repo.getImageByName("Lausanne Cathedral")
    assertEquals("https://example.com/image1.jpg", url)
  }

  @Test
  fun `getImageByName returns null when no pages`() = runBlocking {
    val fakeApi = FakeWikiImageApi()
    fakeApi.nextGetImageResponse = WikiResponse(query = WikiQuery(pages = emptyList()))

    val repo = WikiImageRepository(fakeApi)

    val url = repo.getImageByName("Unknown place")
    assertNull(url)
  }

  @Test
  fun `getImageByName returns null when thumbnail is null`() = runBlocking {
    val fakeApi = FakeWikiImageApi()
    fakeApi.nextGetImageResponse =
        WikiResponse(
            query =
                WikiQuery(
                    pages = listOf(WikiPage(pageid = 1L, title = "No thumb", thumbnail = null))))

    val repo = WikiImageRepository(fakeApi)

    val url = repo.getImageByName("No thumb")
    assertNull(url)
  }

  @Test
  fun `getImageByName returns null on exception`() = runBlocking {
    val fakeApi = FakeWikiImageApi().apply { throwOnGetImage = true }
    val repo = WikiImageRepository(fakeApi)

    val url = repo.getImageByName("Will cause error")
    assertNull(url) // Repository swallows error and returns null
  }

  // ---------- getImagesByName tests ----------

  @Test
  fun `getImagesByName returns up to maxImages urls`() = runBlocking {
    val fakeApi = FakeWikiImageApi()
    fakeApi.nextSearchImagesResponse =
        WikiResponse(
            query =
                WikiQuery(
                    pages =
                        listOf(
                            WikiPage(
                                pageid = 1L,
                                title = "A",
                                thumbnail =
                                    WikiThumbnail(
                                        source = "https://example.com/a.jpg",
                                        width = 800,
                                        height = 600)),
                            WikiPage(
                                pageid = 2L,
                                title = "B",
                                thumbnail =
                                    WikiThumbnail(
                                        source = "https://example.com/b.jpg",
                                        width = 800,
                                        height = 600)),
                            WikiPage(
                                pageid = 3L,
                                title = "C",
                                thumbnail =
                                    WikiThumbnail(
                                        source = "https://example.com/c.jpg",
                                        width = 800,
                                        height = 600)))))

    val repo = WikiImageRepository(fakeApi)

    val urls = repo.getImagesByName("Some place", maxImages = 2)

    assertEquals(2, urls.size)
    assertEquals("https://example.com/a.jpg", urls[1])
    assertEquals("https://example.com/b.jpg", urls[0])
  }

  @Test
  fun `getImagesByName filters out null thumbnails`() = runBlocking {
    val fakeApi = FakeWikiImageApi()
    fakeApi.nextSearchImagesResponse =
        WikiResponse(
            query =
                WikiQuery(
                    pages =
                        listOf(
                            WikiPage(
                                pageid = 1L,
                                title = "A",
                                thumbnail =
                                    WikiThumbnail(
                                        source = "https://example.com/a.jpg",
                                        width = 800,
                                        height = 600)),
                            WikiPage(pageid = 2L, title = "B", thumbnail = null))))

    val repo = WikiImageRepository(fakeApi)

    val urls = repo.getImagesByName("Some place", maxImages = 5)

    assertEquals(1, urls.size)
    assertEquals("https://example.com/a.jpg", urls[0])
  }

  @Test
  fun `getImagesByName returns empty list when no pages`() = runBlocking {
    val fakeApi = FakeWikiImageApi()
    fakeApi.nextSearchImagesResponse = WikiResponse(query = WikiQuery(pages = emptyList()))

    val repo = WikiImageRepository(fakeApi)

    val urls = repo.getImagesByName("Nothing here")
    assertTrue(urls.isEmpty())
  }

  @Test
  fun `getImagesByName returns empty list on exception`() = runBlocking {
    val fakeApi = FakeWikiImageApi().apply { throwOnSearchImages = true }
    val repo = WikiImageRepository(fakeApi)

    val urls = repo.getImagesByName("Will fail")
    assertTrue(urls.isEmpty())
  }

  // ---------- Data class + Moshi parsing tests ----------

  @Test
  fun `Moshi parses WikiResponse with pages and thumbnails`() {
    val json =
        """
            {
              "batchcomplete": true,
              "query": {
                "pages": [
                  {
                    "pageid": 123,
                    "title": "Lausanne Cathedral",
                    "thumbnail": {
                      "source": "https://upload.wikimedia.org/some_image.jpg",
                      "width": 800,
                      "height": 533
                    }
                  }
                ]
              }
            }
            """
            .trimIndent()

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val adapter = moshi.adapter(WikiResponse::class.java)

    val resp = adapter.fromJson(json)
    assertNotNull(resp)
    val pages = resp!!.query?.pages
    assertNotNull(pages)
    assertEquals(1, pages!!.size)

    val page = pages[0]
    assertEquals(123L, page.pageid)
    assertEquals("Lausanne Cathedral", page.title)
    assertEquals("https://upload.wikimedia.org/some_image.jpg", page.thumbnail?.source)
    assertEquals(800, page.thumbnail?.width)
    assertEquals(533, page.thumbnail?.height)
  }

  @Test
  fun `WikiImageRepository default builds a non-null repository`() {
    // This only tests construction; it does not call the real network.
    val repo = WikiImageRepository.Companion.default()
    assertNotNull(repo)
  }
}
