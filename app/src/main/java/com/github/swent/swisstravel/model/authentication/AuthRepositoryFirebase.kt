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
 * A Firebase implementation of [AuthRepository].
 *
 * Retrieves a Google ID token via Credential Manager and authenticates the user with Firebase. Also
 * handles sign-out and credential state clearing.
 *
 * @param auth The [FirebaseAuth] instance for Firebase authentication.
 * @param helper A [GoogleSignInHelper] to extract Google ID token credentials and convert them to
 *   Firebase credentials.
 */
class AuthRepositoryFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
    private val helper: GoogleSignInHelper = DefaultGoogleSignInHelper()
) : AuthRepository {

  fun getGoogleSignInOption(serverClientId: String) =
      GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()

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
  // New function to resend the verification email
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
  // New function to reload the user and check if their email is verified
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
