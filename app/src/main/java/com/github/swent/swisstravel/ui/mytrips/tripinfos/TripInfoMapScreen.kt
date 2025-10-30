package com.github.swent.swisstravel.ui.mytrips.tripinfos

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.swent.swisstravel.ui.map.NavigationMap

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
fun TripInfoMapScreen(onBack: () -> Unit = {}) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = "Trip") },
            navigationIcon = {
              IconButton(
                  onClick = onBack, modifier = Modifier.testTag(TripInfoMapTestTags.BACK_BUTTON)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                  }
            },
            modifier = Modifier.testTag(TripInfoMapTestTags.TOP_APP_BAR))
      }) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize().padding(padding).testTag(TripInfoMapTestTags.MAP_CONTAINER),
            contentAlignment = Alignment.Center) {
              NavigationMap()
            }
      }
}
