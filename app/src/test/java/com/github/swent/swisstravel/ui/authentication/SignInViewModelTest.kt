package com.github.swent.swisstravel.ui.authentication

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.github.swent.swisstravel.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    viewModel.signIn(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertEquals(mockUser, uiState.user)
    assertNull(uiState.errorMsg)
    assertFalse(uiState.signedOut)
  }

  @Test
  fun `signIn failure updates uiState correctly`() = runTest {
    val errorMessage = "Sign-in failed"
    coEvery { mockAuthRepository.signInWithGoogle(any()) } returns
        Result.failure(Exception(errorMessage))
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } returns mockk(relaxed = true)

    viewModel.signIn(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertNull(uiState.user)
    assertEquals(errorMessage, uiState.errorMsg)
    assertTrue(uiState.signedOut)
  }

  @Test
  fun `signIn cancellation updates uiState correctly`() = runTest {
    coEvery {
      mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
    } throws GetCredentialCancellationException()

    viewModel.signIn(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertNull(uiState.user)
    assertEquals("Sign-in cancelled", uiState.errorMsg)
    assertTrue(uiState.signedOut)
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
    viewModel.signIn(mockContext, mockCredentialManager)
    testDispatcher.scheduler.advanceUntilIdle()

    // When
    viewModel.clearErrorMsg()

    // Then
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain() // Reset the main dispatcher to the original one
  }
}
