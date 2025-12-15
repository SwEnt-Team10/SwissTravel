package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.github.swent.swisstravel.tripSettingsViewModel
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

private const val MAX_DESTINATIONS = 9

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
  val context = LocalContext.current
  var isExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { viewModel.generateSuggestions(context) }

  Scaffold(
      topBar = {
        TopBar(onClick = { onPrevious() }, modifier = Modifier.testTag(RETURN_BUTTON))
      }) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background) {
              LazyColumn(
                  modifier = Modifier.fillMaxSize().padding(dimensionResource(R.dimen.mid_padding)),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    item { FirstDestinationsTitle() }

                    item {
                      AddDestinationButton(
                          destinations = destinations,
                          onAddDestination = {
                            destinations.add(Location(coordinate = Coordinate(0.0, 0.0), name = ""))
                          },
                          getSuggestionToggledSelectedSize = {
                              viewModel.getSuggestionToggledSelectedSize()
                          })
                    }

                    itemsIndexed(destinations, key = { index, _ -> index }) { index, _ ->
                      val destinationVm = destinationViewModelFactory(index)
                      DestinationItem(
                          index = index,
                          destinationVm = destinationVm,
                          onLocationSelected = { selectedLocation ->
                            destinations[index] = selectedLocation
                          })
                    }

                    item {
                      SuggestionsHeader(
                          isExpanded = isExpanded, onToggleExpand = { isExpanded = !isExpanded })
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
                          getSuggestionToggledSelectedSize = {
                              viewModel.getSuggestionToggledSelectedSize()
                          })
                    }

                    item {
                      NextButton(
                          onNext = {
                            val manualList = destinations.filter { it.name.isNotEmpty() }
                            val mergedList = manualList + selectedSuggestions
                            viewModel.setDestinations(mergedList)
                            onNext()
                          })
                    }
                  }
            }
      }
}

@Composable
fun FirstDestinationsTitle() {
  Text(
      modifier = Modifier.testTag(FIRST_DESTINATIONS_TITLE),
      text = stringResource(R.string.first_destinations_title),
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))
}

@Composable
fun AddDestinationButton(destinations: List<Location>, onAddDestination: () -> Unit, getSuggestionToggledSelectedSize: () -> Int) {
  Button(
      modifier = Modifier.testTag(ADD_FIRST_DESTINATION),
      onClick = onAddDestination,
      enabled =
          (destinations.isEmpty() || destinations.last().name.isNotEmpty()) &&
              destinations.size + getSuggestionToggledSelectedSize() < MAX_DESTINATIONS,
  ) {
    Text(
        if (destinations.size + getSuggestionToggledSelectedSize() < MAX_DESTINATIONS) {
          stringResource(R.string.add_first_destination)
        } else stringResource(R.string.destination_limited))
  }

  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.mid_spacer)))

  HorizontalDivider()

  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))
}

@Composable
fun DestinationItem(
    index: Int,
    destinationVm: AddressTextFieldViewModelContract,
    onLocationSelected: (Location) -> Unit
) {
  LocationAutocompleteTextField(
      onLocationSelected = onLocationSelected,
      addressTextFieldViewModel = destinationVm,
      clearOnSelect = false,
      name = "Destination ${index + 1}",
      showImages = true)

  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))
}

@Composable
fun SuggestionsHeader(isExpanded: Boolean, onToggleExpand: () -> Unit) {
  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clickable { onToggleExpand() }
              .padding(vertical = dimensionResource(R.dimen.small_padding)),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(text = "See Our Suggestions For You", style = MaterialTheme.typography.titleMedium)
        Icon(
            imageVector =
                if (isExpanded) androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp
                else androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand")
      }
}

@Composable
fun NextButton(onNext: () -> Unit) {
  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.tiny_spacer)))

  HorizontalDivider()

  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_large_spacer)))

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    Button(
        modifier = Modifier.testTag(NEXT_BUTTON),
        onClick = onNext,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
          Text(
              stringResource(R.string.next),
              color = MaterialTheme.colorScheme.onPrimary,
              style = MaterialTheme.typography.titleMedium)
        }
  }
}

/** Extension function for LazyListScope to display a list of suggested destinations. */
fun LazyListScope.SuggestionList(
    destinations: List<Location>,
    suggestions: List<Location>,
    onSuggestionSelected: (Location) -> Unit,
    onSuggestionDeselected: (Location) -> Unit,
    getSuggestionToggledSelectedSize: () -> Int,
) {
    itemsIndexed(suggestions) { _, location ->
        val isSelected =
            destinations.any { it.name == location.name && it.coordinate == location.coordinate }

        // Obtain the Context to show the Toast
        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Logic for clicking the entire Row
                    if (isSelected) {
                        onSuggestionDeselected(location)
                    } else {
                        // Check limit before selecting
                        val currentCount = destinations.size + getSuggestionToggledSelectedSize()
                        if (currentCount < MAX_DESTINATIONS) {
                            onSuggestionSelected(location)
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(
                                        R.string.max_destinations_toast,
                                        MAX_DESTINATIONS
                                    ),
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
                .padding(dimensionResource(R.dimen.small_padding))
                .testTag("suggestion_row_${location.name}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                modifier = Modifier.testTag("suggestion_checkbox_${location.name}"),
                checked = isSelected,
                onCheckedChange = { checked ->
                    // 'checked' is the NEW state.
                    // true = user wants to check it. false = user wants to uncheck it.
                    if (checked) {
                        val currentCount = destinations.size + getSuggestionToggledSelectedSize()

                        if (currentCount < MAX_DESTINATIONS) {
                            onSuggestionSelected(location)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.max_destinations_toast, MAX_DESTINATIONS),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // User is unchecking (removing), always allow
                        onSuggestionDeselected(location)
                    }
                }
            )
        }
        HorizontalDivider()
    }
}

@Preview
@Composable
fun FirstDestinationScreenPreview() {
  FirstDestinationScreen()
}
