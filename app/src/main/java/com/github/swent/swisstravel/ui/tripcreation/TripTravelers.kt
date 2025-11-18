package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.TravelersSelector
import com.github.swent.swisstravel.ui.navigation.TopBar

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
fun TripTravelersScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  var travelers by remember { mutableStateOf(tripSettings.travelers) }

  LaunchedEffect(travelers) {
    val current = viewModel.tripSettings.value.travelers
    if (current != travelers) {
      viewModel.updateTravelers(travelers.adults, travelers.children)
    }
  }
  Scaffold(
      modifier = Modifier.testTag(TripTravelersTestTags.TRIP_TRAVELERS_SCREEN),
      topBar = { TopBar(onClick = { onPrevious() }) }) { pd ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(pd),
            color = MaterialTheme.colorScheme.background) {
              Column(
                  modifier =
                      Modifier.fillMaxSize()
                          .padding(
                              horizontal =
                                  dimensionResource(R.dimen.trip_travelers_padding_horizontal),
                              vertical =
                                  dimensionResource(R.dimen.trip_travelers_padding_vertical)),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.SpaceBetween) {

                    // --- Title ---
                    Text(
                        text = stringResource(R.string.nb_travelers),
                        textAlign = TextAlign.Center,
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground))

                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

                    // --- Travelers selectors ---
                    TravelersSelector(
                        adults = travelers.adults,
                        children = travelers.children,
                        onAdultsChange = { travelers = travelers.copy(adults = it) },
                        onChildrenChange = { travelers = travelers.copy(children = it) })

                    // --- Done button ---
                    Button(
                        onClick = onNext,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary),
                        shape =
                            RoundedCornerShape(
                                dimensionResource(R.dimen.trip_travelers_button_radius)),
                        modifier = Modifier.testTag(TripTravelersTestTags.NEXT)) {
                          Text(
                              text = stringResource(R.string.next),
                              color = MaterialTheme.colorScheme.onPrimary,
                              style = MaterialTheme.typography.titleMedium)
                        }
                  }
            }
      }
}
