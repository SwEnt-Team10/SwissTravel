package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel

/** Test tags for the AddressAutocompleteTextField composable. */
object AddressTextTestTags {
  const val INPUT_LOCATION = "inputLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
}
/**
 * A composable that provides an address autocomplete text field using a dropdown menu.
 *
 * This component interacts with the [AddressTextFieldViewModel] to manage state and handle user
 * input. As the user types in the text field, it fetches location suggestions and displays them in
 * a dropdown menu. When a suggestion is selected, it updates the view model with the chosen
 * location.
 *
 * @param addressTextFieldViewModel The view model that manages the state of the address text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressAutocompleteTextField(
    addressTextFieldViewModel: AddressTextFieldViewModelContract =
        viewModel<AddressTextFieldViewModel>()
) {
  val state = addressTextFieldViewModel.addressState.collectAsState().value
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    TextField(
        value = state.locationQuery,
        onValueChange = {
          addressTextFieldViewModel.setLocationQuery(it)
          expanded = true
        },
        modifier = Modifier.menuAnchor().testTag(AddressTextTestTags.INPUT_LOCATION),
        label = { Text("Address") })
    ExposedDropdownMenu(
        expanded = expanded && state.locationSuggestions.isNotEmpty(),
        onDismissRequest = { expanded = false }) {
          state.locationSuggestions.take(3).forEach { location ->
            DropdownMenuItem(
                text = { Text(location.name) },
                onClick = {
                  addressTextFieldViewModel.setLocation(location)
                  expanded = false
                },
                modifier = Modifier.testTag(AddressTextTestTags.LOCATION_SUGGESTION))
          }
        }
  }
}
