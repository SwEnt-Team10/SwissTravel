package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/** Test tags for the LocationAutocompleteTextField composable. */
object LocationTextTestTags {
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
 * @param onLocationSelected Callback to be invoked when a location is selected.
 * @param modifier The modifier to be applied to the composable.
 * @param addressTextFieldViewModel The view model that manages the state of the address text field.
 * @param name The label for the text field.
 * @param clearOnSelect Whether to clear the text field after a location is selected.
 */

// Parts of this code was written with the assistance of AI.
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun LocationAutocompleteTextField(
    modifier: Modifier = Modifier,
    onLocationSelected: (Location) -> Unit = {},
    addressTextFieldViewModel: AddressTextFieldViewModelContract =
        viewModel<DestinationTextFieldViewModel>(),
    name: String = "location",
    clearOnSelect: Boolean = false
) {
  val state by addressTextFieldViewModel.addressState.collectAsState()
  // Local text state. This is the single source of truth for what's visible in the text field.
  var text by rememberSaveable { mutableStateOf(state.locationQuery) }
  var expanded by remember { mutableStateOf(false) }

  // Track if we're in the middle of a selection to prevent the text effect from triggering
  var isSelecting by remember { mutableStateOf(false) }

  // This effect synchronizes the text field *from* the ViewModel *to* the UI.
  // It runs ONLY when a selection is made in the ViewModel.
  LaunchedEffect(state.selectedLocation) {
    state.selectedLocation?.let { location ->
      isSelecting = true
      text = if (clearOnSelect) "" else location.name
      // Small delay to ensure the text update is processed before we reset the flag
      kotlinx.coroutines.delay(50)
      isSelecting = false
    }
  }

  // This is the single effect that synchronizes user input *from* the UI *to* the ViewModel.
  LaunchedEffect(Unit) {
    snapshotFlow { text }
        .distinctUntilChanged()
        .debounce(700)
        .collectLatest { currentText ->
          // Skip processing if we're in the middle of a selection
          if (isSelecting) return@collectLatest

          // If text differs from selected location, it means user is typing
          if (currentText != state.selectedLocation?.name) {
            // Clear the selection first
            addressTextFieldViewModel.clearSelectedLocation()

            // Fetch suggestions (this API call will be cancelled if text changes again)
            addressTextFieldViewModel.setLocationQuery(currentText)
          }
        }
  }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = text,
        // onValueChange remains lightweight and lag-free.
        onValueChange = { newText ->
          text = newText
          expanded = true
        },
        modifier = modifier.menuAnchor().testTag(LocationTextTestTags.INPUT_LOCATION),
        label = { Text(name) },
        singleLine = true,
        isError =
            (text.isNotEmpty() && state.selectedLocation == null) || (expanded && text.isEmpty()),
        supportingText = {
          if (expanded && text.isEmpty()) {
            Text("$name ${stringResource(R.string.cannot_be_empty)}")
          } else if (text.isNotEmpty() && state.selectedLocation == null) {
            Text(text = stringResource(R.string.dropdown_menu_choose))
          }
        })
    ExposedDropdownMenu(
        expanded = expanded && state.locationSuggestions.isNotEmpty(),
        onDismissRequest = { expanded = false }) {
          val suggestions = state.locationSuggestions.take(3)
          suggestions.forEachIndexed { index, location ->
            DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (location.imageUrl != null) {
                      AsyncImage(
                          model = location.imageUrl,
                          contentDescription = "${location.name} image",
                          placeholder = painterResource(id = R.drawable.debug_placeholder),
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.size(40.dp).clip(CircleShape))
                      Spacer(modifier = Modifier.width(16.dp))
                    }
                    Text(location.name, modifier = Modifier.weight(1f))
                  }
                },
                onClick = {
                  addressTextFieldViewModel.setLocation(location)
                  onLocationSelected(location)
                  expanded = false
                },
                modifier = Modifier.testTag(LocationTextTestTags.LOCATION_SUGGESTION))

            if (index < suggestions.lastIndex) {
              HorizontalDivider(
                  modifier = Modifier.fillMaxWidth(),
                  thickness = 1.dp,
                  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }
          }
        }
  }
}
