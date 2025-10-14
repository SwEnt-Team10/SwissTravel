package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel

object AddressTextTestTags {
    const val INPUT_LOCATION = "input_location"
    const val LOCATION_SUGGESTION = "location_suggestion"
}

@Composable
fun AddressAutocompleteTextField(
    addressTextFieldViewModel: AddressTextFieldViewModelContract = viewModel<AddressTextFieldViewModel>()
) {
    val state = addressTextFieldViewModel.addressState.collectAsState().value

    Column {
        BasicTextField(
            value = state.locationQuery,
            onValueChange = { addressTextFieldViewModel.setLocationQuery(it) },
            modifier = Modifier.testTag(AddressTextTestTags.INPUT_LOCATION)
        )

        state.locationSuggestions.forEach { location ->
            Text(
                text = location.name,
                modifier = Modifier
                    .clickable { addressTextFieldViewModel.setLocation(location) }
                    .testTag(AddressTextTestTags.LOCATION_SUGGESTION)
            )
        }
    }
}