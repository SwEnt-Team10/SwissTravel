package com.github.swent.swisstravel.ui.trip.tripinfos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.map.NavigationMapScreen

/** Screen displaying the map for trip information. */
object TripInfoMapTestTags {
  const val TOP_APP_BAR = "TripInfoMap_TopAppBar"
  const val BACK_BUTTON = "TripInfoMap_BackButton"
  const val MAP_CONTAINER = "TripInfoMap_MapContainer"
}
/**
 * Composable function for the Trip Info Map Screen.
 *
 * @param onBack Lambda function to be called when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInfoMapScreen(onBack: () -> Unit = {}, tripInfoViewModel: TripInfoViewModel) {
  var showMap by remember { mutableStateOf(true) }

  LaunchedEffect(showMap) {
    if (!showMap) {
      withFrameNanos {}
      onBack()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = "Trip") },
            navigationIcon = {
              IconButton(
                  onClick = { showMap = false },
                  modifier = Modifier.testTag(TripInfoMapTestTags.BACK_BUTTON)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_my_trips))
                  }
            },
            modifier = Modifier.testTag(TripInfoMapTestTags.TOP_APP_BAR))
      }) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize().padding(padding).testTag(TripInfoMapTestTags.MAP_CONTAINER),
            contentAlignment = Alignment.Center) {
              if (showMap) {
                NavigationMapScreen(tripInfoViewModel.uiState.collectAsState().value.locations)
              }
            }
      }
}
