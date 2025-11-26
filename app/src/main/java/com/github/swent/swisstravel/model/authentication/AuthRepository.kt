package com.github.swent.swisstravel.model.authentication

/** This file is largely adapted from the bootcamp solution. */
import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

/** Handles authentication operations such as signing in with Google and signing out. */
interface AuthRepository {

  /**
   * Signs in the user using a Google account through the Credential Manager API.
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /**
   * Signs in the user using an email and password.
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser>

  /**
   * Signs up the user using an email and password.
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signUpWithEmailPassword(
      email: String,
      password: String,
      firstName: String,
      lastName: String
  ): Result<FirebaseUser>
  /**
   * Signs out the currently authenticated user and clears the credential state.
   *
   * @return A [Result] indicating success or failure.
   */
  fun signOut(): Result<Unit>

  /**
   * Reloads the current user's state from the server and checks if their email is verified.
   *
   * @return A [Result] containing `true` if the email is verified, `false` otherwise, or an
   *   exception on failure.
   */
  suspend fun reloadAndCheckVerification(): Result<Boolean>

  /**
   * Resends the email verification link to the currently signed-in user.
   *
   * @return A [Result] indicating success or failure.
   */
  suspend fun resendVerificationEmail(): Result<Unit>

  /**
   * Deletes the currently authenticated user.
   *
   * @return A [Result] indicating success or failure.
   */
  suspend fun deleteUser(): Result<Unit>
}
