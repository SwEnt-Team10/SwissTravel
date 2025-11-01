package com.github.swent.swisstravel.utils

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

object FakeHttpClient {

  /**
   * A fake HTTP client that intercepts requests and provides predefined responses for testing
   * location search functionality.
   */
  // TODO : Modify this value once we have an API to mock
  private const val FAKE_BODY = """{"result":"ok"}"""

  private class APIInterceptor(val checkUrl: Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val url = request.url.toString()
      Log.d("MockInterceptor", "Intercepted URL: $url")
      if (checkUrl) {
        assertTrue("Request must use HTTPS", request.url.isHttps)
        assertEquals("json", request.url.queryParameter("format"))
      }
      // TO-DO : parse the code so that the response looks like the actual API response

      return Response.Builder() // you should change this code to adapt it to the APIs
          .code(200)
          .message("OK")
          .request(request)
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .body(FAKE_BODY.toResponseBody("application/json".toMediaTypeOrNull()))
          .build()
    }
  }

  fun getClient(checkUrl: Boolean = false): OkHttpClient =
      OkHttpClient.Builder().addInterceptor(APIInterceptor(checkUrl)).build()
}
