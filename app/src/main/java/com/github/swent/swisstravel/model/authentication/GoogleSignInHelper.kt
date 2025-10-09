package com.github.swent.swisstravel.model.authentication

/** This file is largely adapted from the bootcamp solution. */
import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Interface for extracting Google ID token credentials and converting them to Firebase credentials.
 *
 * Enables unit testing without the need to statically mock SDK methods.
 */
interface GoogleSignInHelper {

  /**
   * Extracts a [GoogleIdTokenCredential] from the given credential data.
   *
   * @param bundle Credential data from the Google sign-in response.
   * @return A [GoogleIdTokenCredential] containing the user's ID token.
   */
  fun extractIdTokenCredential(bundle: Bundle): GoogleIdTokenCredential

  /**
   * Creates a Firebase [AuthCredential] from a Google ID token.
   *
   * @param idToken The ID token returned by Google sign-in.
   * @return A [AuthCredential] used to sign in with FirebaseAuth.
   */
  fun toFirebaseCredential(idToken: String): AuthCredential
}

/** Implementation of [GoogleSignInHelper] that directly calls Google SDK methods. */
class DefaultGoogleSignInHelper : GoogleSignInHelper {
  override fun extractIdTokenCredential(bundle: Bundle): GoogleIdTokenCredential =
      GoogleIdTokenCredential.createFrom(bundle)

  override fun toFirebaseCredential(idToken: String): AuthCredential =
      GoogleAuthProvider.getCredential(idToken, null)
}
