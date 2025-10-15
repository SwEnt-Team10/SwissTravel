package com.android.swisstravel.utils

import android.content.Context
import android.util.Base64
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.json.JSONObject

object FakeJwtGenerator {
  private var _counter = 0
  private val counter
    get() = _counter++

  private fun base64UrlEncode(input: ByteArray): String {
    return Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  fun createFakeGoogleIdToken(name: String, email: String): String {
    val header = JSONObject(mapOf("alg" to "none"))
    val payload =
        JSONObject(
            mapOf(
                "sub" to counter.toString(),
                "email" to email,
                "name" to name,
                "picture" to "http://example.com/avatar.png"))

    val headerEncoded = base64UrlEncode(header.toString().toByteArray())
    val payloadEncoded = base64UrlEncode(payload.toString().toByteArray())

    // Signature can be anything, emulator doesn't check it
    val signature = "sig"

    return "$headerEncoded.$payloadEncoded.$signature"
  }
}

class FakeCredentialManager private constructor(private val context: Context) :
    CredentialManager by CredentialManager.create(context) {
  companion object {
    // Creates a mock CredentialManager that always returns a CustomCredential
    // containing the given fakeUserIdToken when getCredential() is called.
    fun fake(fakeUserIdToken: String): CredentialManager {
      mockkObject(GoogleIdTokenCredential)
      val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
      every { googleIdTokenCredential.idToken } returns fakeUserIdToken
      every { GoogleIdTokenCredential.createFrom(any()) } returns googleIdTokenCredential
      val fakeCredentialManager = mockk<FakeCredentialManager>()
      val mockGetCredentialResponse = mockk<GetCredentialResponse>()

      val fakeCustomCredential =
          CustomCredential(
              type = TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
              data = bundleOf("id_token" to fakeUserIdToken))

      every { mockGetCredentialResponse.credential } returns fakeCustomCredential
      coEvery {
        fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
      } returns mockGetCredentialResponse

      return fakeCredentialManager
    }

    // Creates a mock CredentialManager that returns a sequence of tokens on successive
    // getCredential() calls. Useful to simulate multiple logins within a single test without
    // recreating the composition.
    fun sequence(vararg fakeUserIdTokens: String): CredentialManager {
      mockkObject(GoogleIdTokenCredential)

      val fakeCredentialManager = mockk<FakeCredentialManager>()

      // Prepare GoogleIdTokenCredential objects per token
      val googleCreds = fakeUserIdTokens.map { token ->
        mockk<GoogleIdTokenCredential>().also { every { it.idToken } returns token }
      }

      // Return a different GoogleIdTokenCredential on each createFrom() call
      every { GoogleIdTokenCredential.createFrom(any()) } returnsMany googleCreds andThen googleCreds.last()

      // Prepare GetCredentialResponse per token containing the raw id_token in the bundle
      val responses = fakeUserIdTokens.map { token ->
        val response = mockk<GetCredentialResponse>()
        val credential =
            CustomCredential(type = TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data = bundleOf("id_token" to token))
        every { response.credential } returns credential
        response
      }

      // Return responses in order for successive getCredential() calls
      coEvery { fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returnsMany responses andThen responses.last()

      return fakeCredentialManager
    }
  }
}
