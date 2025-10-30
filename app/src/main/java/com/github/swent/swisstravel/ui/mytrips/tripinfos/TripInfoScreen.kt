package com.github.swent.swisstravel.ui.mytrips.tripinfos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
/**
 * Screen that displays the information of a specific trip
 *
 * @param uid the unique identifier of the trip
 * @param tripInfoViewModel the view model that holds the trip information state
 * @param onPastTrips lambda to be called when navigating back to past trips
 * @param onFullscreenClick lambda to be called when the fullscreen button is clicked
 * @param onEditTrip lambda to be called when the edit trip button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInfoScreen(
    uid: String?,
    tripInfoViewModel: TripInfoViewModel = viewModel(),
    onPastTrips: () -> Unit = {},
    onFullscreenClick: () -> Unit = {},
    onEditTrip: () -> Unit = {}
) {
  LaunchedEffect(uid) { tripInfoViewModel.loadTripInfo(uid) }

  val tripInfoUIState by tripInfoViewModel.uiState.collectAsState()
  val errorMsg = tripInfoUIState.errorMsg

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      tripInfoViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
          TopAppBar(
              title = {
                Text(
                    text = tripInfoUIState.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
              },
              navigationIcon = {
                IconButton(onClick = { onPastTrips() }, modifier = Modifier.testTag("back_button")) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Back to My Trips",
                      tint = MaterialTheme.colorScheme.onBackground)
                }
              },
              actions = {
                  IconButton(
                      onClick = { onEditTrip() },
                      modifier = Modifier.testTag("edit_button")
                  ) {
                      Icon(
                          imageVector = Icons.Filled.Edit,
                          contentDescription = "Edit trip",
                          tint = MaterialTheme.colorScheme.onBackground
                      )
                  }
              })
      }) { pd ->
        Column(
            modifier = Modifier.padding(pd).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
              val location1 = tripInfoUIState.locations.firstOrNull()?.name.orEmpty()
              if (location1.isNotBlank()) {
                Text(
                    text = location1,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("location1_text"))
              }
              Card(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 20.dp)
                      .height(270.dp),
                  shape = RoundedCornerShape(12.dp),
                  elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                  TripInfoZoomableMap(onFullscreenClick = onFullscreenClick)
                  }
            }
      }
}
