package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.TransportMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AchievementTest {

  // --- Helpers -------------------------------------------------------------

  private fun stats(
      totalTrips: Int = 0,
      totalMinutes: Int = 0,
      uniqueLocations: Int = 0,
      mode: TransportMode? = null,
      longestSegmentMin: Int = 0,
  ): UserStats =
      UserStats(
          totalTrips = totalTrips,
          totalTravelMinutes = totalMinutes,
          uniqueLocations = uniqueLocations,
          mostUsedTransportMode = mode,
          longestRouteSegmentMin = longestSegmentMin,
      )

  private fun List<Achievement>.has(id: AchievementId): Boolean = any { it.id == id }

  // --- computeAchievements: trip count ------------------------------------

  @Test
  fun `trip achievements - no trips gives no trip medal`() {
    val achievements = computeAchievements(stats = stats(totalTrips = 0), friendsCount = 0)

    assertFalse(achievements.has(AchievementId.ROOKIE_EXPLORER))
    assertFalse(achievements.has(AchievementId.WEEKEND_ADVENTURER))
    assertFalse(achievements.has(AchievementId.FREQUENT_TRAVELER))
    assertFalse(achievements.has(AchievementId.GLOBAL_NOMAD))
  }

  @Test
  fun `trip achievements - thresholds pick highest tier only`() {
    fun assertTripTier(trips: Int, expected: AchievementId?) {
      val achievements = computeAchievements(stats = stats(totalTrips = trips), friendsCount = 0)

      val allTripIds =
          listOf(
              AchievementId.ROOKIE_EXPLORER,
              AchievementId.WEEKEND_ADVENTURER,
              AchievementId.FREQUENT_TRAVELER,
              AchievementId.GLOBAL_NOMAD,
          )

      allTripIds.forEach { id ->
        if (id == expected) {
          assertTrue(achievements.has(id), "Expected $id for $trips trips")
        } else {
          assertFalse(achievements.has(id), "Did not expect $id for $trips trips")
        }
      }
    }

    assertTripTier(1, AchievementId.ROOKIE_EXPLORER)
    assertTripTier(4, AchievementId.ROOKIE_EXPLORER)
    assertTripTier(5, AchievementId.WEEKEND_ADVENTURER)
    assertTripTier(9, AchievementId.WEEKEND_ADVENTURER)
    assertTripTier(10, AchievementId.FREQUENT_TRAVELER)
    assertTripTier(19, AchievementId.FREQUENT_TRAVELER)
    assertTripTier(20, AchievementId.GLOBAL_NOMAD)
    assertTripTier(50, AchievementId.GLOBAL_NOMAD)
  }

  // --- computeAchievements: travel time -----------------------------------

  @Test
  fun `time achievements - thresholds pick highest tier only`() {
    fun assertTimeTier(minutes: Int, expected: AchievementId?) {
      val achievements =
          computeAchievements(stats = stats(totalMinutes = minutes), friendsCount = 0)

      val allIds =
          listOf(
              AchievementId.JUST_WARMING_UP,
              AchievementId.MILEAGE_MAKER,
              AchievementId.ENDURANCE_TRAVELER,
              AchievementId.TIME_LORD,
          )

      allIds.forEach { id ->
        if (id == expected) {
          assertTrue(achievements.has(id), "Expected $id for $minutes minutes")
        } else {
          assertFalse(achievements.has(id), "Did not expect $id for $minutes minutes")
        }
      }
    }

    assertTimeTier(0, null)
    assertTimeTier(100, AchievementId.JUST_WARMING_UP)
    assertTimeTier(999, AchievementId.JUST_WARMING_UP)
    assertTimeTier(1_000, AchievementId.MILEAGE_MAKER)
    assertTimeTier(4_999, AchievementId.MILEAGE_MAKER)
    assertTimeTier(5_000, AchievementId.ENDURANCE_TRAVELER)
    assertTimeTier(19_999, AchievementId.ENDURANCE_TRAVELER)
    assertTimeTier(20_000, AchievementId.TIME_LORD)
    assertTimeTier(50_000, AchievementId.TIME_LORD)
  }

  // --- computeAchievements: unique locations ------------------------------

  @Test
  fun `location achievements - thresholds pick highest tier only`() {
    fun assertLocationTier(locations: Int, expected: AchievementId?) {
      val achievements =
          computeAchievements(stats = stats(uniqueLocations = locations), friendsCount = 0)

      val allIds =
          listOf(
              AchievementId.LOCAL_TOURIST,
              AchievementId.CITY_HOPPER,
              AchievementId.CONTINENT_CRAWLER,
              AchievementId.GLOBE_TROTTER,
          )

      allIds.forEach { id ->
        if (id == expected) {
          assertTrue(achievements.has(id), "Expected $id for $locations locations")
        } else {
          assertFalse(achievements.has(id), "Did not expect $id for $locations locations")
        }
      }
    }

    assertLocationTier(0, null)
    assertLocationTier(3, AchievementId.LOCAL_TOURIST)
    assertLocationTier(9, AchievementId.LOCAL_TOURIST)
    assertLocationTier(10, AchievementId.CITY_HOPPER)
    assertLocationTier(19, AchievementId.CITY_HOPPER)
    assertLocationTier(20, AchievementId.CONTINENT_CRAWLER)
    assertLocationTier(49, AchievementId.CONTINENT_CRAWLER)
    assertLocationTier(50, AchievementId.GLOBE_TROTTER)
    assertLocationTier(100, AchievementId.GLOBE_TROTTER)
  }

  // --- computeAchievements: longest route segment -------------------------

  @Test
  fun `longest segment achievements - thresholds pick highest tier only`() {
    fun assertLongestTier(minutes: Int, expected: AchievementId?) {
      val achievements =
          computeAchievements(stats = stats(longestSegmentMin = minutes), friendsCount = 0)

      val allIds =
          listOf(
              AchievementId.SHORT_HOP,
              AchievementId.JOURNEYMAN,
              AchievementId.LONG_HAULER,
              AchievementId.IRON_ROUTE_CHAMPION,
          )

      allIds.forEach { id ->
        if (id == expected) {
          assertTrue(achievements.has(id), "Expected $id for $minutes min segment")
        } else {
          assertFalse(achievements.has(id), "Did not expect $id for $minutes min segment")
        }
      }
    }

    assertLongestTier(0, null)
    assertLongestTier(15, AchievementId.SHORT_HOP)
    assertLongestTier(59, AchievementId.SHORT_HOP)
    assertLongestTier(60, AchievementId.JOURNEYMAN)
    assertLongestTier(179, AchievementId.JOURNEYMAN)
    assertLongestTier(180, AchievementId.LONG_HAULER)
    assertLongestTier(599, AchievementId.LONG_HAULER)
    assertLongestTier(600, AchievementId.IRON_ROUTE_CHAMPION)
    assertLongestTier(1000, AchievementId.IRON_ROUTE_CHAMPION)
  }

  // --- computeAchievements: social friends count --------------------------

  @Test
  fun `social achievements - thresholds pick highest tier only`() {
    fun assertSocialTier(friends: Int, expected: AchievementId?) {
      val achievements = computeAchievements(stats = stats(), friendsCount = friends)

      val allIds =
          listOf(
              AchievementId.NEW_FRIEND,
              AchievementId.SOCIAL_TRAVELER,
              AchievementId.POPULAR_GUIDE,
              AchievementId.LEGENDARY_CONNECTOR,
          )

      allIds.forEach { id ->
        if (id == expected) {
          assertTrue(achievements.has(id), "Expected $id for $friends friends")
        } else {
          assertFalse(achievements.has(id), "Did not expect $id for $friends friends")
        }
      }
    }

    assertSocialTier(0, null)
    assertSocialTier(1, AchievementId.NEW_FRIEND)
    assertSocialTier(4, AchievementId.NEW_FRIEND)
    assertSocialTier(5, AchievementId.SOCIAL_TRAVELER)
    assertSocialTier(9, AchievementId.SOCIAL_TRAVELER)
    assertSocialTier(10, AchievementId.POPULAR_GUIDE)
    assertSocialTier(19, AchievementId.POPULAR_GUIDE)
    assertSocialTier(20, AchievementId.LEGENDARY_CONNECTOR)
    assertSocialTier(50, AchievementId.LEGENDARY_CONNECTOR)
  }

  // --- computeAchievements: transport mode non-tiered ---------------------

  @Test
  fun `transport achievements - one non-tiered medal for most used mode`() {
    fun assertMode(mode: TransportMode?, expected: AchievementId?) {
      val achievements = computeAchievements(stats = stats(mode = mode), friendsCount = 0)

      val allIds =
          listOf(
              AchievementId.TRAIN_ENTHUSIAST,
              AchievementId.ROAD_WARRIOR,
              AchievementId.BUS_BUDDY,
              AchievementId.ECO_RIDER,
          )

      allIds.forEach { id ->
        if (id == expected) {
          assertTrue(achievements.has(id), "Expected $id for mode=$mode")
        } else {
          assertFalse(achievements.has(id), "Did not expect $id for mode=$mode")
        }
      }
    }

    assertMode(TransportMode.TRAIN, AchievementId.TRAIN_ENTHUSIAST)
    assertMode(TransportMode.CAR, AchievementId.ROAD_WARRIOR)
    assertMode(TransportMode.BUS, AchievementId.BUS_BUDDY)
    assertMode(TransportMode.TRAM, AchievementId.ECO_RIDER)
    assertMode(null, null)
  }

  // --- AchievementId -> AchievementData mapping ---------------------------

  @Test
  fun `toData maps every AchievementId to an AchievementData with same id`() {
    AchievementId.values().forEach { id ->
      val data = id.toData()
      assertEquals(id, data.id, "toData should preserve id for $id")
    }
  }

  // --- AchievementId - category mapping -----------------------------------

  @Test
  fun `every achievement id maps to expected category`() {
    // Just ensure the mapping does not throw and is exhaustive
    AchievementId.values().forEach { id ->
      val category = id.category()
      // Sanity check: category has this id in its tier list, except transport mode
      val idsInCategory = category.tiers()
      assertTrue(id in idsInCategory, "Expected $id to be present in tiers() of category $category")
    }
  }

  // --- AchievementCategory - tiers ----------------------------------------

  @Test
  fun `tiers returns 4 ids per category`() {
    AchievementCategory.values().forEach { category ->
      val ids = category.tiers()
      assertEquals(4, ids.size, "Each category should have exactly 4 tiers")
    }
  }

  // --- AchievementCategory - displayStringRes -----------------------------

  @Test
  fun `displayStringRes returns non zero resource id for each category`() {
    AchievementCategory.values().forEach { category ->
      val res = category.displayStringRes()
      assertTrue(res != 0, "displayStringRes should return a valid resource id for $category")
    }
  }
}
