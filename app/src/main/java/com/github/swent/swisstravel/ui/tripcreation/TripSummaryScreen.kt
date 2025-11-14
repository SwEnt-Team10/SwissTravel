package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.navigation.TopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Test tags for UI tests to identify components. */
object TripSummaryTestTags {
  const val TRIP_SUMMARY_TITLE = "tripSummary"
  const val TRIP_NAME_FIELD = "tripName"
  const val FROM_DATE = "fromDate"
  const val TO_DATE = "toDate"
  const val NUMBER_OF_TRAVELERS = "numberOfTravelers"
  const val ADULTS_COUNT = "adultCount"
  const val CHILDREN_COUNT = "childCount"
  const val TRAVELLING_PREFERENCES_LABEL = "travellingPreferences"
  const val PREFERENCE_ICON = "preferenceIcon"
  const val ARRIVAL_LABEL = "arrivalLabel"
  const val DEPARTURE_LABEL = "departureLabel"

  const val PLACES_LABEL = "placesLabel"
  const val DESTINATIONS_EMPTY_LIST = "destinationsList"
  const val DESTINATION_ITEM = "destinationItem"
  const val CREATE_TRIP_BUTTON = "createTripButton"
  const val TRIP_SUMMARY_SCREEN = "tripSummaryScreen"
}

