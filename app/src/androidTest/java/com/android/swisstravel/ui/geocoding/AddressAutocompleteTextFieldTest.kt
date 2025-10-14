package com.android.swisstravel.ui.geocoding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.ui.geocoding.AddressAutocompleteTextField
import com.github.swent.swisstravel.ui.geocoding.AddressTextTestTags
import org.junit.Rule
import org.junit.Test

class AddressAutocompleteTextFieldTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testTextFieldAndSuggestionsDisplayed() {
    val fakeViewModel = FakeAddressTextFieldViewModel()

    composeTestRule.setContent {
      AddressAutocompleteTextField(addressTextFieldViewModel = fakeViewModel)
    }

    composeTestRule
        .onNodeWithTag(AddressTextTestTags.INPUT_LOCATION)
        .assertIsDisplayed()
        .performTextInput("Lausanne")

    composeTestRule
        .onAllNodesWithTag(AddressTextTestTags.LOCATION_SUGGESTION)
        .assertAny(hasText("Lausanne"))
  }
}
