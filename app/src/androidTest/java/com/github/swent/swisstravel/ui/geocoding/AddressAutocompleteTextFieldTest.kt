package com.github.swent.swisstravel.ui.geocoding

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import org.junit.Rule
import org.junit.Test

class AddressAutocompleteTextFieldTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setContentWithFakeViewModel(fakeViewModel: FakeAddressTextFieldViewModel) {
    composeTestRule.setContent {
      SwissTravelTheme { LocationAutocompleteTextField(addressTextFieldViewModel = fakeViewModel) }
    }
  }

  @Test
  fun testTextFieldAndSuggestionsDisplayed() {
    val fakeViewModel = FakeAddressTextFieldViewModel()
    setContentWithFakeViewModel(fakeViewModel)

    composeTestRule
        .onNodeWithTag(LocationTextTestTags.INPUT_LOCATION)
        .assertIsDisplayed()
        .performTextInput("Lausanne")

    composeTestRule
        .onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION)
        .assertAny(hasText("Lausanne"))
  }

  @Test
  fun testSuggestionsDisappearAfterSelection() {
    val fakeViewModel = FakeAddressTextFieldViewModel()
    setContentWithFakeViewModel(fakeViewModel)

    composeTestRule.onNodeWithTag(LocationTextTestTags.INPUT_LOCATION).performTextInput("Genève")

    composeTestRule
        .onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION)
        .onFirst()
        .performClick()

    composeTestRule.onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION).assertCountEquals(0)

    composeTestRule.onNodeWithTag(LocationTextTestTags.INPUT_LOCATION)
  }

  @Test
  fun testClearTextField() {
    val fakeViewModel = FakeAddressTextFieldViewModel()
    setContentWithFakeViewModel(fakeViewModel)

    composeTestRule.onNodeWithTag(LocationTextTestTags.INPUT_LOCATION).performTextReplacement("")

    composeTestRule.onNodeWithTag(LocationTextTestTags.INPUT_LOCATION)
  }

  @Test
  fun testEmptyInputShowsNoSuggestions() {
    val fakeViewModel = FakeAddressTextFieldViewModel()
    setContentWithFakeViewModel(fakeViewModel)

    composeTestRule.onNodeWithTag(LocationTextTestTags.INPUT_LOCATION).performTextReplacement("")

    composeTestRule.onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION).assertCountEquals(0)
  }
}
