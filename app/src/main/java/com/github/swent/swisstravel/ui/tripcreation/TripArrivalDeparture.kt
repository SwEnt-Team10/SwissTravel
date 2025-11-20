package com.github.swent.swisstravel.ui.tripcreation

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModel
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModelContract
import com.github.swent.swisstravel.ui.geocoding.LocationAutocompleteTextField
import com.github.swent.swisstravel.ui.navigation.TopBar
import com.github.swent.swisstravel.ui.tripcreation.ArrivalDepartureTestTags.NEXT_BUTTON
import com.github.swent.swisstravel.ui.tripcreation.TripFirstDestinationsTestTags.RETURN_BUTTON

/** Test tags for UI tests to identify components within the Arrival/Departure screen. */
object ArrivalDepartureTestTags {
  const val ARRIVAL_TEXTFIELD = "arrival_textfield"
  const val DEPARTURE_TEXTFIELD = "departure_textfield"
  const val NEXT_BUTTON = "next"
}

/**
 * A composable screen that allows users to set the arrival and departure locations for their trip.
 *
 * This screen features two [LocationAutocompleteTextField] components, one for the arrival location
 * and one for the departure location. It uses separate instances of
 * [AddressTextFieldViewModelContract] to manage the state of each text field independently. The
 * selected locations are synchronized with the main [TripSettingsViewModel].
 *
 * @param viewModel The [TripSettingsViewModel] instance that holds the overall trip configuration.
 * @param onNext A callback function to be invoked when the user proceeds to the next step.
 * @param arrivalAddressVm The view model for the arrival address text field. A unique key is used
 *   to ensure a distinct instance from the departure view model.
 * @param departureAddressVm The view model for the departure address text field. A unique key is
 *   used to ensure a distinct instance from the arrival view model.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArrivalDepartureScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    arrivalAddressVm: AddressTextFieldViewModel = viewModel(key = "arrivalAddressVm"),
    departureAddressVm: AddressTextFieldViewModel = viewModel(key = "departureAddressVm")
) {
  // Use different separate view models for arrival and departure
  val arrivalState by arrivalAddressVm.addressState.collectAsState()
  val departureState by departureAddressVm.addressState.collectAsState()
  val context = LocalContext.current
  val emptyDeparture = stringResource(R.string.departure_required)
  val emptyArrival = stringResource(R.string.arrival_required)
  val emptyDepartureAndArrival = stringResource(R.string.departure_and_arrival_required)

  // A LaunchedEffect to synchronize the selected arrival location with the TripSettingsViewModel.
  // It triggers whenever the selected location in the arrivalAddressVm changes.
  LaunchedEffect(arrivalState.selectedLocation) {
    arrivalState.selectedLocation?.let { viewModel.updateArrivalLocation(it) }
  }

  // A LaunchedEffect to synchronize the selected departure location with the TripSettingsViewModel.
  // It triggers whenever the selected location in the departureAddressVm changes.
  LaunchedEffect(departureState.selectedLocation) {
    departureState.selectedLocation?.let { viewModel.updateDepartureLocation(it) }
  }

  Scaffold(
      topBar = {
        TopBar(
            onClick = { onPrevious() }, modifier = Modifier.fillMaxWidth().testTag(RETURN_BUTTON))
      }) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(dimensionResource(R.dimen.arrival_departure_padding)),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                  // --- Title ---
                  Text(
                      text = stringResource(R.string.arrivalDeparture),
                      textAlign = TextAlign.Center,
                      style =
                          MaterialTheme.typography.headlineMedium.copy(
                              fontWeight = FontWeight.Bold,
                          ))

                  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_spacer)))

                  // --- Arrival Destination (autocomplete) ---
                  LocationAutocompleteTextField(
                      addressTextFieldViewModel = arrivalAddressVm,
                      modifier = Modifier.testTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD),
                      name = stringResource(R.string.arrival_location))
                  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.large_spacer)))

                  // --- Departure Destination (autocomplete) ---
                  LocationAutocompleteTextField(
                      addressTextFieldViewModel = departureAddressVm,
                      modifier = Modifier.testTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD),
                      name = stringResource(R.string.departure_location))

                  Spacer(modifier = Modifier.height(dimensionResource(R.dimen.medium_spacer)))
                }

                // --- Done button ---
                Button(
                    modifier = Modifier.testTag(NEXT_BUTTON),
                    onClick = {

                      // check that both locations are set
                      if (arrivalState.selectedLocation == null &&
                          departureState.selectedLocation == null) {
                        Toast.makeText(context, emptyDepartureAndArrival, Toast.LENGTH_SHORT).show()
                        return@Button
                      }
                      if (departureState.selectedLocation == null) {
                        Toast.makeText(context, emptyDeparture, Toast.LENGTH_SHORT).show()
                        return@Button
                      }
                      if (arrivalState.selectedLocation == null) {
                        Toast.makeText(context, emptyArrival, Toast.LENGTH_SHORT).show()
                        return@Button
                      }

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

/** A composable preview function for the [ArrivalDepartureScreen]. */
@Preview
@Composable
fun TripArrivalDeparturePreview() {
  ArrivalDepartureScreen()
}
