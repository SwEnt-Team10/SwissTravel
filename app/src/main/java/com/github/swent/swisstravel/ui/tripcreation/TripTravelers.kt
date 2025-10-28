package com.github.swent.swisstravel.ui.tripcreation

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
                  modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Travelers selectors ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                      Counter(
                          label = stringResource(R.string.nb_adults),
                          count = travelers.adults,
                          onIncrement = {
                            travelers = travelers.copy(adults = travelers.adults + 1)
                          },
                          onDecrement = {
                            if (travelers.adults > 1)
                                travelers = travelers.copy(adults = travelers.adults - 1)
                          },
                          enableButton = travelers.adults > 1)

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
      }
}

/** A reusable traveler selector composable that displays adult and children counters. */
@Composable
fun TravelersSelector(
    adults: Int,
    children: Int,
    onAdultsChange: (Int) -> Unit,
    onChildrenChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
    Counter(
        label = stringResource(R.string.nb_adults),
        count = adults,
        onIncrement = { onAdultsChange(adults + 1) },
        onDecrement = { if (adults > 1) onAdultsChange(adults - 1) },
        enableButton = adults > 1)

    Spacer(modifier = Modifier.height(96.dp))

    Counter(
        label = stringResource(R.string.nb_children),
        count = children,
        onIncrement = { onChildrenChange(children + 1) },
        onDecrement = { if (children > 0) onChildrenChange(children - 1) },
        enableButton = children > 0)
  }
}
