package com.github.swent.swisstravel.ui.tripcreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserStats
import com.github.swent.swisstravel.ui.geocoding.FakeAddressTextFieldViewModel
import com.github.swent.swisstravel.ui.geocoding.LocationTextTestTags
import com.github.swent.swisstravel.utils.FakeTripsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fake UserRepository to avoid Firebase. */
class FakeUserRepository : UserRepository {
  override suspend fun getCurrentUser(): User {
    return User(
        uid = "fakeUid123",
        name = "Test User",
        biography = "Fake Bio",
        email = "test@example.com",
        profilePicUrl = "",
        preferences = listOf(Preference.MUSEUMS, Preference.QUICK),
        friends = emptyList(),
        stats = UserStats(),
        pinnedTripsUids = emptyList(),
        pinnedPicturesUids = emptyList(),
        favoriteTripsUids = emptyList())
  }

  override suspend fun getUserByUid(uid: String): User? {
    // no op in tests
    return null
  }

  override suspend fun getUserByNameOrEmail(query: String): List<User> {
    // no op in tests
    return emptyList()
  }

  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {
    // no-op in tests
  }

  override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
    // no-op in tests
  }

  override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
    // no-op in tests
  }

  override suspend fun removeFriend(uid: String, friendUid: String) {
    // no-op in tests
  }

  override suspend fun updateUser(
      uid: String,
      name: String?,
      biography: String?,
      profilePicUrl: String?,
      preferences: List<Preference>?,
      pinnedTripsUids: List<String>?,
      pinnedPicturesUids: List<String>?
  ) {
    // no-op in tests
  }

  override suspend fun addFavoriteTrip(uid: String, tripUid: String) {
    // No-op
  }

  override suspend fun removeFavoriteTrip(uid: String, tripUid: String) {
    // No-op
  }

  override suspend fun updateUserStats(uid: String, stats: UserStats) {
    // no-op in tests
  }
}

val emptyUserRepo =
    object : UserRepository {
      override suspend fun getCurrentUser(): User {
        return User(
            uid = "0",
            name = "",
            biography = "",
            email = "",
            profilePicUrl = "",
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats(),
            pinnedTripsUids = emptyList(),
            pinnedPicturesUids = emptyList(),
            favoriteTripsUids = emptyList())
      }

      override suspend fun getUserByUid(uid: String): User? {
        // no op for tests
        return null
      }

      override suspend fun getUserByNameOrEmail(query: String): List<User> {
        // no op for tests
        return emptyList()
      }

      override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {}

      override suspend fun updateUserStats(uid: String, stats: UserStats) {
        /** no-op for tests* */
      }

      override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
        /** no-op for tests* */
      }

      override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {}

      override suspend fun removeFriend(uid: String, friendUid: String) {}

      override suspend fun updateUser(
          uid: String,
          name: String?,
          biography: String?,
          profilePicUrl: String?,
          preferences: List<Preference>?,
          pinnedTripsUids: List<String>?,
          pinnedPicturesUids: List<String>?
      ) {}

      override suspend fun addFavoriteTrip(uid: String, tripUid: String) {}

      override suspend fun removeFavoriteTrip(uid: String, tripUid: String) {}
    }

class ArrivalDepartureTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val fakeTripSettingsViewModel =
      TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository())
  private val fakeArrivalVm = FakeAddressTextFieldViewModel()
  private val fakeDepartureVm = FakeAddressTextFieldViewModel()

  @Test
  fun arrivalDepartureScreenIsDisplayed() {
    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = fakeTripSettingsViewModel,
          onNext = {},
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ArrivalDepartureTestTags.DEPARTURE_TEXTFIELD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun selectingSuggestionUpdatesState() {
    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = fakeTripSettingsViewModel,
          onNext = {},
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }
    // First, find the specific arrival input field and type into it
    composeTestRule
        .onNode(hasTestTag(ArrivalDepartureTestTags.ARRIVAL_TEXTFIELD), useUnmergedTree = true)
        .performTextInput("test")
    composeTestRule.waitForIdle()
    // Now, click the first suggestion that appears. Indexing is okay here
    // because the suggestions are in a predictable order within the dropdown.
    composeTestRule
        .onAllNodesWithTag(LocationTextTestTags.LOCATION_SUGGESTION)
        .onFirst()
        .performClick()
    assert(fakeArrivalVm.addressState.value.selectedLocation!!.name == "Lausanne")
  }

  @Test
  fun clickingNextButtonTriggersOnNextAndSavesTripProfile() {
    var onNextCalled = false
    val tripSettingsViewModel = TripSettingsViewModel(FakeTripsRepository(), FakeUserRepository())

    // Define the locations that will be "selected"
    val arrivalLocation = Location(Coordinate(46.5197, 6.6323), "Lausanne")
    val departureLocation = Location(Coordinate(46.2044, 6.1432), "Geneva")

    // Pre-set the state in the fake ViewModels as if the user has selected them
    fakeArrivalVm.setLocation(arrivalLocation)
    fakeDepartureVm.setLocation(departureLocation)

    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = tripSettingsViewModel,
          onNext = { onNextCalled = true },
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }

    // Click the button to trigger saveTrip()
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()

    // Assert that the onNext lambda was called
    assert(onNextCalled)

    // Assert that the ViewModel's internal tripProfile is correctly updated
    val finalTripProfile = tripSettingsViewModel.tripSettings.value.arrivalDeparture
    assertEquals(arrivalLocation, finalTripProfile.arrivalLocation)
    assertEquals(departureLocation, finalTripProfile.departureLocation)
  }

  @Test
  fun arrivalAndDepartureAreMandatory() {
    val fakeArrivalVm = FakeAddressTextFieldViewModel()
    val fakeDepartureVm = FakeAddressTextFieldViewModel()
    // Define the locations that will be "selected"
    val arrivalLocation = Location(Coordinate(46.5197, 6.6323), "Lausanne")
    val departureLocation = Location(Coordinate(46.2044, 6.1432), "Geneva")
    var onNextCalled = false

    composeTestRule.setContent {
      ArrivalDepartureScreen(
          viewModel = fakeTripSettingsViewModel,
          onNext = { onNextCalled = true },
          arrivalAddressVm = fakeArrivalVm,
          departureAddressVm = fakeDepartureVm)
    }

    // should not trigger on next if both are empty
    fakeArrivalVm.setLocation(null)
    fakeDepartureVm.setLocation(null)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertFalse(onNextCalled)

    // should not trigger on next if one is empty
    fakeArrivalVm.setLocation(arrivalLocation)
    fakeDepartureVm.setLocation(null)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertFalse(onNextCalled)

    // should not trigger on next if the other is empty
    fakeArrivalVm.setLocation(null)
    fakeDepartureVm.setLocation(departureLocation)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertFalse(onNextCalled)

    // should trigger on next if both are filled
    fakeArrivalVm.setLocation(arrivalLocation)
    fakeDepartureVm.setLocation(departureLocation)
    composeTestRule.onNodeWithTag(ArrivalDepartureTestTags.NEXT_BUTTON).performClick()
    assertTrue(onNextCalled)
  }
}
