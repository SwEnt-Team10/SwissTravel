package com.github.swent.swisstravel.ui.tripcreation

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.DateSelectorRow
import com.github.swent.swisstravel.ui.navigation.TopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

/** Test tags for UI tests to identify components. */
object TripDateTestTags {
  const val NEXT = "next"
  const val TRIP_DATE_SCREEN = "tripDateScreen"
}

/**
 * Screen where users can set the start and end dates for their trip.
 *
 * @param viewModel ViewModel to handle the trip settings logic.
 * @param onNext Callback to be invoked when the user wants to proceed to the next step.
 */
@Composable
fun TripDateScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  var startDate by remember { mutableStateOf(tripSettings.date.startDate ?: LocalDate.now()) }
  var endDate by remember {
    mutableStateOf(tripSettings.date.endDate ?: LocalDate.now().plusDays(1))
  }

  LaunchedEffect(startDate, endDate) { viewModel.updateDates(startDate, endDate) }

  val context = LocalContext.current
  val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

  // --- Listen for validation events ---
  LaunchedEffect(Unit) {
    viewModel.validationEvents.collectLatest {
      when (it) {
        ValidationEvent.Proceed -> onNext()
        ValidationEvent.EndDateIsBeforeStartDateError -> {
          Toast.makeText(context, R.string.end_date_error, Toast.LENGTH_SHORT).show()
        }
        ValidationEvent.EndDateIsBeforeToday ->
            Toast.makeText(context, R.string.end_date_past, Toast.LENGTH_SHORT).show()
        else -> {
          /* Ignore other events */
        }
      }
    }
  }

  // --- Date picker dialogs ---
  val startDatePicker = remember {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
          startDate = LocalDate.of(year, month + 1, dayOfMonth)
          if (endDate.isBefore(startDate)) endDate = startDate.plusDays(1)
        },
        startDate.year,
        startDate.monthValue - 1,
        startDate.dayOfMonth)
  }

  val endDatePicker = remember {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> endDate = LocalDate.of(year, month + 1, dayOfMonth) },
        endDate.year,
        endDate.monthValue - 1,
        endDate.dayOfMonth)
  }

  LaunchedEffect(startDate) {
    startDatePicker.updateDate(startDate.year, startDate.monthValue - 1, startDate.dayOfMonth)
  }

  LaunchedEffect(endDate) {
    endDatePicker.updateDate(endDate.year, endDate.monthValue - 1, endDate.dayOfMonth)
  }
  Scaffold(
      topBar = { TopBar(onClick = { onPrevious() }) },
      modifier = Modifier.testTag(TripDateTestTags.TRIP_DATE_SCREEN),
  ) { pd ->
    Surface(
        modifier = Modifier.fillMaxSize().padding(pd),
        color = MaterialTheme.colorScheme.background) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(
                          horizontal = dimensionResource(R.dimen.mid_padding),
                          vertical = dimensionResource(R.dimen.medium_padding)),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween) {

                // --- Title ---
                Text(
                    text = stringResource(R.string.trip_dates),
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground))

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_spacer)))

                // --- Date selectors ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      DateSelectorRow(
                          label = stringResource(R.string.start_date),
                          dateText = startDate.format(formatter),
                          onClick = { startDatePicker.show() })

                      Spacer(
                          modifier =
                              Modifier.height(dimensionResource(R.dimen.medium_large_spacer)))

                      DateSelectorRow(
                          label = stringResource(R.string.end_date),
                          dateText = endDate.format(formatter),
                          onClick = { endDatePicker.show() })
                    }

                // --- Disclaimer ---
                Text(
                    text = stringResource(R.string.trip_dates_disclaimer),
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground))

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.small_spacer)))

                // --- Done button ---
                Button(
                    onClick = { viewModel.onNextFromDateScreen() },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.date_done_button_radius)),
                    modifier = Modifier.testTag(TripDateTestTags.NEXT)) {
                      Text(
                          text = stringResource(R.string.next),
                          color = MaterialTheme.colorScheme.onPrimary,
                          style = MaterialTheme.typography.titleMedium)
                    }
              }
        }
  }
}
