package com.github.swent.swisstravel.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthCredential
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AuthRepositoryFirebaseTest {

  private lateinit var authRepository: AuthRepositoryFirebase
  private val mockAuth: FirebaseAuth = mockk()
  private val mockHelper: GoogleSignInHelper = mockk()
  private val mockUser: FirebaseUser = mockk()
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
  fun signInWithGoogle_Success() = runTest {
    coEvery { mockAuth.signInWithCredential(mockFirebaseCredential) } returns
        Tasks.forResult(mockAuthResult)

    val result = authRepository.signInWithGoogle(mockCredential)

    assertTrue(result.isSuccess)
    assertEquals(mockUser, result.getOrNull())
  }

  @Test
  fun signInWithGoogle_Failure_WrongCredentialType() = runTest {
    val wrongCredential: Credential = mockk()

    val result = authRepository.signInWithGoogle(wrongCredential)

    assertTrue(result.isFailure)
    assertEquals(
        "Login failed: Credential is not of type Google ID", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithGoogle_Failure_UserNull() = runTest {
    every { mockAuthResult.user } returns null
    coEvery { mockAuth.signInWithCredential(mockFirebaseCredential) } returns
        Tasks.forResult(mockAuthResult)

    val result = authRepository.signInWithGoogle(mockCredential)

    assertTrue(result.isFailure)
    assertEquals(
        "Login failed: Could not retrieve user information", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithGoogle_Failure_FirebaseSignInFails() = runTest {
    val exception = Exception("Firebase error")
    coEvery { mockAuth.signInWithCredential(mockFirebaseCredential) } returns
        Tasks.forException(exception)

    val result = authRepository.signInWithGoogle(mockCredential)

    assertTrue(result.isFailure)
    assertEquals("Login failed: Firebase error", result.exceptionOrNull()?.message)
  }

  @Test
  fun signOut_Success() {
    every { mockAuth.signOut() } returns Unit

    val result = authRepository.signOut()

    assertTrue(result.isSuccess)
    verify { mockAuth.signOut() }
  }

  @Test
  fun signOut_Failure() {
    val exception = Exception("Logout error")
    every { mockAuth.signOut() } throws exception

    val result = authRepository.signOut()

    assertTrue(result.isFailure)
    assertEquals("Logout failed: Logout error", result.exceptionOrNull()?.message)
  }
}
