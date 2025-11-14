package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The ViewModel for the Sign-Up screen.
 *
 * This class extends [BaseAuthViewModel] and provides the specific logic for signing up with an
 * email and password. It interacts with the [AuthRepository] to perform the sign-up operation and
 * updates the [AuthUiState] accordingly.
 *
 * @param repository The [AuthRepository] implementation to use for authentication.
 */
class SignUpViewModel(repository: AuthRepository = AuthRepositoryFirebase()) :
    BaseAuthViewModel(repository) {

  /**
   * Initiates the email and password sign-up flow.
   *
   * It updates the UI state to indicate loading, then calls the repository to perform the sign-up.
   * The UI state is updated with the user information on success or an error message on failure.
   *
   * @param email The user's email.
   * @param password The user's password.
   * @param firstName The user's first name.
   * @param lastName The user's last name.
   * @param context The application context, used for retrieving error string resources.
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
