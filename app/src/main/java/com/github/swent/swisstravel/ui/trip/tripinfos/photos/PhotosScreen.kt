package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.add.AddPhotosScreen
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.add.PhotosViewModel

@Composable
fun PhotosScreen(
    onBack: () -> Unit = {},
    photosViewModel: PhotosViewModel,
    tripId: String,
    launchPickerOverride: ((PickVisualMediaRequest) -> Unit)? = null
) {
    LaunchedEffect(tripId) {
        photosViewModel.loadPhotos(tripId)
    }
    val photosUIState by photosViewModel.uiState.collectAsState()
    if (photosViewModel.isOnEditMode()) {
        EditPhotosScreen()
    } else {
        AddPhotosScreen(
            onBack = {
                onBack()
            },
            photosViewModel = photosViewModel,
            tripId = tripId,
            launchPickerOverride = launchPickerOverride
        )
    }
}