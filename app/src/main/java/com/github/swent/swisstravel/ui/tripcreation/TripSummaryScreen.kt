package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryTestTags.ADULTS_COUNT
import com.github.swent.swisstravel.ui.tripcreation.TripSummaryTestTags.CHILDREN_COUNT
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
              contentPadding = PaddingValues(dimensionResource(R.dimen.trip_summary_padding)),
              verticalArrangement =
                  Arrangement.spacedBy(dimensionResource(R.dimen.trip_summary_padding)),
              modifier =
                  Modifier.fillMaxSize()
                      .padding(pd)
                      .testTag(TripSummaryTestTags.TRIP_SUMMARY_SCREEN)) {

                // 1. Trip Name Section
                item { TripNameField(state, viewModel) }

                // 2. Trip Details Card (Dates & Travelers)
                item {
                  SectionCard(title = "Trip Details", icon = Icons.Default.DateRange) {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                dimensionResource(R.dimen.trip_summary_card_content_spacing))) {
                          DateSummary(state.date.startDate, state.date.endDate)
                          TravelerSummary(state)
                        }
                  }
                }

                // 3. Itinerary Card (Start, End, Via)
                item {
                  SectionCard(title = "Itinerary", icon = Icons.Default.Place) {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                dimensionResource(R.dimen.trip_summary_card_content_spacing))) {
                          ArrivalDepartureSummary(
                              arrival = state.arrivalDeparture.arrivalLocation?.name ?: "",
                              departure = state.arrivalDeparture.departureLocation?.name ?: "")
                          DestinationSummary(state.destinations)
                        }
                  }
                }

                // 4. Preferences Card
                item {
                  SectionCard(
                      title = stringResource(R.string.travelling_preferences_summary),
                      icon = Icons.Default.Settings,
                      // Pass test tag to the title inside the card logic or handle externally
                      titleTestTag = TripSummaryTestTags.TRAVELLING_PREFERENCES_LABEL) {
                        PreferenceSummary(state.preferences)
                      }
                }

                // 5. Create Button
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

/** A Helper Card container for sections */
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    titleTestTag: String? = null,
    content: @Composable () -> Unit
) {
  Card(
      elevation =
          CardDefaults.cardElevation(
              defaultElevation = dimensionResource(R.dimen.trip_summary_card_elevation)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(dimensionResource(R.dimen.trip_summary_card_shape)),
      modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.trip_summary_padding))) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
              Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary)
              Spacer(
                  modifier =
                      Modifier.width(dimensionResource(R.dimen.trip_summary_icon_title_spacing)))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = titleTestTag?.let { Modifier.testTag(it) } ?: Modifier)
          }
          Spacer(modifier = Modifier.height(dimensionResource(R.dimen.trip_summary_header_spacing)))
          content()
        }
      }
}

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
      shape = RoundedCornerShape(dimensionResource(R.dimen.trip_summary_input_radius)),
      modifier = Modifier.fillMaxWidth().testTag(TripSummaryTestTags.TRIP_NAME_FIELD),
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
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    // We map the "From" and "To" texts to a simpler layout while keeping the full string for the
    // test tags
    // to ensure tests pass if they rely on specific text content.
    Column(modifier = Modifier.weight(1f)) {
      SummaryTitle(
          text = "${stringResource(R.string.from_summary)} ${formatDateForDisplay(startDate)}",
          testTag = TripSummaryTestTags.FROM_DATE)
    }
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
      SummaryTitle(
          text = "${stringResource(R.string.to_summary)} ${formatDateForDisplay(endDate)}",
          testTag = TripSummaryTestTags.TO_DATE)
    }
  }
}

/**
 * Composable to display the number of travelers.
 *
 * @param state The current state of the trip settings.
 */
