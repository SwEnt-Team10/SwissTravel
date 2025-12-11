package com.github.swent.swisstravel.ui.profile.selectpinnedpictures

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.github.swent.swisstravel.model.image.Image
import com.github.swent.swisstravel.model.image.ImageHelper
import com.github.swent.swisstravel.model.image.ImageRepository
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

/**
 * Test class for Select Pinned Pictures View Model. Some of these were made with the help of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectPinnedPicturesViewModelTest {

  private lateinit var viewModel: SelectPinnedPicturesViewModel
  private val testDispatcher = StandardTestDispatcher()

  @MockK private lateinit var mockUserRepository: UserRepository
  @MockK private lateinit var mockImageRepository: ImageRepository
  @MockK private lateinit var mockUser: User
  @MockK private lateinit var mockBitmap: Bitmap
  @MockK private lateinit var mockContext: Context
  @MockK private lateinit var mockUri: Uri

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    Dispatchers.setMain(testDispatcher)

    // Mock the singleton object ImageHelper to avoid Android dependencies (Base64/BitmapFactory)
    mockkObject(ImageHelper)

    // Default User setup
    every { mockUser.uid } returns "testUser"
    every { mockUser.pinnedPicturesUids } returns emptyList()
    coEvery { mockUserRepository.getCurrentUser() } returns mockUser
  }

  @After
  fun tearDown() {
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun initLoadsExistingImagesSuccessfully() =
      runTest(testDispatcher) {
        // Given
        val imageUid = "existing-image-1"
        val base64 = "base64Data"
        val imageObj = Image(uid = imageUid, ownerId = "testUser", base64 = base64)

        every { mockUser.pinnedPicturesUids } returns listOf(imageUid)
        coEvery { mockImageRepository.getImage(imageUid) } returns imageObj
        coEvery { ImageHelper.base64ToBitmap(base64) } returns mockBitmap

        // When
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.images.size)
        assertEquals(imageUid, state.images[0].uid)
        assertEquals(mockBitmap, state.images[0].bitmap)
        assertNull(state.errorMsg)
      }

  @Test
  fun initHandlesErrorsGracefullyDuringLoading() =
      runTest(testDispatcher) {
        // Given
        every { mockUser.pinnedPicturesUids } throws Exception("Network error")

        // When
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.images.isEmpty())
        assertTrue(state.errorMsg!!.contains("Network error"))
      }

  @Test
  fun addNewImagesProcessesUrisAndAddsToState() =
      runTest(testDispatcher) {
        // Given
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        val compressedBase64 = "compressedBase64"
        coEvery { ImageHelper.uriCompressedToBase64(mockContext, mockUri) } returns compressedBase64
        coEvery { ImageHelper.base64ToBitmap(compressedBase64) } returns mockBitmap

        // When
        viewModel.addNewImages(mockContext, listOf(mockUri))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.images.size)
        assertEquals(null, state.images[0].uid) // New images have null UID initially
        assertEquals(compressedBase64, state.images[0].base64String)
        assertEquals(mockBitmap, state.images[0].bitmap)
      }

  @Test
  fun removeImageRemovesImageFromStateAndMarksExistingUidForDeletion() =
      runTest(testDispatcher) {
        // Given
        val existingUid = "existing-uid"
        val existingImage = PinnedImage(uid = existingUid, bitmap = mockBitmap)
        // We simulate a state where an image is already loaded
        every { mockUser.pinnedPicturesUids } returns listOf(existingUid)
        coEvery { mockImageRepository.getImage(existingUid) } returns
            Image(existingUid, "user", "b64")
        coEvery { ImageHelper.base64ToBitmap("b64") } returns mockBitmap

        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // Pre-check
        assertEquals(1, viewModel.uiState.value.images.size)

        // When
        viewModel.removeImage(0)

        // Then
        assertTrue(viewModel.uiState.value.images.isEmpty())
      }

  @Test
  fun savePicturesUploadsNewImagesAndDeletesRemovedOnes() =
      runTest(testDispatcher) {
        // Given
        // 1. One existing image that we will remove
        val imageToRemoveUid = "to-delete"
        every { mockUser.pinnedPicturesUids } returns listOf(imageToRemoveUid)
        coEvery { mockImageRepository.getImage(imageToRemoveUid) } returns
            Image(imageToRemoveUid, "user", "oldB64")
        coEvery { ImageHelper.base64ToBitmap("oldB64") } returns mockBitmap

        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // 2. Add a new image
        val newBase64 = "newBase64"
        coEvery { ImageHelper.uriCompressedToBase64(mockContext, mockUri) } returns newBase64
        coEvery { ImageHelper.base64ToBitmap(newBase64) } returns mockBitmap

        viewModel.addNewImages(mockContext, listOf(mockUri))
        advanceUntilIdle()

        // 3. Remove the old image
        // State now has: [ExistingImage, NewImage]. Remove index 0.
        viewModel.removeImage(0)

        // Setup save mocks
        val newImageUid = "new-image-uid"
        coEvery { mockImageRepository.addImage(newBase64) } returns newImageUid
        coEvery { mockImageRepository.deleteImage(imageToRemoveUid) } returns Unit
        coEvery {
          mockUserRepository.updateUser(any(), any(), any(), any(), any(), any(), any())
        } returns Unit

        val onSuccess = mockk<() -> Unit>(relaxed = true)

        // When
        viewModel.savePictures(onSuccess)
        advanceUntilIdle()

        // Then
        // 1. Verify new image uploaded
        coVerify { mockImageRepository.addImage(newBase64) }

        // 2. Verify old image deleted
        coVerify { mockImageRepository.deleteImage(imageToRemoveUid) }

        // 3. Verify user profile updated with only the new UID
        val uidsSlot = slot<List<String>>()
        coVerify {
          mockUserRepository.updateUser(uid = "testUser", pinnedPicturesUids = capture(uidsSlot))
        }
        assertEquals(listOf(newImageUid), uidsSlot.captured)

        // 4. Verify callback
        verify { onSuccess() }
      }

  @Test
  fun savePicturesHandlesErrorsAndUpdatesErrorMsg() =
      runTest(testDispatcher) {
        // Given
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        coEvery {
          mockUserRepository.updateUser(any(), any(), any(), any(), any(), any(), any())
        } throws Exception("Save failed")

        // When
        viewModel.savePictures {}
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.errorMsg!!.contains("Save failed"))
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun clearErrorMsgResetsErrorMessage() =
      runTest(testDispatcher) {
        // Given
        every { mockUser.pinnedPicturesUids } throws Exception("Fail load")
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMsg != null)

        // When
        viewModel.clearErrorMsg()

        // Then
        assertNull(viewModel.uiState.value.errorMsg)
      }
}
