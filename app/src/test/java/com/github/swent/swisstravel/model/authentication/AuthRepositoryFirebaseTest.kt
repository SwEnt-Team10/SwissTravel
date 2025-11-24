package com.github.swent.swisstravel.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.UserProfileChangeRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AuthRepositoryFirebaseTest {

  private lateinit var authRepository: AuthRepositoryFirebase
  private val mockAuth: FirebaseAuth = mockk(relaxed = true) // Relaxed to avoid mocking all calls
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
  fun signUpWithEmailPassword_Success() = runTest {
    // ARRANGE
    coEvery { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns
        Tasks.forResult(mockAuthResult)
    coEvery { mockUser.updateProfile(any<UserProfileChangeRequest>()) } returns
        Tasks.forResult(null)
    coEvery { mockUser.sendEmailVerification() } returns Tasks.forResult(null)

    // ACT
    val result =
        authRepository.signUpWithEmailPassword("test@example.com", "password", "John", "Doe")

    // ASSERT
    assertTrue(result.isSuccess)
    assertEquals(mockUser, result.getOrNull())
    coVerify { mockUser.updateProfile(any<UserProfileChangeRequest>()) }
    coVerify { mockUser.sendEmailVerification() }
  }

  @Test
  fun signUpWithEmailPassword_Failure_UserNull() = runTest {
    // Arrange
    every { mockAuthResult.user } returns null
    coEvery { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns
        Tasks.forResult(mockAuthResult)

    // Act
    val result =
        authRepository.signUpWithEmailPassword("test@example.com", "password", "John", "Doe")

    // Assert
    assertTrue(result.isFailure)
    assertEquals(
        "Sign up failed: Could not retrieve user information", result.exceptionOrNull()?.message)
  }

  @Test
  fun signUpWithEmailPassword_Failure_FirebaseFails() = runTest {
    // Arrange
    val exception = Exception("Firebase error")
    coEvery { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns
        Tasks.forException(exception)

    // Act
    val result =
        authRepository.signUpWithEmailPassword("test@example.com", "password", "John", "Doe")

    // Assert
    assertTrue(result.isFailure)
    assertEquals("Sign up failed: Firebase error", result.exceptionOrNull()?.message)
  }

  @Test
  fun signInWithEmailPassword_Success() = runTest {
    // Arrange
    coEvery { mockAuth.signInWithEmailAndPassword(any(), any()) } returns
        Tasks.forResult(mockAuthResult)

    // Act
    val result = authRepository.signInWithEmailPassword("test@example.com", "password")

    // Assert
    assertTrue(result.isSuccess)
    assertEquals(mockUser, result.getOrNull())
  }

  @Test
  fun signInWithEmailPassword_Failure_FirebaseFails() = runTest {
    // Arrange
    val exception = Exception("Firebase error")
    coEvery { mockAuth.signInWithEmailAndPassword(any(), any()) } returns
        Tasks.forException(exception)

    // Act
    val result = authRepository.signInWithEmailPassword("test@example.com", "password")

    // Assert
    assertTrue(result.isFailure)
    assertEquals("Login failed: Firebase error", result.exceptionOrNull()?.message)
  }

  @Test
  fun reloadAndCheckVerification_Success_UserVerified() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.isEmailVerified } returns true
    coEvery { mockUser.reload() } returns Tasks.forResult(null)

    // Act
    val result = authRepository.reloadAndCheckVerification()

    // Assert
    assertTrue(result.isSuccess)
    assertTrue(result.getOrThrow())
    coVerify { mockUser.reload() } // Verify that reload was called
  }

  @Test
  fun reloadAndCheckVerification_Success_UserNotVerified() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.isEmailVerified } returns false
    coEvery { mockUser.reload() } returns Tasks.forResult(null)

    // Act
    val result = authRepository.reloadAndCheckVerification()

    // Assert
    assertTrue(result.isSuccess)
    assertFalse(result.getOrThrow())
  }

  @Test
  fun reloadAndCheckVerification_Failure_NoUser() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns null

    // Act
    val result = authRepository.reloadAndCheckVerification()

    // Assert
    assertTrue(result.isFailure)
    assertEquals("No user is currently signed in.", result.exceptionOrNull()?.message)
  }

  @Test
  fun resendVerificationEmail_Success() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns mockUser
    coEvery { mockUser.sendEmailVerification() } returns Tasks.forResult(null)

    // Act
    val result = authRepository.resendVerificationEmail()

    // Assert
    assertTrue(result.isSuccess)
    coVerify { mockUser.sendEmailVerification() }
  }

  @Test
  fun resendVerificationEmail_Failure_NoUser() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns null

    // Act
    val result = authRepository.resendVerificationEmail()

    // Assert
    assertTrue(result.isFailure)
    assertEquals("No user is currently signed in.", result.exceptionOrNull()?.message)
  }

  @Test
  fun signOut_Success() {
    every { mockAuth.signOut() } returns Unit

    val result = authRepository.signOut()

    assertTrue(result.isSuccess)
    verify { mockAuth.signOut() }
  }


  @Test
  fun deleteUser_Success() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns mockUser
    coEvery { mockUser.delete() } returns Tasks.forResult(null)

    // Act
    val result = authRepository.deleteUser()

    // Assert
    assertTrue(result.isSuccess)
    coVerify { mockUser.delete() }
  }

  @Test
  fun deleteUser_Failure() = runTest {
    // Arrange
    val exception = Exception("Delete failed")
    every { mockAuth.currentUser } returns mockUser
    coEvery { mockUser.delete() } returns Tasks.forException(exception)

    // Act
    val result = authRepository.deleteUser()

    // Assert
    assertTrue(result.isFailure)
    assertEquals("Failed to delete user: Delete failed", result.exceptionOrNull()?.message)
  }

  @Test
  fun deleteUser_NoUser() = runTest {
    // Arrange
    every { mockAuth.currentUser } returns null

    // Act
    val result = authRepository.deleteUser()

    // Assert
    // If no user is signed in, deleteUser returns failure (idempotent)
    assertTrue(result.isFailure)
  }
}
