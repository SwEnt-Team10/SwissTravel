package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SignUpViewModelTest {

  private lateinit var viewModel: SignUpViewModel
  private val mockAuthRepository: AuthRepository = mockk()
  private val mockContext: Context = mockk(relaxed = true)
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModel = SignUpViewModel(repository = mockAuthRepository)
  }

  @Test
  fun `signUpWithEmailPassword success updates state`() =
      runTest(testDispatcher) {
        val mockUser: FirebaseUser = mockk()
        // bring the test to a success value
        coEvery { mockAuthRepository.signUpWithEmailPassword(any(), any(), any(), any()) } returns
            Result.success(mockUser)
        // load the fake viewModel with the credentials
        viewModel.signUpWithEmailPassword(
            "test@example.com", "password", "John", "Doe", mockContext)

        advanceUntilIdle()
        // make sure that the state is updated correctly
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(mockUser, uiState.user)
        assertNull(uiState.errorMsg)
        assertFalse(uiState.signedOut)
      }

  @Test
  fun `signUpWithEmailPassword failure updates state with error`() =
      runTest(testDispatcher) {
        // bring the test to a failure value
        val exception = IllegalStateException("Sign up failed")
        coEvery { mockAuthRepository.signUpWithEmailPassword(any(), any(), any(), any()) } returns
            Result.failure(exception)
        // load up the viewModel with the credentials
        viewModel.signUpWithEmailPassword(
            "test@example.com", "password", "John", "Doe", mockContext)

        advanceUntilIdle()
        // make sure that the sign up fails correctly
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("Sign up failed", uiState.errorMsg)
        assertTrue(uiState.signedOut)
        assertNull(uiState.user)
      }

  @Test
  fun `clearErrorMsg clears the error message`() =
      runTest(testDispatcher) {
        val exception = IllegalStateException("Sign up failed")
        coEvery { mockAuthRepository.signUpWithEmailPassword(any(), any(), any(), any()) } returns
            Result.failure(exception)

        viewModel.signUpWithEmailPassword(
            "test@example.com", "password", "John", "Doe", mockContext)
        // sets up the error message to see whether the Toast appears
        advanceUntilIdle()
        viewModel.clearErrorMsg()
        // make sure the error message is gone
        val uiState = viewModel.uiState.value
        assertNull(uiState.errorMsg)
      }
}
