package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SignInViewModelTest {

  private lateinit var viewModel: SignInViewModel
  private lateinit var mockAuthRepository: AuthRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockContext: Context

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockAuthRepository = mockk()
    mockCredentialManager = mockk()
    mockContext = mockk(relaxed = true)

    every { mockContext.getString(any()) } returns "fake-client-id"

    viewModel = SignInViewModel(mockAuthRepository)
  }

  @Test
  fun `signIn success updates uiState correctly`() = runTest {
    val mockUser = mockk<FirebaseUser>()
    coEvery { mockAuthRepository.signInWithGoogle(any()) } returns Result.success(mockUser)
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } returns mockk(relaxed = true)

    viewModel.signInWithGoogle(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    Assert.assertFalse(uiState.isLoading)
    Assert.assertEquals(mockUser, uiState.user)
    Assert.assertNull(uiState.errorMsg)
    Assert.assertFalse(uiState.signedOut)
  }

  @Test
  fun `signIn failure updates uiState correctly`() = runTest {
    val errorMessage = "Sign-in failed"
    coEvery { mockAuthRepository.signInWithGoogle(any()) } returns
        Result.failure(Exception(errorMessage))
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } returns mockk(relaxed = true)

    viewModel.signInWithGoogle(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    Assert.assertFalse(uiState.isLoading)
    Assert.assertNull(uiState.user)
    Assert.assertEquals(errorMessage, uiState.errorMsg)
    Assert.assertTrue(uiState.signedOut)
  }

  @Test
  fun `signIn cancellation updates uiState correctly`() = runTest {
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws GetCredentialCancellationException()

    viewModel.signInWithGoogle(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    Assert.assertFalse(uiState.isLoading)
    Assert.assertNull(uiState.user)
    Assert.assertEquals("Sign-in cancelled", uiState.errorMsg)
    Assert.assertTrue(uiState.signedOut)
  }

  @Test
  fun `clearErrorMsg clears error message`() = runTest {
    // Given
    val errorMessage = "An error occurred"
    coEvery { mockAuthRepository.signInWithGoogle(any()) } returns
        Result.failure(Exception(errorMessage))
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } returns mockk(relaxed = true)
    viewModel.signInWithGoogle(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    // When
    viewModel.clearErrorMsg()

    // Then
    Assert.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `signIn should not proceed if already loading`() =
      runTest(testDispatcher) {
        val viewModelSpy = spyk(viewModel, recordPrivateCalls = true)

        val loadingState = AuthUiState(isLoading = true)
        every { viewModelSpy.uiState } returns MutableStateFlow(loadingState)

        viewModelSpy.signInWithGoogle(mockContext, mockCredentialManager)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockAuthRepository.signInWithGoogle(any()) }
      }

  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset the main dispatcher to the original one
  }
}
