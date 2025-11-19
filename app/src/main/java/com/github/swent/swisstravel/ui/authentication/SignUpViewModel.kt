package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignUpStage {
  FILLING_FORM,
  PENDING_VERIFICATION,
}

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
        repository
            .signUpWithEmailPassword(email, password, firstName, lastName)
            .fold(
                onSuccess = { user ->
                  // Sign up successful, email sent. Now wait for verification.
                  _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user, // Keep user info
                        signUpStage = SignUpStage.PENDING_VERIFICATION, // << CHANGE HERE
                        errorMsg = null,
                        signedOut = false)
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

  /**
   * Checks if the current user's email has been verified. This is called when the user clicks the
   * "Done" button.
   */
  fun checkVerificationStatus() {
    if (_uiState.value.isLoading) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      repository
          .reloadAndCheckVerification()
          .fold(
              onSuccess = { isVerified ->
                if (isVerified) {
                  // Success -> Set the flag to trigger navigation.
                  _uiState.update { it.copy(isLoading = false, isEmailVerified = true) }
                } else {
                  // Not verified yet. Inform the user.
                  _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMsg =
                            "Email not verified. Please check your inbox and click the link.")
                  }
                }
              },
              onFailure = { failure ->
                _uiState.update { it.copy(isLoading = false, errorMsg = failure.localizedMessage) }
              })
    }
  }

  /**
   * Resends the verification email to the user. This is called when the user clicks the "Resend
   * Email" button.
   */
  fun resendVerificationEmail() {
    if (_uiState.value.isLoading) return
    viewModelScope.launch {
      // We don't need a full loading indicator, just a confirmation message.
      repository
          .resendVerificationEmail()
          .fold(
              onSuccess = {
                // Use the error message channel to send a confirmation toast.
                _uiState.update { it.copy(errorMsg = "New verification email sent.") }
              },
              onFailure = { failure ->
                _uiState.update { it.copy(errorMsg = failure.localizedMessage) }
              })
    }
  }
}
