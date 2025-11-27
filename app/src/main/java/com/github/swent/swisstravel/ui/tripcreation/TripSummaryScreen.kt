package com.github.swent.swisstravel.ui.tripcreation

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.Preference
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
 * Composable to display the trip summary screen.
 *
 * @param viewModel The view model to use.
 * @param onNext The action to perform when the next button is clicked.
 * @param onPrevious The action to perform when the previous button is clicked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripSummaryScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val state by viewModel.tripSettings.collectAsState()
  val listState = rememberLazyListState()
  val context = LocalContext.current

  Scaffold(
      topBar = {
        Box(
            modifier =
                Modifier.testTag(TripSummaryTestTags.TRIP_SUMMARY_TITLE).clickable {
                  onPrevious()
                }) {
              TopBar(onClick = onPrevious, title = stringResource(R.string.trip_summary))
            }
      }) { pd ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          LazyColumn(
              state = listState,
              modifier =
                  Modifier.fillMaxSize()
                      .padding(pd)
                      .testTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)) {
                item { TripNameField(state, viewModel) }

                item { DateSummary(state.date.startDate, state.date.endDate) }

                item { TravelerSummary(state) }

                item { PreferenceSummary(state.preferences) }

                item {
                  ArrivalDepartureSummary(
                      arrival = state.arrivalDeparture.arrivalLocation?.name ?: "",
                      departure = state.arrivalDeparture.departureLocation?.name ?: "")
                }

                item { DestinationSummary(state.destinations) }

                item {
                  CreateTripButton(
                      enabled = state.name.isNotBlank(),
                      onClick = {
                        viewModel.saveTrip(context)
                        onNext()
                      })
                }
              }
        }
      }
}

/* ---------------------------- COMPONENTS ---------------------------- */

/**
 * Composable to display the trip name field.
 *
 * @param state The current state of the trip settings.
 */
@Composable
private fun TripNameField(state: TripSettings, viewModel: TripSettingsViewModel) {
  val focusManager = LocalFocusManager.current

  OutlinedTextField(
      value = state.name,
      onValueChange = viewModel::updateName,
      label = { Text(stringResource(R.string.trip_name_summary)) },
      isError = state.invalidNameMsg != null,
      supportingText = state.invalidNameMsg?.let { { Text(stringResource(R.string.name_empty)) } },
      modifier =
          Modifier.fillMaxWidth()
              .padding(dimensionResource(R.dimen.trip_summary_padding))
              .testTag(TripSummaryTestTags.TRIP_NAME_FIELD),
      keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }))
}

/**
 * Composable to display the start and end dates.
 *
 * @param startDate The start date.
 * @param endDate The end date.
 */
@Composable
private fun DateSummary(startDate: Any?, endDate: Any?) {
  SummaryTitle(
      text = "${stringResource(R.string.from_summary)} ${formatDateForDisplay(startDate)}",
      testTag = TripSummaryTestTags.FROM_DATE)

  SummaryTitle(
      text = "${stringResource(R.string.to_summary)} ${formatDateForDisplay(endDate)}",
      testTag = TripSummaryTestTags.TO_DATE)
}

/**
 * Composable to display the number of travelers.
 *
 * @param state The current state of the trip settings.
 */
@Composable
private fun TravelerSummary(state: TripSettings) {
  SummaryTitle(
      text = stringResource(R.string.number_of_travelers),
      testTag = TripSummaryTestTags.NUMBER_OF_TRAVELERS)

  SummaryValue(
      value = state.travelers.adults,
      singular = stringResource(R.string.adult),
      plural = stringResource(R.string.adults),
      tag = TripSummaryTestTags.ADULTS_COUNT)

  SummaryValue(
      value = state.travelers.children,
      singular = stringResource(R.string.child),
      plural = stringResource(R.string.children),
      tag = TripSummaryTestTags.CHILDREN_COUNT)
}

/**
 * Composable to display the list of travelling preferences.
 *
 * @param prefs The list of travelling preferences to display.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceSummary(prefs: List<Preference>) {
  SummaryTitle(
      text = stringResource(R.string.travelling_preferences_summary),
      testTag = TripSummaryTestTags.TRAVELLING_PREFERENCES_LABEL)

  if (prefs.isEmpty()) {
    Text(
        text = stringResource(R.string.no_preferences),
        style = MaterialTheme.typography.bodyLarge,
        modifier = summaryPadding())
  } else {
    Box(
        modifier = Modifier.fillMaxWidth().testTag("${TripSummaryTestTags.PREFERENCE_ICON}_list"),
        contentAlignment = Alignment.Center) {
          FlowRow(
              horizontalArrangement =
                  Arrangement.spacedBy(
                      dimensionResource(R.dimen.trip_summary_horizontal_arrangement)),
              verticalArrangement =
                  Arrangement.spacedBy(
                      dimensionResource(R.dimen.trip_summary_vertical_arrangement))) {
                prefs.forEachIndexed { idx, preference ->
                  Box(modifier = Modifier.testTag("${TripSummaryTestTags.PREFERENCE_ICON}_$idx")) {
                    TripPreferenceIcon(preference = preference)
                  }
                }
              }
        }
  }
}

/**
 * Composable to display the arrival and departure times.
 *
 * @param arrival The arrival time.
 * @param departure The departure time.
 */
