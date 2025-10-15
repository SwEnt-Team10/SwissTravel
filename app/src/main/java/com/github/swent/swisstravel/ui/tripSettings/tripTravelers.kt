package com.github.swent.swisstravel.ui.tripSettings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.Counter

/** Test tags for UI tests to identify components. */
object TripTravelersTestTags {
  const val NEXT = "next"
  const val TRIP_TRAVELERS_SCREEN = "tripTravelersScreen"
}

/**
 * Screen where users can set the number of travelers for their trip.
 *
 * @param viewModel ViewModel to handle the trip settings logic.
 * @param onNext Callback to be invoked when the user proceeds to the next step.
 */
@Composable
fun TripTravelersScreen(viewModel: TripSettingsViewModel = viewModel(), onNext: () -> Unit = {}) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  var travelers by remember { mutableStateOf(tripSettings.travelers) }

  LaunchedEffect(travelers) {
    val current = viewModel.tripSettings.value.travelers
    if (current != travelers) {
      viewModel.updateTravelers(travelers.adults, travelers.children)
    }
  }

  Surface(
      modifier = Modifier.fillMaxSize().testTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {

              // --- Title ---
              Text(
                  text = stringResource(R.string.nbTravelers),
                  textAlign = TextAlign.Center,
                  style =
                      MaterialTheme.typography.headlineMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onBackground))

              Spacer(modifier = Modifier.height(16.dp))

              // --- Travelers selectors ---
              Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.Center,
              ) {
                Counter(
                    label = stringResource(R.string.nbAdults),
                    count = travelers.adults,
                    onIncrement = { travelers = travelers.copy(adults = travelers.adults + 1) },
                    onDecrement = {
                      if (travelers.adults > 1)
                          travelers = travelers.copy(adults = travelers.adults - 1)
                    })

                Spacer(modifier = Modifier.height(96.dp))

                Counter(
                    label = stringResource(R.string.nbChildren),
                    count = travelers.children,
                    onIncrement = { travelers = travelers.copy(children = travelers.children + 1) },
                    onDecrement = {
                      if (travelers.children > 0)
                          travelers = travelers.copy(children = travelers.children - 1)
                    })
              }

              Spacer(modifier = Modifier.height(16.dp))

              // --- Done button ---
              Button(
                  onClick = onNext,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(24.dp),
                  modifier = Modifier.testTag(TripTravelersTestTags.NEXT)) {
                    Text(
                        text = stringResource(R.string.next),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium)
                  }
            }
      }
}
