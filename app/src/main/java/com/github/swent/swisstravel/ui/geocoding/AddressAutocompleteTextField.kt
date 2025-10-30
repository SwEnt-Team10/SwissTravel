package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

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
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AddressAutocompleteTextField(
    addressTextFieldViewModel: AddressTextFieldViewModelContract =
        viewModel<AddressTextFieldViewModel>(),
    modifier: Modifier = Modifier,
    name: String = "location"
) {
  val state by addressTextFieldViewModel.addressState.collectAsState()
  // Local text state to avoid immediate writes to the ViewModel on every keystroke.
  // This prevents frequent state updates that can cause recomposition/focus loss.
  var text by rememberSaveable { mutableStateOf(state.locationQuery) }
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = text,
        onValueChange = {
          text = it
          // open the dropdown while typing
          expanded = true
        },
        modifier = modifier.menuAnchor().testTag(AddressTextTestTags.INPUT_LOCATION),
        label = { Text(name) })
    ExposedDropdownMenu(
        expanded = expanded && state.locationSuggestions.isNotEmpty(),
        onDismissRequest = { expanded = false }) {
          val suggestions = state.locationSuggestions.take(3)
          suggestions.forEachIndexed { index, location ->
            DropdownMenuItem(
                text = { Text(location.name) },
                onClick = {
                  // Update both ViewModel (selected) and local text state
                  addressTextFieldViewModel.setLocation(location)
                  text = location.name
                  expanded = false
                },
                modifier = Modifier.testTag(AddressTextTestTags.LOCATION_SUGGESTION))

            // Add a divider between items for clarity (but not after the last item)
            if (index < suggestions.lastIndex) {
              HorizontalDivider(
                  modifier = Modifier.fillMaxWidth(),
                  thickness = 1.dp,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
          }
        }
  }

  // When the selectedLocation from the ViewModel changes (e.g., setLocation called
  // from elsewhere), update the local text to reflect it.
  LaunchedEffect(state.selectedLocation) { state.selectedLocation?.let { text = it.name } }

  // Debounce user input and call the ViewModel only after the user stops typing.
  LaunchedEffect(Unit) {
    snapshotFlow { text }
        .debounce(300)
        .distinctUntilChanged()
        .collectLatest { query ->
          // Only call the view model when the query changed and is different from
          // the current ViewModel state to avoid unnecessary network calls.
          if (query != state.locationQuery) {
            addressTextFieldViewModel.setLocationQuery(query)
          }
        }
  }
}