@Composable
private fun TravelerSummary(state: TripSettings) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier =
              Modifier.padding(end = dimensionResource(R.dimen.trip_summary_icon_title_spacing)))
      SummaryTitle(
          text = stringResource(R.string.number_of_travelers),
          testTag = TripSummaryTestTags.NUMBER_OF_TRAVELERS)
    }

    Row(
        modifier =
            Modifier.padding(
                start = dimensionResource(R.dimen.trip_summary_traveler_indent),
                top = dimensionResource(R.dimen.trip_summary_traveler_top_padding))) {
          SummaryValue(
              value = state.travelers.adults,
              singular = stringResource(R.string.adult),
              plural = stringResource(R.string.adults),
              tag = ADULTS_COUNT)

          Spacer(modifier = Modifier.width(dimensionResource(R.dimen.trip_summary_padding)))

          SummaryValue(
              value = state.travelers.children,
              singular = stringResource(R.string.child),
              plural = stringResource(R.string.children),
              tag = CHILDREN_COUNT)
        }
  }
}

/**
 * Composable to display the list of travelling preferences.
 *
 * @param prefs The list of travelling preferences to display.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceSummary(prefs: List<Preference>) {
  // Title is handled by the SectionCard wrapper to allow for Icon alignment

  if (prefs.isEmpty()) {
    Text(
        text = stringResource(R.string.no_preferences),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
  } else {
    Box(
        modifier = Modifier.fillMaxWidth().testTag("${TripSummaryTestTags.PREFERENCE_ICON}_list"),
        contentAlignment = Alignment.CenterStart) {
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
  Column(
      verticalArrangement =
          Arrangement.spacedBy(dimensionResource(R.dimen.trip_summary_group_spacing))) {
        // Arrival (Start)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = stringResource(R.string.arrival),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.testTag(TripSummaryTestTags.ARRIVAL_LABEL))
          Text(
              text = arrival,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("${TripSummaryTestTags.ARRIVAL_LABEL}_value"))
        }

        // Departure (End)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = stringResource(R.string.departure),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.testTag(TripSummaryTestTags.DEPARTURE_LABEL))
          Text(
              text = departure,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("${TripSummaryTestTags.DEPARTURE_LABEL}_value"))
        }
      }
}

/**
 * Composable to display the list of destinations.
 *
 * @param destinations The list of destinations to display.
 */
@Composable
private fun DestinationSummary(destinations: List<Location>) {
  Column {
    SummaryTitle(text = stringResource(R.string.places), testTag = TripSummaryTestTags.PLACES_LABEL)

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.trip_summary_group_spacing)))

    if (destinations.isEmpty()) {
      Text(
          text = stringResource(R.string.no_wanted_places),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
          modifier = Modifier.testTag(TripSummaryTestTags.DESTINATIONS_EMPTY_LIST))
    } else {
      Column(
          verticalArrangement =
              Arrangement.spacedBy(dimensionResource(R.dimen.trip_summary_destination_spacing))) {
            destinations
                .filter { it.name.isNotBlank() }
                .forEachIndexed { index, loc ->
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small bullet point or icon could go here
                    Text(
                        text = "•",
                        modifier =
                            Modifier.padding(
                                end = dimensionResource(R.dimen.trip_summary_group_spacing)),
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = loc.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier.testTag("${TripSummaryTestTags.DESTINATION_ITEM}_$index"))
                  }
                }
          }
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
  Button(
      enabled = enabled,
      onClick = { onClick() },
      shape = RoundedCornerShape(dimensionResource(R.dimen.trip_summary_card_shape)),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      modifier =
          Modifier.fillMaxWidth()
              .height(dimensionResource(R.dimen.trip_summary_button_height))
              .testTag(TripSummaryTestTags.CREATE_TRIP_BUTTON)) {
        Text(
            stringResource(R.string.create_trip_summary),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
      }
}

/* ---------------------------- HELPERS ---------------------------- */

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
      style = MaterialTheme.typography.bodyLarge, // Adjusted size for inside cards
      fontWeight = FontWeight.Medium,
      modifier = Modifier.testTag(testTag))
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
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.testTag(tag))
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
