package com.github.swent.swisstravel.ui.authentication

import androidx.credentials.Credential
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.mockk

/**
 * A mock implementation of the [com.github.swent.swisstravel.model.authentication.AuthRepository]
 * for testing purposes. This allows us to define the behavior of authentication functions during
 * tests.
 */
class MockAuthRepository : AuthRepository {

  // These variables allow us to control the outcome of the mock functions from our tests.
  private val mockUser: FirebaseUser = mockk(relaxed = true)

  // Results for each repository function
  var signUpResult: Result<FirebaseUser> = Result.success(mockUser)
  var signInEmailResult: Result<FirebaseUser> = Result.success(mockUser)
  var signInGoogleResult: Result<FirebaseUser> = Result.success(mockUser)
  var signOutResult: Result<Unit> = Result.success(Unit)
  var checkVerificationResult: Result<Boolean> = Result.success(false) // Default to not verified
  var resendEmailResult: Result<Unit> = Result.success(Unit)

  var deleteUserResult: Result<Unit> = Result.success(Unit)

  override suspend fun signUpWithEmailPassword(
      email: String,
      password: String,
      firstName: String,
      lastName: String
  ): Result<FirebaseUser> {
    return signUpResult
  }

  override suspend fun reloadAndCheckVerification(): Result<Boolean> {
    return checkVerificationResult
  }

  override suspend fun resendVerificationEmail(): Result<Unit> {
    return resendEmailResult
  }

  override suspend fun deleteUser(): Result<Unit> {
    return deleteUserResult
  }

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return signInGoogleResult
  }

  override suspend fun signInWithEmailPassword(
      email: String,
      password: String
  ): Result<FirebaseUser> {
    return signInEmailResult
  }

  override fun signOut(): Result<Unit> {
    return signOutResult
  }
}
