package com.github.swent.swisstravel.ui.tripsettings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.ui.geocoding.AddressAutocompleteTextField
import com.github.swent.swisstravel.ui.geocoding.AddressTextFieldViewModelContract
import com.github.swent.swisstravel.ui.tripcreation.TripSettingsViewModel
import com.github.swent.swisstravel.ui.tripsettings.ArrivalDepartureTestTags.NEXT_BUTTON

object ArrivalDepartureTestTags {
  const val ARRIVAL_TEXTFIELD = "arrival_textfield"
  const val DEPARTURE_TEXTFIELD = "departure_textfield"
  const val NEXT_BUTTON = "next"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArrivalDepartureScreen(
    viewModel: TripSettingsViewModel = viewModel(),
    onNext: () -> Unit = {},
    arrivalAddressVm: AddressTextFieldViewModelContract = viewModel(key = "arrivalAddressVm"),
    departureAddressVm: AddressTextFieldViewModelContract = viewModel(key = "departureAddressVm")
) {

  // Use different separate view models for arrival and departure

  val arrivalState by arrivalAddressVm.addressState.collectAsState()
  val departureState by departureAddressVm.addressState.collectAsState()

  // Synch the queries with the view model
  LaunchedEffect(arrivalState.locationQuery) {
    // Update the model at each modification
    viewModel.updateArrivalLocation(arrivalState.selectedLocation)
  }
  LaunchedEffect(arrivalState.selectedLocation) {
    arrivalState.selectedLocation?.let { viewModel.updateArrivalLocation(it) }
  }
  LaunchedEffect(departureState.locationQuery) {
    viewModel.updateDepartureLocation(departureState.selectedLocation)
  }
  LaunchedEffect(departureState.selectedLocation) {
    departureState.selectedLocation?.let { viewModel.updateDepartureLocation(it) }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp),
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

            Spacer(modifier = Modifier.height(32.dp))

            // --- Arrival Destination (autocomplete) ---
            AddressAutocompleteTextField(
                addressTextFieldViewModel = arrivalAddressVm,
                modifier = Modifier.testTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD))
            Spacer(modifier = Modifier.height(32.dp))

            // --- Departure Destination (autocomplete) ---
            AddressAutocompleteTextField(
                addressTextFieldViewModel = departureAddressVm,
                modifier = Modifier.testTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD))

            Spacer(modifier = Modifier.height(32.dp))
          }

          // --- Done button ---
          Button(
              modifier = Modifier.testTag(NEXT_BUTTON),
              onClick = {
                // save uses the trip arrival and departure LiveData which we keep updated via the
                // LaunchedEffects
                // above
                viewModel.saveTrip()
                onNext()
              },
              colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text(
                    stringResource(R.string.done),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium)
              }
        }
  }
}

@Preview
@Composable
fun TripArrivalDeparturePreview() {
  ArrivalDepartureScreen()
}
