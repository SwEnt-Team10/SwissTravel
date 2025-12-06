package com.github.swent.swisstravel.ui.trip.tripinfos.photos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

object LoadingPhotosTestTags {
    const val LOADING_PHOTOS_SCAFFOLD = "loadingPhotosScaffold"
    const val LOADING_PHOTOS_COLUMN = "loadingPhotosColumn"
    const val LOADING_PHOTOS_INDICATOR = "loadingPhotosIndicator"
}

@Composable
fun LoadingPhotosScreen() {
    Scaffold(
        modifier = Modifier.testTag(LoadingPhotosTestTags.LOADING_PHOTOS_SCAFFOLD)
    ) { pd ->
        Column(
            modifier = Modifier.padding(pd)
                .fillMaxSize()
                .testTag(LoadingPhotosTestTags.LOADING_PHOTOS_COLUMN),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            CircularProgressIndicator(
                modifier = Modifier.testTag(LoadingPhotosTestTags.LOADING_PHOTOS_INDICATOR)
            )
        }
    }
}