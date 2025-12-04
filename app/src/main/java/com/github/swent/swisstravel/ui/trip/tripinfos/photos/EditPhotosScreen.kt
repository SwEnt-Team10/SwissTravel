package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R

@Composable
fun EditPhotosScreen(
    photosViewModel: PhotosViewModel = viewModel(),
    onCancel: () -> Unit = {},
    tripId: String
) {
    LaunchedEffect(tripId) { photosViewModel.loadPhotos(tripId) }
    val uiState by photosViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            EditTopBar(
                onCancel =
                    {
                        photosViewModel.savePhotos(tripId)
                        onCancel()
                    }
            )
        },
        bottomBar = {
            EditBottomBar(
                onRemove = {
                    photosViewModel.removePhotos()
                }
            )
        }
    ) {
        pd ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(integerResource(R.integer.images_on_grid)),
            modifier = Modifier.padding(pd)
        ) {
            itemsIndexed(uiState.listUri) { index, uri ->
                Box(
                    modifier = Modifier.clickable {
                        photosViewModel.selectToRemove(index)
                    }
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null
                    )

                    if (uiState.uriSelected.contains(index)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(
    onCancel: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = "Select photos"
            )
        },
        navigationIcon = {
            CancelButton(
                onCancel = onCancel
            )
        }
    )
}

@Composable
private fun CancelButton(
    onCancel: () -> Unit = {}
) {
    IconButton(
        onClick = {
            onCancel()
        }
    ) {
        Icon(
            imageVector = Icons.Filled.Cancel,
            contentDescription = "Cancel Edit",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun EditBottomBar(
    onRemove: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                onRemove()
            }
        ) {
            Text(
                text = "Remove"
            )
        }
    }
}