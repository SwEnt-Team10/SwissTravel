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