@Composable
private fun ArrivalDepartureSummary(arrival: String, departure: String) {

  SummaryTitle(text = stringResource(R.string.arrival), testTag = TripSummaryTestTags.ARRIVAL_LABEL)

  Text(
      text = arrival,
      style = MaterialTheme.typography.bodyLarge,
      modifier = summaryPadding().testTag("${TripSummaryTestTags.ARRIVAL_LABEL}_value"))

  SummaryTitle(
      text = stringResource(R.string.departure), testTag = TripSummaryTestTags.DEPARTURE_LABEL)

  Text(
      text = departure,
      style = MaterialTheme.typography.bodyLarge,
      modifier = summaryPadding().testTag("${TripSummaryTestTags.DEPARTURE_LABEL}_value"))
}

/**
 * Composable to display the list of destinations.
 *
 * @param destinations The list of destinations to display.
 */
@Composable
private fun DestinationSummary(destinations: List<Location>) {

  SummaryTitle(text = stringResource(R.string.places), testTag = TripSummaryTestTags.PLACES_LABEL)

  if (destinations.isEmpty()) {
    Text(
        text = stringResource(R.string.no_wanted_places),
        style = MaterialTheme.typography.bodyLarge,
        modifier = summaryPadding().testTag(TripSummaryTestTags.DESTINATIONS_EMPTY_LIST))
  } else {
    destinations
        .filter { it.name.isNotBlank() }
        .forEachIndexed { index, loc ->
          Text(
              text = loc.name,
              style = MaterialTheme.typography.bodyLarge,
              modifier = summaryPadding().testTag("${TripSummaryTestTags.DESTINATION_ITEM}_$index"))
        }
  }
}

/**
 * Composable to display a button to create a trip.
 *
 * @param enabled Whether the button is enabled.
 * @param onClick The action to perform when the button is clicked.
 */
@Composable
private fun CreateTripButton(enabled: Boolean, onClick: () -> Unit) {
  Box(
      modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.trip_summary_padding)),
      contentAlignment = Alignment.Center) {
        Button(
            enabled = enabled,
            onClick = { onClick() },
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag(TripSummaryTestTags.CREATE_TRIP_BUTTON)) {
              Text(
                  stringResource(R.string.create_trip_summary),
                  color = MaterialTheme.colorScheme.onPrimary,
                  style = MaterialTheme.typography.titleMedium)
            }
      }
}

/* ---------------------------- HELPERS ---------------------------- */

/**
 * Composable to add padding to a summary.
 *
 * @return The padding modifier.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
@Composable
private fun summaryPadding(): Modifier =
    Modifier.padding(
        horizontal = dimensionResource(R.dimen.trip_summary_padding_horizontal),
        vertical = dimensionResource(R.dimen.trip_summary_padding_vertical))

/**
 * Composable to display the title of a summary.
 *
 * @param text The text to display.
 * @param testTag The test tag to apply to the title.
 */
@Composable
private fun SummaryTitle(text: String, testTag: String) {
  Text(
      text = text,
      style = MaterialTheme.typography.headlineSmall,
      modifier = summaryPadding().testTag(testTag))
}

/**
 * Composable to display the value of a summary.
 *
 * @param value The value to display.
 * @param singular The singular form of the value.
 * @param plural The plural form of the value.
 * @param tag The test tag
 */
@Composable
private fun SummaryValue(value: Int, singular: String, plural: String, tag: String) {
  val label = if (value == 1) singular else plural
  Text(
      text = "$value $label",
      style = MaterialTheme.typography.bodyLarge,
      modifier = summaryPadding().testTag(tag))
}

/** Formats a date for display in the "d MMM yyyy" format for the specified locale. */
private fun formatDateForDisplay(date: Any?, locale: Locale = Locale.FRANCE): String {
  if (date == null) return ""
  val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

  return when (date) {
    is LocalDate -> date.format(formatter)
    is String -> {
      try {
        LocalDate.parse(date).format(formatter)
      } catch (_: DateTimeParseException) {
        date
      }
    }
    else -> date.toString()
  }
}
