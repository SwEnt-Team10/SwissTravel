package com.github.swent.swisstravel.model.authentication

/** This file is largely adapted from the bootcamp solution. */
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * A Firebase implementation of the [AuthRepository] interface.
 *
 * This class handles all authentication logic using the Firebase Authentication SDK. It supports
 * signing in with Google, signing in with email/password, creating new users, and managing email
 * verification states.
 *
 * @param auth The [FirebaseAuth] instance used for all Firebase authentication operations.
 * @param helper A [GoogleSignInHelper] to extract Google ID token credentials and convert them to
 *   Firebase credentials.
 */
class AuthRepositoryFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
    private val helper: GoogleSignInHelper = DefaultGoogleSignInHelper()
) : AuthRepository {

  /**
   * Constructs a [GetSignInWithGoogleOption] to be used with the Credential Manager API.
   *
   * This is a helper function specific to the Firebase implementation and is not part of the
   * [AuthRepository] interface.
   *
   * @param serverClientId The Web client ID from the Google Cloud console.
   * @return A [GetSignInWithGoogleOption] configured for signing in with Google.
   */
  fun getGoogleSignInOption(serverClientId: String) =
      GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()

  /**
   * Signs a user in with Google using a credential from the Credential Manager.
   *
   * It extracts the ID token from the [Credential], converts it to a Firebase credential, and signs
   * the user into Firebase Authentication.
   *
   * @param credential The [Credential] object, expected to be a Google ID Token credential.
   * @return A [Result] containing the [FirebaseUser] on success, or an exception on failure.
   */
  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val idToken = helper.extractIdTokenCredential(credential.data).idToken
        val firebaseCred = helper.toFirebaseCredential(idToken)

        // Sign in with Firebase
        val user =
            auth.signInWithCredential(firebaseCred).await().user
                ?: return Result.failure(
                    IllegalStateException("Login failed: Could not retrieve user information"))
        return Result.success(user)
      } else {
        return Result.failure(
            IllegalStateException("Login failed: Credential is not of type Google ID"))
      }
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Login failed: ${e.localizedMessage ?: "Unexpected error"}"))
    }
  }

  /**
   * Signs a user in with their email and password.
   *
   * @param email The user's email address.
   * @param password The user's password.
   * @return A [Result] containing the [FirebaseUser] on success, or an exception on failure.
   */
  override suspend fun signInWithEmailPassword(
      email: String,
      password: String
  ): Result<FirebaseUser> {
    return try {
      val user =
          auth.signInWithEmailAndPassword(email, password).await().user
              ?: return Result.failure(
                  IllegalStateException("Login failed: Could not retrieve user information"))
      Result.success(user)
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Login failed: ${e.localizedMessage ?: "Unexpected error"}"))
    }
  }

  /**
   * Creates a new user account with the given email, password, and name.
   *
   * After creating the user, it updates their Firebase profile with the provided first and last
   * name and sends a verification email.
   *
   * @param email The new user's email address.
   * @param password The new user's chosen password.
   * @param firstName The user's first name.
   * @param lastName The user's last name.
   * @return A [Result] containing the newly created [FirebaseUser] on success, or an exception on
   *   failure.
   */
  override suspend fun signUpWithEmailPassword(
      email: String,
      password: String,
      firstName: String,
      lastName: String
  ): Result<FirebaseUser> {
    return try {
      val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
      val user =
          userCredential.user
              ?: return Result.failure(
                  IllegalStateException("Sign up failed: Could not retrieve user information"))

      // After creation, update the user's profile with their name
      val profileUpdates = userProfileChangeRequest { displayName = "$firstName $lastName" }
      user.updateProfile(profileUpdates).await()
      user.sendEmailVerification().await()

      // In a real application, you would also create a document in your Firestore 'users'
      // collection here to store additional information like first name, last name, etc.

      Result.success(user)
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Sign up failed: ${e.localizedMessage ?: "Unexpected error"}"))
    }
  }

  /**
   * Resends the verification email to the currently signed-in user.
   *
   * Fails if no user is currently authenticated.
   *
   * @return A [Result] indicating success ([Unit]) or an exception on failure.
   */
  override suspend fun resendVerificationEmail(): Result<Unit> {
    return try {
      val user = auth.currentUser
      if (user == null) {
        Result.failure(IllegalStateException("No user is currently signed in."))
      } else {
        user.sendEmailVerification().await()
        Result.success(Unit)
      }
    } catch (e: Exception) {
      Result.failure(IllegalStateException("Failed to resend email: ${e.localizedMessage}"))
    }
  }

  /**
   * Checks the verification status of the current user's email.
   *
   * It is crucial that this method reloads the user's state from the Firebase backend before
   * checking the `isEmailVerified` property, as the local token may be stale.
   *
   * @return A [Result] containing `true` if the email is verified and `false` otherwise. Fails if
   *   no user is signed in.
   */
  override suspend fun reloadAndCheckVerification(): Result<Boolean> {
    return try {
      val user = auth.currentUser
      if (user == null) {
        Result.failure(IllegalStateException("No user is currently signed in."))
      } else {
        // IMPORTANT: You must reload the user's state from Firebase
        user.reload().await()
        Result.success(user.isEmailVerified)
      }
    } catch (e: Exception) {
      Result.failure(IllegalStateException("Failed to check verification: ${e.localizedMessage}"))
    }
  }

  /**
   * Signs out the currently authenticated user from Firebase.
   *
   * @return A [Result] indicating success ([Unit]) or an exception on failure.
   */
  override fun signOut(): Result<Unit> {
    return try {
      // Firebase sign out
      auth.signOut()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Logout failed: ${e.localizedMessage ?: "Unexpected error."}"))
    }
  }
}
