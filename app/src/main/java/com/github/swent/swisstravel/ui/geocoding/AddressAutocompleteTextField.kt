package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object AddressTextTestTags {
    const val INPUT_LOCATION = "inputLocation"
    const val LOCATION_SUGGESTION = "locationSuggestion"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressAutocompleteTextField(
    addressTextFieldViewModel: AddressTextFieldViewModel = viewModel()
) {
    val textFieldState by addressTextFieldViewModel.addressState.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }

    val locationSuggestions = textFieldState.locationSuggestions
    val locationQuery = textFieldState.locationQuery

    ExposedDropdownMenuBox(
        expanded = showDropdown && locationSuggestions.isNotEmpty(),
        onExpandedChange = { showDropdown = it },
    ) {
        OutlinedTextField(
            value = locationQuery,
            onValueChange = {
                addressTextFieldViewModel.setLocationQuery(it)
                showDropdown = true
            },
            modifier =
                Modifier.menuAnchor()
                    .fillMaxWidth()
                    .testTag(AddressTextTestTags.INPUT_LOCATION),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = showDropdown && locationSuggestions.isNotEmpty(),
            onDismissRequest = { showDropdown = false }) {
            locationSuggestions.take(3).forEach { location ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                location.name.take(30) +
                                        if (location.name.length > 30) "..."
                                        else "", // Limit name length
                            maxLines = 1 // Ensure name doesn't overflow
                        )
                    },
                    onClick = {
                        addressTextFieldViewModel.setLocationQuery(location.name)
                        addressTextFieldViewModel.setLocation(location)
                        showDropdown = false // Close dropdown on selection
                    },
                    modifier =
                        Modifier.padding(8.dp)
                            .testTag(AddressTextTestTags.LOCATION_SUGGESTION))
            }

            if (locationSuggestions.size > 3) {
                DropdownMenuItem(
                    text = { Text("More...") },
                    onClick = {},
                    modifier = Modifier.padding(8.dp))
            }
        }
    }
}