/**
 * Composable function that displays a summary screen for trip settings.
 *
 * @param viewModel The ViewModel that holds the trip settings state.
 * @param onNext Callback function to be invoked when the user proceeds to the next step.
 * @param onPrevious Callback function to be invoked when the user goes back to the previous step.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripSummaryScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val state by viewModel.tripSettings.collectAsState()
  val context = LocalContext.current
  var tripName by rememberSaveable { mutableStateOf("") }
  val startDate = state.date.startDate
  val endDate = state.date.endDate
  val departure = state.arrivalDeparture.departureLocation?.name ?: ""
  val arrival = state.arrivalDeparture.arrivalLocation?.name ?: ""

  val tripSummaryTitle = stringResource(R.string.trip_summary)
  val tripNameLabel = stringResource(R.string.trip_name_summary)
  val numberOfTravelersLabel = stringResource(R.string.number_of_travelers)
  val adultSingular = stringResource(R.string.adult)
  val adultPlural = stringResource(R.string.adults)
  val childSingular = stringResource(R.string.child)
  val childPlural = stringResource(R.string.children)
  val travellingPreferencesLabel = stringResource(R.string.travelling_preferences_summary)
  val arrivalLabel = stringResource(R.string.arrival)
  val departureLabel = stringResource(R.string.departure)
  val placesLabel = stringResource(R.string.places)
  val createTripLabel = stringResource(R.string.create_trip_summary)
  val emptyNameToast = stringResource(R.string.trip_name_required)
  val emptyDeparture = stringResource(R.string.departure_required)
  val emptyArrival = stringResource(R.string.arrival_required)
  val noWantedPlaces = stringResource(R.string.no_wanted_places)
  val fromDate = stringResource(R.string.from_summary)
  val toDate = stringResource(R.string.to_summary)
  val listState = rememberLazyListState()
  val focusManager = LocalFocusManager.current

  Scaffold(
      topBar = {
        Box(
            modifier =
                Modifier.testTag(TripSummaryTestTags.TRIP_SUMMARY_TITLE).clickable {
                  onPrevious()
                }) {
              TopBar(onClick = onPrevious, title = tripSummaryTitle)
            }
      }) { pd ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          LazyColumn(
              state = listState,
              modifier =
                  Modifier.fillMaxSize()
                      .padding(pd)
                      .testTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN),
          ) {
            item {
              OutlinedTextField(
                  value = tripName,
                  onValueChange = { new -> tripName = new },
                  label = { Text(tripNameLabel) },
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(16.dp)
                          .testTag(TripSummaryTestTags.TRIP_NAME_FIELD),
                  keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                  keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }))
            }
            // Summary of dates
            item {
              Text(
                  text = "$fromDate ${formatDateForDisplay(startDate)}",
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.FROM_DATE))
            }
            item {
              Text(
                  text = "$toDate ${formatDateForDisplay(endDate)}",
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.TO_DATE))
            }
            // Summary of travelers
            item {
              Text(
                  text = numberOfTravelersLabel,
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.NUMBER_OF_TRAVELERS))
            }
            item {
              val nAdults = state.travelers.adults
              val stringAdult = if (nAdults == 1) adultSingular else adultPlural
              Text(
                  text = "$nAdults $stringAdult",
                  style = MaterialTheme.typography.bodyLarge,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.ADULTS_COUNT))
            }
            item {
              val nChildren = state.travelers.children
              val stringChild = if (nChildren == 1) childSingular else childPlural
              Text(
                  text = "$nChildren $stringChild",
                  style = MaterialTheme.typography.bodyLarge,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.CHILDREN_COUNT))
            }
            // Summary of travelling preferences
            item {
              Text(
                  text = travellingPreferencesLabel,
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.TRAVELLING_PREFERENCES_LABEL))
            }
            item {
              val prefs = state.preferences
              if (prefs.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_preferences),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
              } else {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag("${TripSummaryTestTags.PREFERENCE_ICON}_list"),
                    contentAlignment = Alignment.Center) {
                      FlowRow(
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                          verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            prefs.forEachIndexed { idx, preference ->
                              Box(
                                  modifier =
                                      Modifier.testTag(
                                          "${TripSummaryTestTags.PREFERENCE_ICON}_$idx")) {
                                    TripPreferenceIcon(preference = preference)
                                  }
                            }
                          }
                    }
              }
            }
            // summary of start and end locations
            item {
              Text(
                  text = arrivalLabel,
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.ARRIVAL_LABEL))
            }
            item {
              Text(
                  text = arrival,
                  style = MaterialTheme.typography.bodyLarge,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag("${TripSummaryTestTags.ARRIVAL_LABEL}_value"))
            }
            item {
              Text(
                  text = departureLabel,
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.DEPARTURE_LABEL))
            }
            item {
              Text(
                  text = departure,
                  style = MaterialTheme.typography.bodyLarge,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag("${TripSummaryTestTags.DEPARTURE_LABEL}_value"))
            }
            // summary of places
            item {
              Text(
                  text = placesLabel,
                  style = MaterialTheme.typography.headlineSmall,
                  modifier =
                      Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                          .testTag(TripSummaryTestTags.PLACES_LABEL))
            }
            val destinations = state.destinations
            if (destinations.isEmpty()) {
              item {
                Text(
                    text = noWantedPlaces,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag(TripSummaryTestTags.DESTINATIONS_EMPTY_LIST))
              }
            } else {
              itemsIndexed(destinations) { index, loc ->
                if (loc.name.isEmpty()) {
                  return@itemsIndexed
                }
                Text(
                    text = loc.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("${TripSummaryTestTags.DESTINATION_ITEM}_$index"))
              }
            }
            // Create trip button
            item {
              Box(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                          if (tripName.isBlank()) {
                            Toast.makeText(context, emptyNameToast, Toast.LENGTH_SHORT).show()
                            return@Button
                          } else if (departure == "") {
                            Toast.makeText(context, emptyDeparture, Toast.LENGTH_SHORT).show()
                            return@Button
                          } else if (arrival == "") {
                            Toast.makeText(context, emptyArrival, Toast.LENGTH_SHORT).show()
                          } else {
                            viewModel.updateName(tripName)
                            viewModel.saveTrip()
                            Toast.makeText(context, createTripLabel, Toast.LENGTH_SHORT).show()
                            onNext()
                          }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag(TripSummaryTestTags.CREATE_TRIP_BUTTON)) {
                          Text(
                              createTripLabel,
                              color = MaterialTheme.colorScheme.onPrimary,
                              style = MaterialTheme.typography.titleMedium)
                        }
                  }
            }
          }
        }
      }
}
/**
 * Formats a date for display in the "d MMM yyyy" format for the specified locale.
 *
 * @param date The date to format, which can be a LocalDate or a String.
 * @param locale The locale to use for formatting. Defaults to Locale.FRANCE.
 * @return The formatted date string, or an empty string if the date is null.
 */
private fun formatDateForDisplay(date: Any?, locale: Locale = Locale.FRANCE): String {
  if (date == null) return ""
  val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
  return when (date) {
    is LocalDate -> date.format(formatter)
    is String -> {
      try {
        LocalDate.parse(date).format(formatter)
      } catch (e: DateTimeParseException) {
        date
      }
    }
    else -> date.toString()
  }
}
