package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.activity.WikiImageRepository
import kotlinx.coroutines.FlowPreview

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
 * @param showImages Whether to show images next to the location suggestions.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun LocationAutocompleteTextField(
    modifier: Modifier = Modifier,
    onLocationSelected: (Location) -> Unit = {},
    addressTextFieldViewModel: AddressTextFieldViewModelContract =
        viewModel<DestinationTextFieldViewModel>(),
    name: String = "location",
    clearOnSelect: Boolean = false,
    showImages: Boolean = false,
) {
  val state by addressTextFieldViewModel.addressState.collectAsState()
  var expanded by remember { mutableStateOf(false) }
  val wikiRepo = remember(showImages) { if (showImages) WikiImageRepository.default() else null }

  // Derive text to display from the ViewModel's state
  val textToShow = if (clearOnSelect && state.selectedLocation != null) "" else state.locationQuery

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
        value = textToShow,
        // When the user types, immediately update the ViewModel.
        // The ViewModel is now the single source of truth.
        onValueChange = { newText ->
          addressTextFieldViewModel.setLocationQuery(newText)
          expanded = true
        },
        modifier = modifier.menuAnchor().testTag(LocationTextTestTags.INPUT_LOCATION),
        label = { Text(name) },
        singleLine = true,
        // Error state logic is simplified
        isError = textToShow.isNotEmpty() && state.selectedLocation == null,
        supportingText = {
          if (textToShow.isNotEmpty() && state.selectedLocation == null) {
            Text(text = stringResource(R.string.dropdown_menu_choose))
          }
        })
    DropdownMenu(
        expanded = expanded && state.locationSuggestions.isNotEmpty(),
        onDismissRequest = { expanded = false },
        properties = PopupProperties(focusable = false)) {
          val suggestions = state.locationSuggestions.take(3)
          suggestions.forEachIndexed { index, location ->
            DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showImages && wikiRepo != null) {
                      LocationImage(location = location, wikiRepo = wikiRepo)
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

/**
 * A composable that fetches and displays an image for a given location using the
 * WikiImageRepository. It handles its own state for the image URL.
 *
 * @param location The location to fetch the image for.
 * @param wikiRepo The repository to fetch the image from.
 */
@Composable
private fun LocationImage(location: Location, wikiRepo: WikiImageRepository) {
  var wikiImageUrl by remember(location.name) { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  // Fetch the image URL when the location name changes.
  LaunchedEffect(location.name) { wikiImageUrl = wikiRepo.getImageByName(location.name) }

  if (wikiImageUrl != null) {
    AsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(wikiImageUrl)
                .addHeader("User-Agent", "SwissTravelApp/1.0 (swisstravel.epfl@proton.me)")
                .build(),
        contentDescription = "${location.name} image",
        placeholder = painterResource(id = R.drawable.debug_placeholder),
        error = painterResource(id = R.drawable.debug_placeholder),
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.size(dimensionResource(R.dimen.location_autocomplete_image_padding))
                .clip(CircleShape))
    Spacer(
        modifier = Modifier.width(dimensionResource(R.dimen.location_autocomplete_image_padding)))
  }
}
