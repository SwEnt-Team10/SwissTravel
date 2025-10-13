package com.android.swisstravel.ui.composable

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.ui.composable.CounterTestTags
import com.github.swent.swisstravel.ui.composable.DateSelectorRow
import com.github.swent.swisstravel.ui.composable.DateSelectorTestTags
import com.github.swent.swisstravel.ui.composable.PreferenceSlider
import com.github.swent.swisstravel.ui.composable.PreferenceSwitch
import com.github.swent.swisstravel.ui.composable.PreferenceToggle
import com.github.swent.swisstravel.ui.composable.SliderTestTags
import com.github.swent.swisstravel.ui.composable.SwitchTestTags
import com.github.swent.swisstravel.ui.composable.ToggleTestTags
import com.github.swent.swisstravel.ui.composable.counter
import org.junit.Rule
import org.junit.Test

class ComposableTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun counterTest() {
    val count = mutableStateOf(0)
    composeTestRule.setContent {
      counter(
          label = "test",
          count = count.value,
          onIncrement = { count.value++ },
          onDecrement = { count.value-- })
    }
    composeTestRule.onNodeWithTag("test" + CounterTestTags.COUNTER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CounterTestTags.DECREMENT).performClick()
    assert(count.value == 0)
    composeTestRule.onNodeWithTag(CounterTestTags.INCREMENT).performClick()
    assert(count.value == 1)
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
