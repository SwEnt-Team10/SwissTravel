package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R.string
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for authentication screens.
 *
 * @property isLoading `true` if an authentication operation is in progress.
 * @property user The currently signed-in [FirebaseUser], or `null` if not authenticated.
 * @property errorMsg A specific error message to display, or `null` if there's no error.
 * @property signedOut `true` if the user is not authenticated or a sign-out has just occurred. This
 *   can be used to trigger navigation events.
 * @property signUpStage The current stage of the sign-up process.
 * @property isEmailVerified `true` if the user's email is verified, `false` otherwise.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
    val signUpStage: SignUpStage = SignUpStage.FILLING_FORM,
    val isEmailVerified: Boolean = false
)

/**
 * An abstract base ViewModel that provides common authentication functionality.
 *
 * This class encapsulates shared logic for both sign-in and sign-up processes, including UI state
 * management (`AuthUiState`) and the Google Sign-In flow. It is designed to be extended by concrete
 * ViewModels like [SignInViewModel] and [SignUpViewModel].
 *
 * @param repository The [AuthRepository] implementation used for authentication operations.
 */
abstract class BaseAuthViewModel(
    protected val repository: AuthRepository = AuthRepositoryFirebase()
) : ViewModel() {

  protected val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState

  /** Clears any error message from the UI state. */
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

  /**
   * Initiates the Google sign-in flow.
   *
   * Google users are automatically verified, so we immediately set [isEmailVerified] to true and
   * reset the [signUpStage].
   */
  fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      // Helper functions assumed to be in your BaseAuthViewModel or class
      val signInOptions = getSignInOptions(context)
      val signInRequest = signInRequest(signInOptions)

      try {
        val credential = getCredential(context, signInRequest, credentialManager)

        repository
            .signInWithGoogle(credential)
            .fold(
                onSuccess = { user ->
                  _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        errorMsg = null,
                        signedOut = false,
                        // 1. Google accounts are always verified
                        isEmailVerified = true,
                        // 2. Reset stage to ensure UI doesn't show the "Check Email" screen
                        signUpStage = SignUpStage.FILLING_FORM)
                  }
                },
                onFailure = { failure ->
                  _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMsg = failure.localizedMessage,
                        signedOut = true,
                        user = null)
                  }
                })
      } catch (e: GetCredentialCancellationException) {
        _uiState.update {
          it.copy(isLoading = false, errorMsg = "Sign-in cancelled", signedOut = true, user = null)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Unexpected error: ${e.localizedMessage}",
              signedOut = true,
              user = null)
        }
      }
    }
  }
}

/**
 * The ViewModel for the Sign-In screen.
 *
 * This class extends [BaseAuthViewModel] and provides the specific logic for signing in with an
 * email and password. It interacts with the [AuthRepository] to perform the sign-in operation and
 * updates the [AuthUiState] accordingly.
 *
 * @param repository The [AuthRepository] implementation to use for authentication.
 */
class SignInViewModel(repository: AuthRepository = AuthRepositoryFirebase()) :
    BaseAuthViewModel(repository) {

  /**
   * Initiates the email and password sign-in flow.
   *
   * It updates the UI state to indicate loading, then calls the repository to perform the sign-in.
   * The UI state is updated with the user information on success or an error message on failure.
   *
   * @param email The user's email.
   * @param password The user's password.
   * @param context The application context, used for retrieving error string resources.
   */
  fun signInWithEmailPassword(email: String, password: String, context: Context) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      try {
        repository.signInWithEmailPassword(email, password).fold({ user ->
          if (user.isEmailVerified) {
            _uiState.update {
              it.copy(isLoading = false, user = user, errorMsg = null, signedOut = false)
            }
          } else {
            repository.signOut()
            _uiState.update {
              it.copy(
                  isLoading = false,
                  errorMsg = "Email not verified. Please verify your email.",
                  signedOut = true,
                  user = null)
            }
          }
        }) { failure ->
          _uiState.update {
            it.copy(
                isLoading = false,
                errorMsg = failure.localizedMessage,
                signedOut = true,
                user = null)
          }
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = context.getString(string.unexpected_error, e.localizedMessage),
              signedOut = true,
              user = null)
        }
      }
    }
  }
}
