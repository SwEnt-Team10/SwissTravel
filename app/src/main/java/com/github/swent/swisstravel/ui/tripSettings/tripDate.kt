package com.github.swent.swisstravel.ui.tripSettings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.composable.DateSelectorRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TripDateTestTags {
  const val NEXT = "next"
  const val TRIP_DATE_SCREEN = "tripDateScreen"
}

@Composable
fun TripDateScreen(viewModel: TripSettingsViewModel = viewModel(), onNext: () -> Unit = {}) {
  val tripSettings by viewModel.tripSettings.collectAsState()
  var startDate by remember { mutableStateOf(tripSettings.date.startDate ?: LocalDate.now()) }
  var endDate by remember {
    mutableStateOf(tripSettings.date.endDate ?: LocalDate.now().plusDays(1))
  }

  val context = LocalContext.current
  val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

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

  Surface(
      modifier = Modifier.fillMaxSize().testTag(TripDateTestTags.TRIP_DATE_SCREEN),
      color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {

              // --- Title ---
              Text(
                  text = stringResource(R.string.tripDates),
                  textAlign = TextAlign.Center,
                  style =
                      MaterialTheme.typography.headlineMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onBackground))

              Spacer(modifier = Modifier.height(32.dp))

              // --- Date selectors ---
              Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    DateSelectorRow(
                        label = stringResource(R.string.startDate),
                        dateText = startDate.format(formatter),
                        onClick = { startDatePicker.show() })

                    Spacer(modifier = Modifier.height(48.dp))

                    DateSelectorRow(
                        label = stringResource(R.string.endDate),
                        dateText = endDate.format(formatter),
                        onClick = { endDatePicker.show() })
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // --- Done button ---
              Button(
                  onClick = {
                    viewModel.updateDates(startDate, endDate)
                    onNext()
                  },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(24.dp),
                  modifier = Modifier.testTag(TripDateTestTags.NEXT)) {
                    Text(
                        text = stringResource(R.string.next),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium)
                  }
            }
      }
}
