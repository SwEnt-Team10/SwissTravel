package com.github.swent.swisstravel.ui.friends

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.theme.SwissTravelTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendElementTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun sampleUser(uid: String = "uid1", name: String = "Alice Friend"): User =
      User(
          uid = uid,
          name = name,
          biography = "",
          email = "alice@example.com",
          profilePicUrl = "",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats(),
          pinnedTripsUids = emptyList(),
          pinnedPicturesUids = emptyList(),
          favoriteTripsUids = emptyList())

  @Test
  fun friendElement_showsArrowWhenNotPendingOrNotShouldAccept() {
    val user = sampleUser()

    composeRule.setContent {
      SwissTravelTheme {
        FriendElement(
            userToDisplay = user, onClick = {}, isPendingRequest = false, shouldAccept = false)
      }
    }

    // Arrow is visible
    composeRule
        .onNodeWithTag(FriendElementTestTags.ARROW_ICON, useUnmergedTree = true)
        .assertExists()

    // Accept / decline buttons are not shown
    composeRule.onNodeWithTag(FriendElementTestTags.ACCEPT_BUTTON).assertDoesNotExist()
    composeRule.onNodeWithTag(FriendElementTestTags.DECLINE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun friendElement_showsAcceptAndDeclineWhenPendingAndShouldAccept() {
    val user = sampleUser()

    var acceptCalled = false
    var declineCalled = false

    composeRule.setContent {
      SwissTravelTheme {
        FriendElement(
            userToDisplay = user,
            onClick = {},
            isPendingRequest = true,
            shouldAccept = true,
            onAccept = { acceptCalled = true },
            onDecline = { declineCalled = true })
      }
    }

    // Arrow should not be visible
    composeRule.onNodeWithTag(FriendElementTestTags.ARROW_ICON).assertDoesNotExist()

    // Accept / decline buttons are visible
    composeRule.onNodeWithTag(FriendElementTestTags.ACCEPT_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FriendElementTestTags.DECLINE_BUTTON).assertIsDisplayed()

    // Click accept
    composeRule.onNodeWithTag(FriendElementTestTags.ACCEPT_BUTTON).performClick()
    assertEquals(true, acceptCalled)

    // Click decline
    composeRule.onNodeWithTag(FriendElementTestTags.DECLINE_BUTTON).performClick()
    assertEquals(true, declineCalled)
  }

  @Test
  fun friendElement_cardClickInvokesOnClickAndShowsName() {
    val user = sampleUser(uid = "friend42", name = "Bob Friend")
    var clicked = false

    composeRule.setContent {
      SwissTravelTheme {
        FriendElement(
            userToDisplay = user,
            onClick = { clicked = true },
            isPendingRequest = false,
            shouldAccept = false)
      }
    }

    // Card has correct test tag
    val cardTag = FriendElementTestTags.getTestTagForFriend(user)
    composeRule.onNodeWithTag(cardTag).assertIsDisplayed()

    // Name is shown
    composeRule.onNodeWithText("Bob Friend").assertIsDisplayed()

    // Clicking the card triggers onClick
    composeRule.onNodeWithTag(cardTag).performClick()
    assertEquals(true, clicked)
  }
}
