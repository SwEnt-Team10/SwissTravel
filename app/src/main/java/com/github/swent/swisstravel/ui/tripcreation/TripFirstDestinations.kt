package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.map.MySwitzerlandLocationRepository
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModelContract
import com.github.swent.swisstravel.ui.geocoding.DestinationTextFieldViewModel
import com.github.swent.swisstravel.ui.geocoding.DestinationTextFieldViewModelFactory
import com.github.swent.swisstravel.ui.geocoding.LocationAutocompleteTextField
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.ADD_FIRST_DESTINATION
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.FIRST_DESTINATIONS_TITLE
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.NEXT_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON

/** Object containing test tags for the [FirstDestinationScreen] composable. */
object TripFirstDestinationsTestTags {
  const val FIRST_DESTINATIONS_TITLE = "first_destinations_title"
  const val ADD_FIRST_DESTINATION = "add_first_destination"
  const val NEXT_BUTTON = "next_button"
  const val RETURN_BUTTON = "return_button"
}

private const val MAX_DESTINATIONS = 24

/**
 * Screen for entering the first destinations of a trip.
 *
 * @param viewModel The ViewModel managing the trip settings state.
 * @param onNext Callback invoked when the user proceeds to the next step.
 * @param onPrevious Callback invoked when the user goes back to the previous step.
 * @param destinationViewModelFactory Factory function to create ViewModels for destination input
 *   fields.
 */
@Composable
fun FirstDestinationScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    destinationViewModelFactory: @Composable (Int) -> AddressTextFieldViewModelContract = { index ->
      viewModel<DestinationTextFieldViewModel>(
          key = "destination_$index",
          factory = DestinationTextFieldViewModelFactory(MySwitzerlandLocationRepository()))
    }
) {
  val destinations = remember { mutableStateListOf<Location>() }

  Scaffold(
      topBar = {
        TopBar(onClick = { onPrevious() }, modifier = Modifier.testTag(RETURN_BUTTON))
      }) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background) {
              Column(
                  modifier = Modifier.padding(dimensionResource(R.dimen.mid_padding)),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.SpaceBetween) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)) {
                          // --- Title ---
                          Text(
                              modifier = Modifier.testTag(FIRST_DESTINATIONS_TITLE),
                              text = stringResource(R.string.first_destinations_title),
                              textAlign = TextAlign.Center,
                              style =
                                  MaterialTheme.typography.headlineMedium.copy(
                                      fontWeight = FontWeight.Bold))

                          Spacer(
                              modifier = Modifier.height(dimensionResource(R.dimen.medium_spacer)))

                          // --- List of Destination Input Fields ---
                          LazyColumn(
                              modifier = Modifier.fillMaxWidth(),
                              horizontalAlignment = Alignment.CenterHorizontally) {
                                itemsIndexed(destinations, key = { index, _ -> index }) { index, _
                                  ->
                                  // Create a new ViewModel for each destination input field
                                  val destinationVm = destinationViewModelFactory(index)
                                  LocationAutocompleteTextField(
                                      onLocationSelected = { selectedLocation ->
                                        destinations[index] = selectedLocation
                                      },
                                      addressTextFieldViewModel = destinationVm,
                                      clearOnSelect = false,
                                      name = "Destination ${index + 1}")

                                  Spacer(
                                      modifier =
                                          Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
                                }
                              }

                          Spacer(
                              modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

                          // --- Add Destination Button ---
                          Button(
                              modifier = Modifier.testTag(ADD_FIRST_DESTINATION),
                              onClick = {
                                destinations.add(
                                    Location(coordinate = Coordinate(0.0, 0.0), name = ""))
                              },
                              enabled =
                                  (destinations.isEmpty() ||
                                      destinations.last().name.isNotEmpty()) &&
                                      destinations.size < MAX_DESTINATIONS,
                          ) {
                            Text(
                                if (destinations.size < MAX_DESTINATIONS) {
                                  stringResource(R.string.add_first_destination)
                                } else stringResource(R.string.destination_limited))
                          }
                        }

                    // --- Next Button ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center) {
                          Button(
                              modifier = Modifier.testTag(NEXT_BUTTON),
                              onClick = {
                                val finalList = destinations.filter { it.name.isNotEmpty() }
                                viewModel.setDestinations(finalList)
                                onNext()
                              },
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = MaterialTheme.colorScheme.primary)) {
                                Text(
                                    stringResource(R.string.next),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium)
                              }
                        }
                  }
            }
      }
}

@Preview
@Composable
fun FirstDestinationScreenPreview() {
  FirstDestinationScreen()
}
