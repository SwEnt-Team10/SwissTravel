package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Sign-Up screen.
 *
 * This ViewModel handles the business logic for user registration, including sign-up with
 * email/password and Google. It communicates with the provided [AuthRepository] to perform
 * authentication operations and updates the UI state accordingly.
 *
 * @param repository The authentication repository used to interact with the authentication service.
 *   Defaults to an instance of [AuthRepositoryFirebase].
 */
class SignUpViewModel(repository: AuthRepository = AuthRepositoryFirebase()) :
    BaseAuthViewModel(repository) {

  /**
   * Initiates the sign-up process with an email, password, and user's name.
   *
   * It sets the UI state to loading, calls the repository's `signUpWithEmailPassword` method, and
   * updates the UI state with the result (either the signed-in user or an error message).
   *
   * @param email The user's email address.
   * @param password The user's chosen password.
   * @param firstName The user's first name.
   * @param lastName The user's last name.
   * @param context The Android context, used for retrieving string resources for error messages.
   */
  fun signUpWithEmailPassword(
      email: String,
      password: String,
      firstName: String,
      lastName: String,
      context: Context
  ) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      try {
        repository.signUpWithEmailPassword(email, password, firstName, lastName).fold({ user ->
          _uiState.update {
            it.copy(isLoading = false, user = user, errorMsg = null, signedOut = false)
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
        // Unexpected errors
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = context.getString(R.string.unexpected_error, e.localizedMessage),
              signedOut = true,
              user = null)
        }
      }
    }
  }
}
