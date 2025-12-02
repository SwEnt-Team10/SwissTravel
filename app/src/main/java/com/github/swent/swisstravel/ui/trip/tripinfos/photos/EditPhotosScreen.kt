package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.add.PhotosUIState
import com.github.swent.swisstravel.ui.trip.tripinfos.photos.add.PhotosViewModel

@Composable
fun EditPhotosScreen(
    uiState: PhotosUIState,
    photosViewModel: PhotosViewModel
) {
    Scaffold(
        topBar = {
            EditTopBar(
                photosViewModel = photosViewModel
            )
        }
    ) {
        pd ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(integerResource(R.integer.images_on_grid)),
            modifier = Modifier.padding(pd)
        ) {
            itemsIndexed(uiState.listUri) { index, uri ->
                Button(
                    onClick = {}
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(
    photosViewModel: PhotosViewModel
) {
    TopAppBar(
        title = {
            Text(
                text = "Select photos"
            )
        },
        navigationIcon = {
            CancelButton(
                photosViewModel
            )
        }
    )
}

@Composable
private fun CancelButton(
    photosViewModel: PhotosViewModel
) {
    IconButton(
        onClick = {
            photosViewModel.switchOnEditMode()
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = "Cancel Edit",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}