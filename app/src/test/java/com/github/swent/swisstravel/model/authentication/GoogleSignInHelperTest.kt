package com.github.swent.swisstravel.model.authentication

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultGoogleSignInHelperTest {

  private lateinit var googleSignInHelper: DefaultGoogleSignInHelper
  private val mockAuthCredential: AuthCredential = mockk()

  @Before
  fun setUp() {
    googleSignInHelper = DefaultGoogleSignInHelper()
    mockkStatic(GoogleIdTokenCredential::class)
    mockkStatic(GoogleAuthProvider::class)
  }

  @Test
  fun toFirebaseCredential_callsCorrectMethod() {
    val idToken = "test_id_token"
    every { GoogleAuthProvider.getCredential(idToken, null) } returns mockAuthCredential

    val result = googleSignInHelper.toFirebaseCredential(idToken)

    assertEquals(mockAuthCredential, result)
    verify { GoogleAuthProvider.getCredential(idToken, null) }
  }
}
