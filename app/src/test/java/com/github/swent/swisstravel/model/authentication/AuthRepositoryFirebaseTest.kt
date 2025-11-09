package com.github.swent.swisstravel.model.authentication

import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthCredential
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AuthRepositoryFirebaseTest {

  private lateinit var authRepository: AuthRepositoryFirebase
  private val mockAuth: FirebaseAuth = mockk()
  private val mockHelper: GoogleSignInHelper = mockk()
  private val mockUser: FirebaseUser = mockk(relaxed = true)
  private val mockCredential: CustomCredential = mockk()
  private val mockIdTokenCredential: GoogleIdTokenCredential = mockk()
  private val mockFirebaseCredential: GoogleAuthCredential = mockk()
  private val mockAuthResult: AuthResult = mockk()

  @Before
  fun setUp() {
    authRepository = AuthRepositoryFirebase(mockAuth, mockHelper)
    every { mockCredential.type } returns GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCredential.data } returns mockk()
    every { mockHelper.extractIdTokenCredential(any()) } returns mockIdTokenCredential
    every { mockIdTokenCredential.idToken } returns "test_token"
    every { mockHelper.toFirebaseCredential("test_token") } returns mockFirebaseCredential
    every { mockAuthResult.user } returns mockUser
  }

  @Test
  fun `signInWithEmailPassword success`() = runTest {
    val user =
        authRepository
            .signUpWithEmailPassword("test@email.com", "password", "John", "Doe")
            .getOrNull()!!
    authRepository.signOut()
    val result = authRepository.signInWithEmailPassword("test@email.com", "password")

    assertTrue(result.isSuccess)
    assertEquals(user.email, result.getOrNull()?.email)
  }

  @Test
  fun `signInWithEmailPassword failure`() = runTest {
    val result = authRepository.signInWithEmailPassword("test@email.com", "password")

    assertTrue(result.isFailure)
  }

  @Test
  fun `signUpWithEmailPassword success`() = runTest {
    val result = authRepository.signUpWithEmailPassword("test@email.com", "password", "John", "Doe")

    assertTrue(result.isSuccess)
    assertEquals("test@email.com", result.getOrNull()?.email)
    assertEquals("John Doe", result.getOrNull()?.displayName)
  }

  @Test
  fun `signUpWithEmailPassword failure`() = runTest {
    authRepository.signUpWithEmailPassword("test@email.com", "password", "John", "Doe")

    val result = authRepository.signUpWithEmailPassword("test@email.com", "password", "John", "Doe")

    assertTrue(result.isFailure)
  }

  @Test
  fun signOut_Success() = runTest {
    authRepository.signUpWithEmailPassword("test@email.com", "password", "John", "Doe")
    val result = authRepository.signOut()

    assertTrue(result.isSuccess)
    assertEquals(null, mockAuth.currentUser)
  }
}
