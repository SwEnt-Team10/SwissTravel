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

/** Test class for Select Pinned Pictures View Model. */
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

    // Mock the singleton object ImageHelper
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
  fun initSkipsFailedImages() =
      runTest(testDispatcher) {
        // Given: User has 2 images, one fails to load
        val uidSuccess = "success-img"
        val uidFail = "fail-img"
        val imageObj = Image(uid = uidSuccess, ownerId = "testUser", base64 = "base64")

        every { mockUser.pinnedPicturesUids } returns listOf(uidSuccess, uidFail)

        // Mock success
        coEvery { mockImageRepository.getImage(uidSuccess) } returns imageObj
        coEvery { ImageHelper.base64ToBitmap("base64") } returns mockBitmap

        // Mock failure
        coEvery { mockImageRepository.getImage(uidFail) } throws Exception("Download failed")

        // When
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Should contain only the successfully loaded image", 1, state.images.size)
        assertEquals(uidSuccess, state.images[0].uid)
      }

  @Test
  fun initHandlesErrorsGracefullyDuringUserFetch() =
      runTest(testDispatcher) {
        // Given
        coEvery { mockUserRepository.getCurrentUser() } throws Exception("Network error")

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
  fun toggleSelectionUpdatesSelectedIndices() =
      runTest(testDispatcher) {
        // Given
        val existingUid = "existing-uid"
        every { mockUser.pinnedPicturesUids } returns listOf(existingUid)
        coEvery { mockImageRepository.getImage(existingUid) } returns
            Image(existingUid, "user", "b64")
        coEvery { ImageHelper.base64ToBitmap("b64") } returns mockBitmap

        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // When: Select index 0
        viewModel.toggleSelection(0)

        // Then
        assertTrue(viewModel.uiState.value.selectedIndices.contains(0))

        // When: Toggle index 0 again
        viewModel.toggleSelection(0)

        // Then
        assertFalse(viewModel.uiState.value.selectedIndices.contains(0))
      }

  @Test
  fun removeSelectedImagesRemovesFromStateAndCallsDeleteOnRepo() =
      runTest(testDispatcher) {
        // Given
        val existingUid = "existing-uid"
        every { mockUser.pinnedPicturesUids } returns listOf(existingUid)
        coEvery { mockImageRepository.getImage(existingUid) } returns
            Image(existingUid, "user", "b64")
        coEvery { ImageHelper.base64ToBitmap("b64") } returns mockBitmap
        coEvery { mockImageRepository.deleteImage(existingUid) } returns Unit

        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // Select the image
        viewModel.toggleSelection(0)

        // When
        viewModel.removeSelectedImages()
        advanceUntilIdle()

        // Then
        assertTrue("Images list should be empty", viewModel.uiState.value.images.isEmpty())
        assertTrue(
            "Selection list should be empty", viewModel.uiState.value.selectedIndices.isEmpty())

        // Verify delete was called immediately
        coVerify { mockImageRepository.deleteImage(existingUid) }
      }

  @Test
  fun savePicturesUploadsNewImagesUpdatesProfile() =
      runTest(testDispatcher) {
        // Given
        viewModel = SelectPinnedPicturesViewModel(mockUserRepository, mockImageRepository)
        advanceUntilIdle()

        // Add a new image
        val newBase64 = "newBase64"
        coEvery { ImageHelper.uriCompressedToBase64(mockContext, mockUri) } returns newBase64
        coEvery { ImageHelper.base64ToBitmap(newBase64) } returns mockBitmap

        viewModel.addNewImages(mockContext, listOf(mockUri))
        advanceUntilIdle()

        // Setup save mocks
        val newImageUid = "new-image-uid"
        coEvery { mockImageRepository.addImage(newBase64) } returns newImageUid
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

        // 2. Verify user profile updated with the new UID
        val uidsSlot = slot<List<String>>()
        coVerify {
          mockUserRepository.updateUser(uid = "testUser", pinnedPicturesUids = capture(uidsSlot))
        }
        assertEquals(listOf(newImageUid), uidsSlot.captured)

        // 3. Verify callback
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
}
