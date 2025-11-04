package com.github.swent.swisstravel.ui.tripSettings

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.displayStringRes
import com.github.swent.swisstravel.ui.tripcreation.TripPreferenceIcon
import com.github.swent.swisstravel.ui.tripcreation.TripPreferenceIconTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripPreferenceIconTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tripPreferenceIcon_hasTagAndIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                TripPreferenceIcon(preference = Preference.PUBLIC_TRANSPORT)
            }
        }

        composeTestRule
            .onNodeWithTag(TripPreferenceIconTestTags.TRIP_PREFERENCE_ICON)
            .assertIsDisplayed()
    }

    @Test
    fun tripPreferenceIcon_displaysCorrectText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedText = context.getString(Preference.PUBLIC_TRANSPORT.displayStringRes())

        composeTestRule.setContent {
            MaterialTheme {
                TripPreferenceIcon(preference = Preference.PUBLIC_TRANSPORT)
            }
        }

        composeTestRule
            .onNodeWithText(expectedText)
            .assertIsDisplayed()
    }
}