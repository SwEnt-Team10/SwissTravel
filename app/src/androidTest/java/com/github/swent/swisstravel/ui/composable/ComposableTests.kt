package com.github.swent.swisstravel.ui.composable

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.user.Preference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposableTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun counterTest() {
    val count = mutableStateOf(0)
    composeTestRule.setContent {
      Counter(
          label = "test",
          count = count.value,
          onIncrement = { count.value++ },
          onDecrement = { count.value-- },
          enableButton = count.value > 0)
    }
    composeTestRule.onNodeWithTag("test" + CounterTestTags.COUNTER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CounterTestTags.DECREMENT).performClick()
    assertEquals(0, count.value)
    composeTestRule.onNodeWithTag(CounterTestTags.INCREMENT).performClick()
    assertEquals(1, count.value)
    composeTestRule.onNodeWithTag(CounterTestTags.COUNT).assertIsDisplayed()
  }

  @Test
  fun dateSelectorTest() {
    val clicked = mutableStateOf(false)
    composeTestRule.setContent {
      DateSelectorRow(label = "test", dateText = "test", onClick = { clicked.value = true })
    }
    composeTestRule.onNodeWithTag("test" + DateSelectorTestTags.DATE_SELECTOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DateSelectorTestTags.DATE).performClick()
    assert(clicked.value)
  }

  @Test
  fun preferenceSelectorTest() {
    val selected = mutableStateOf(listOf(Preference.FOODIE, Preference.SPORTS))

    composeTestRule.setContent {
      MaterialTheme {
        PreferenceSelector(
            isChecked = { pref -> selected.value.contains(pref) },
            onCheckedChange = { pref ->
              selected.value =
                  if (selected.value.contains(pref)) selected.value.filter { it != pref }
                  else selected.value + pref
            })
      }
    }

    assertTrue(selected.value.contains(Preference.FOODIE))
    assertTrue(selected.value.contains(Preference.SPORTS))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
    for (preference in Preference.values().filter { it != Preference.WHEELCHAIR_ACCESSIBLE }) {
      composeTestRule
          .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(preference))
          .assertIsDisplayed()
    }
    composeTestRule
        .onNodeWithTag(
            PreferenceSelectorTestTags.getTestTagButton(Preference.WHEELCHAIR_ACCESSIBLE))
        .assertIsNotDisplayed()

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.FOODIE))
        .performClick()
    assertFalse(selected.value.contains(Preference.FOODIE))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.WELLNESS))
        .performClick()
    assertTrue(selected.value.contains(Preference.WELLNESS))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.SPORTS))
        .performClick()
    assertFalse(selected.value.contains(Preference.SPORTS))

    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.getTestTagButton(Preference.FOODIE))
        .performClick()
    assertTrue(selected.value.contains(Preference.FOODIE))
  }

  @Test
  fun preferenceSelectorPreviewContentDisplays() {
    composeTestRule.setContent { PreviewContentPreferenceSelector() }
    composeTestRule
        .onNodeWithTag(PreferenceSelectorTestTags.PREFERENCE_SELECTOR)
        .assertIsDisplayed()
  }

  @Test
  fun sliderTest() {
    val value = mutableStateOf(0)
    composeTestRule.setContent {
      PreferenceSlider(label = "test", value = value.value, onValueChange = { value.value = it })
    }
    composeTestRule.onNodeWithTag("test" + SliderTestTags.SLIDER).assertIsDisplayed()
  }

  @Test
  fun switchTest() {
    val checked = mutableStateOf(false)
    composeTestRule.setContent {
      PreferenceSwitch(
          label = "test", checked = checked.value, onCheckedChange = { checked.value = it })
    }
    composeTestRule.onNodeWithTag("test" + SwitchTestTags.SWITCH_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SwitchTestTags.SWITCH).performClick()
    assert(checked.value)
  }

  @Test
  fun toggleTest() {
    val value = mutableStateOf(false)
    composeTestRule.setContent {
      PreferenceToggle(label = "test", value = value.value, onValueChange = { value.value = it })
    }
    composeTestRule.onNodeWithTag("test" + ToggleTestTags.TOGGLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ToggleTestTags.YES).performClick()
    assert(value.value)
    composeTestRule.onNodeWithTag(ToggleTestTags.NO).performClick()
    assert(!value.value)
  }
}
