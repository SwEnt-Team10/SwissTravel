package com.github.swent.swisstravel.ui.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.user.Achievement
import com.github.swent.swisstravel.model.user.AchievementId
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.model.user.toData
import org.junit.Rule
import org.junit.Test

class ProfileAchievementsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun stats(
      totalTrips: Int = 0,
      totalMinutes: Int = 0,
      uniqueLocations: Int = 0,
      mode: TransportMode? = null,
      longestSegmentMin: Int = 0,
  ) =
      UserStats(
          totalTrips = totalTrips,
          totalTravelMinutes = totalMinutes,
          uniqueLocations = uniqueLocations,
          mostUsedTransportMode = mode,
          longestRouteSegmentMin = longestSegmentMin,
      )

  private fun achievementFor(id: AchievementId): Achievement {
    val data = id.toData()
    return Achievement(
        id = data.id,
        label = data.label,
        icon = data.icon,
    )
  }

  @Test
  fun achievementsRow_showsMedals_andOpensDialogOnClick() {
    val rookie = achievementFor(AchievementId.ROOKIE_EXPLORER)
    val global = achievementFor(AchievementId.GLOBAL_NOMAD)

    composeTestRule.setContent {
      MaterialTheme {
        AchievementsDisplay(
            achievements = listOf(rookie, global),
            stats = stats(totalTrips = 12),
            friendsCount = 0,
            isOwnProfile = true,
            profileName = "Me",
        )
      }
    }

    // Row is present
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENTS).assertIsDisplayed()

    // Both medals are shown
    composeTestRule.onAllNodesWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL).assertCountEquals(2)

    // No dialog initially
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_DIALOG).assertDoesNotExist()

    // Click first medal -> dialog appears
    composeTestRule.onAllNodesWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL)[0].performClick()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_DIALOG).assertIsDisplayed()

    // "Tiers" title visible
    composeTestRule.onNodeWithText("Tiers").assertIsDisplayed()

    // 4 tiers for trips
    composeTestRule
        .onAllNodesWithTag(ProfileScreenTestTags.ACHIEVEMENT_TIER_ROW)
        .assertCountEquals(4)
  }

  @Test
  fun achievementDialog_showsCurrentStat_forOwnProfile() {
    val rookie = achievementFor(AchievementId.ROOKIE_EXPLORER)

    composeTestRule.setContent {
      MaterialTheme {
        AchievementsDisplay(
            achievements = listOf(rookie),
            stats = stats(totalTrips = 3),
            friendsCount = 0,
            isOwnProfile = true,
            profileName = "Lionel",
        )
      }
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL).performClick()

    // From strings: "You currently have: %1$d %2$s"
    composeTestRule
        .onNodeWithText("You currently have: 3 total trips completed.")
        .assertIsDisplayed()
  }

  @Test
  fun achievementDialog_showsCurrentStat_forOtherProfile() {
    val rookie = achievementFor(AchievementId.ROOKIE_EXPLORER)

    composeTestRule.setContent {
      MaterialTheme {
        AchievementsDisplay(
            achievements = listOf(rookie),
            stats = stats(totalTrips = 7),
            friendsCount = 0,
            isOwnProfile = false,
            profileName = "Alex",
        )
      }
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL).performClick()

    // From strings: "%1$s currently has: %2$d %3$s"
    composeTestRule
        .onNodeWithText("Alex currently has: 7 total trips completed.")
        .assertIsDisplayed()
  }

  @Test
  fun transportAchievement_ownProfile_showsModeText() {
    val trainAch = achievementFor(AchievementId.TRAIN_ENTHUSIAST)

    composeTestRule.setContent {
      MaterialTheme {
        AchievementsDisplay(
            achievements = listOf(trainAch),
            stats = stats(mode = TransportMode.TRAIN),
            friendsCount = 0,
            isOwnProfile = true,
            profileName = "Me",
        )
      }
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL).performClick()

    // "Your most used transport mode is train."
    composeTestRule.onNodeWithText("Your most used transport mode is train.").assertIsDisplayed()
  }

  @Test
  fun transportAchievement_otherProfile_showsModeTextWithName() {
    val busAch = achievementFor(AchievementId.BUS_BUDDY)

    composeTestRule.setContent {
      MaterialTheme {
        AchievementsDisplay(
            achievements = listOf(busAch),
            stats = stats(mode = TransportMode.BUS),
            friendsCount = 0,
            isOwnProfile = false,
            profileName = "Sarah",
        )
      }
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL).performClick()

    // "%1$s's most used transport mode is %2$s."
    composeTestRule.onNodeWithText("Sarah's most used transport mode is bus.").assertIsDisplayed()
  }

  @Test
  fun transportAchievement_noMode_showsNoneText() {
    val trainAch = achievementFor(AchievementId.TRAIN_ENTHUSIAST)

    // Own profile
    composeTestRule.setContent {
      MaterialTheme {
        AchievementsDisplay(
            achievements = listOf(trainAch),
            stats = stats(mode = null),
            friendsCount = 0,
            isOwnProfile = true,
            profileName = "Me",
        )
      }
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.ACHIEVEMENT_MEDAL).performClick()

    composeTestRule
        .onNodeWithText("You don't have a preferred transport mode yet.")
        .assertIsDisplayed()
  }
}
