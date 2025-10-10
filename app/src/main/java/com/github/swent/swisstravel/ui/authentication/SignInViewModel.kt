package com.github.swent.swisstravel.ui.authentication

/**
 * ViewModel for the Sign-In view.
 *
 * This file is largely adapted from the bootcamp solution.
 */
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R.string
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Represents the UI state for authentication.
 *
 * @property isLoading Whether an authentication operation is in progress.
 * @property user The currently signed-in [FirebaseUser], or null if not signed in.
 * @property errorMsg An error message to display, or null if there is no error.
 * @property signedOut True if a signed-out operation has completed.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false
)

/**
 * ViewModel for the Sign-In view.
 *
 * @property repository The repository used to perform authentication operations.
 */
class SignInViewModel(private val repository: AuthRepository = AuthRepositoryFirebase()) :
    ViewModel() {
  private var auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  private fun getSignInOptions(context: Context) =
      GetSignInWithGoogleOption.Builder(
              serverClientId = context.getString(string.default_web_client_id))
          .build()

  private fun signInRequest(signInOptions: GetSignInWithGoogleOption) =
      GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()

  private suspend fun getCredential(
      context: Context,
      request: GetCredentialRequest,
      credentialManager: CredentialManager
  ) = credentialManager.getCredential(context, request).credential

  /** Initiates the Google sign-in flow and updates the UI state on success or failure. */
  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      val signInOptions = getSignInOptions(context)
      val signInRequest = signInRequest(signInOptions)

      try {
        // Launch Credential Manager UI safely
        val credential = getCredential(context, signInRequest, credentialManager)
        // Pass the credential to the repository
        when (credential) {
          // Password credential
          // GoogleIdToken credential
          is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
              try {
                // Use googleIdTokenCredential and extract the ID to validate and
                // authenticate on your server.
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                Log.d("SignInViewModel", "Successfully got Google ID token")

                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                _uiState.update { it.copy(isLoading = false) }
              } catch (e: GoogleIdTokenParsingException) {
                Log.e("403", "Received an invalid google id token response", e)
              }
            } else {
              // Catch any unrecognized custom credential type here.
              Log.e("403", "Unexpected type of credential")
            }
          }
          else -> {
            // Catch any unrecognized credential type here.
            Log.e("403", "Unexpected type of credential")
          }
        }
      } catch (e: Exception) {
        Log.e(e.toString(), "Non expected exception when signing in")
      }
    }
  }

  private suspend fun firebaseAuthWithGoogle(idToken: String) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential).await()
  }

  fun signOutWithGoogle(credentialManager: CredentialManager) {
    auth.signOut()
    viewModelScope.launch {
      try {
        credentialManager.clearCredentialState(
            request = androidx.credentials.ClearCredentialStateRequest())
        _uiState.update { it.copy(signedOut = true) }
        Log.d("SignInViewModel", "Successfully signed out")
      } catch (e: Exception) {
        Log.e("SignInViewModel", "Error during sign out: ${e.message}", e)
        _uiState.update { it.copy(errorMsg = "Could not sign out") }
      }
    }
  }
}
