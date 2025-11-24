package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.github.swent.swisstravel.model.authentication.AuthRepositoryFirebase
import com.google.firebase.auth.FirebaseUser
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
  var currentUser: FirebaseUser? = null

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
                  // We only update the stage to show the "Check your email" screen.
                  _uiState.update {
                    it.copy(
                        isLoading = false,
                        signUpStage = SignUpStage.PENDING_VERIFICATION,
                        errorMsg = null,
                        // Ensure the UI still treats them as not fully logged in
                        user = null,
                        signedOut = true)
                  }
                  // currentUser = user
                },
                onFailure = { failure ->
                  // If sign up fails, try to sign in.
                  // This handles the case where the user already exists but is not verified.
                  repository
                      .signInWithEmailPassword(email, password)
                      .fold(
                          onSuccess = { user ->
                            if (user.isEmailVerified) {
                              _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMsg = "Account already exists. Please sign in.",
                                    signedOut = true,
                                    user = null)
                              }
                              repository.signOut()
                            } else {
                              // User exists but is not verified. Delete the user and retry sign up.
                              viewModelScope.launch {
                                repository
                                    .deleteUser()
                                    .fold(
                                        onSuccess = {
                                          // Reset loading state to allow the recursive call to
                                          // proceed
                                          _uiState.update { it.copy(isLoading = false) }
                                          // Retry sign up
                                          signUpWithEmailPassword(
                                              email, password, firstName, lastName, context)
                                        },
                                        onFailure = { deleteFailure ->
                                          _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                errorMsg =
                                                    "Failed to reset account: ${deleteFailure.localizedMessage}",
                                                signedOut = true,
                                                user = null)
                                          }
                                        })
                              }
                            }
                          },
                          onFailure = {
                            // If sign in also fails, show the original error
                            _uiState.update {
                              it.copy(
                                  isLoading = false,
                                  errorMsg = failure.localizedMessage,
                                  signedOut = true,
                                  user = null)
                            }
                          })
                })
      } catch (e: Exception) {
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
                  // SUCCESS: Now we can populate the user and sign them in
                  // You might need to fetch the current user from the repo here
                  // Assuming your repo has this

                  _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmailVerified = true,
                        user = currentUser, // Populate user now
                        signedOut = false // Allow access to app
                        )
                  }
                } else {
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
