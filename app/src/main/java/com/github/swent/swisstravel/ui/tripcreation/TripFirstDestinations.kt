package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Screen for entering the first destinations of a trip. Note that parts of this class was generated
 * with the help of AI.
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
  val suggestions by viewModel.suggestions.collectAsState()
  val selectedSuggestions by viewModel.selectedSuggestions.collectAsState()
  val context = androidx.compose.ui.platform.LocalContext.current

  androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.generateSuggestions(context) }

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

                          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

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

                          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

                          androidx.compose.material3.HorizontalDivider()

                          Spacer(
                              modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

                          // --- List of Destination Input Fields ---
                          LazyColumn(
                              modifier = Modifier.weight(1f).fillMaxWidth(),
                              horizontalAlignment = Alignment.CenterHorizontally) {
                                itemsIndexed(destinations, key = { index, _ -> index }) { index, _
                                  ->
                                  val destinationVm = destinationViewModelFactory(index)
                                  LocationAutocompleteTextField(
                                      onLocationSelected = { selectedLocation ->
                                        destinations[index] = selectedLocation
                                      },
                                      addressTextFieldViewModel = destinationVm,
                                      clearOnSelect = false,
                                      name = "Destination ${index + 1}",
                                      showImages = true)

                                  Spacer(
                                      modifier =
                                          Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
                                }
                              }

                          Spacer(
                              modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

                          // --- Suggestions ---
                          var isExpanded by remember {
                            androidx.compose.runtime.mutableStateOf(false)
                          }

                          Row(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .clickable { isExpanded = !isExpanded }
                                      .padding(vertical = dimensionResource(R.dimen.small_padding)),
                              horizontalArrangement = Arrangement.SpaceBetween,
                              verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "See Our Suggestions For You",
                                    style = MaterialTheme.typography.titleMedium)
                                androidx.compose.material3.Icon(
                                    imageVector =
                                        if (isExpanded)
                                            androidx.compose.material.icons.Icons.Filled
                                                .KeyboardArrowUp
                                        else
                                            androidx.compose.material.icons.Icons.Filled
                                                .KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand")
                              }

                          if (isExpanded) {
                            SuggestionList(
                                destinations = selectedSuggestions,
                                suggestions = suggestions,
                                onSuggestionSelected = { location ->
                                  viewModel.toggleSuggestion(location)
                                },
                                onSuggestionDeselected = { location ->
                                  viewModel.toggleSuggestion(location)
                                },
                                modifier =
                                    Modifier.height(
                                        dimensionResource(
                                            R.dimen.first_destination_suggestion_height)))
                          }
                        }
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))

                    HorizontalDivider()

                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.medium_large_spacer)))

                    // --- Next Button ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center) {
                          Button(
                              modifier = Modifier.testTag(NEXT_BUTTON),
                              onClick = {
                                val manualList = destinations.filter { it.name.isNotEmpty() }
                                val mergedList = manualList + selectedSuggestions
                                viewModel.setDestinations(mergedList)
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

/**
 * A composable that displays a list of suggested destinations from the Grand Tour of Switzerland.
 * Users can select or deselect these suggestions via checkboxes. This composable was generated with
 * the help of AI.
 *
 * @param destinations The list of currently selected suggested destinations, used to determine
 *   checkbox states.
 * @param onSuggestionSelected Callback invoked when a user selects a suggestion.
 * @param onSuggestionDeselected Callback invoked when a user deselects a suggestion.
 * @param modifier The modifier to be applied to the list.
 */
@Composable
fun SuggestionList(
    destinations: List<Location>,
    suggestions: List<Location>,
    onSuggestionSelected: (Location) -> Unit,
    onSuggestionDeselected: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyColumn(modifier = modifier.fillMaxWidth()) {
    itemsIndexed(suggestions) { _, location ->
      val isSelected =
          destinations.any { it.name == location.name && it.coordinate == location.coordinate }
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .clickable {
                    if (isSelected) {
                      onSuggestionDeselected(location)
                    } else {
                      onSuggestionSelected(location)
                    }
                  }
                  .padding(dimensionResource(R.dimen.small_padding))
                  .testTag("suggestion_row_${location.name}"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f))
            androidx.compose.material3.Checkbox(
                modifier = Modifier.testTag("suggestion_checkbox_${location.name}"),
                checked = isSelected,
                onCheckedChange = { checked ->
                  if (checked) {
                    onSuggestionSelected(location)
                  } else {
                    onSuggestionDeselected(location)
                  }
                })
          }
      HorizontalDivider()
    }
  }
}

@Preview
@Composable
fun FirstDestinationScreenPreview() {
  FirstDestinationScreen()
}
