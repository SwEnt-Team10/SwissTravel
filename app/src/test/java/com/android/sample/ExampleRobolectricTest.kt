package com.android.sample

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.screen.SecondScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SecondActivityTest : TestCase() {

  @get:Rule val composeTestRule = createAndroidComposeRule<SecondActivity>()

  @Test
  fun test() = run {
    step("Start Second Activity") {
      ComposeScreen.onComposeScreen<SecondScreen>(composeTestRule) {
        simpleText {
          assertIsDisplayed()
          assertTextEquals("Hello Robolectric!")
        }
      }
    }
  }
}
