package com.github.swent.swisstravel.ui.composable

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class CancelButtonTest {
    @get:Rule val composeTestRule = createComposeRule()
    @Test
    fun cancelButtonIsDisplayed() {
        composeTestRule.setContent {
            CancelButton(
                onCancel = {},
                contentDescription = "testButton"
            )
        }
        composeTestRule.onNodeWithTag(CancelButtonTestTag.CANCEL_BUTTON).isDisplayed()
    }

    @Test
    fun cancelButtonTriggerWhenClick() {
        var isClicked = false
        composeTestRule.setContent {
            CancelButton(
                onCancel = {
                    isClicked = true
                },
                contentDescription = "testButton"
            )
        }
        composeTestRule.onNodeWithTag(CancelButtonTestTag.CANCEL_BUTTON).performClick()
        assert(isClicked)
    }
}