package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.TransportMode

/** Enum describing all achievements. */
enum class AchievementId {
  // 1) Trip Count
  ROOKIE_EXPLORER,
  WEEKEND_ADVENTURER,
  FREQUENT_TRAVELER,
  GLOBAL_NOMAD,

  // 2) Travel Time
  JUST_WARMING_UP,
  MILEAGE_MAKER,
  ENDURANCE_TRAVELER,
  TIME_LORD,

  // 3) Unique Locations
  LOCAL_TOURIST,
  CITY_HOPPER,
  CONTINENT_CRAWLER,
  GLOBE_TROTTER,

  // 4) Transport Mode
  TRAIN_ENTHUSIAST,
  ROAD_WARRIOR,
  BUS_BUDDY,
  ECO_RIDER,

  // 5) Longest Route Segment
  SHORT_HOP,
  JOURNEYMAN,
  LONG_HAULER,
  IRON_ROUTE_CHAMPION,

  // 6) Social / Friends
  NEW_FRIEND,
  SOCIAL_TRAVELER,
  POPULAR_GUIDE,
  LEGENDARY_CONNECTOR,
}

/**
 * A single achievement instance for a user.
 *
 * @property id Logical id (use it for i18n / icons in the UI)
 */
data class Achievement(
    val id: AchievementId,
)

/** Compute unlocked achievements from user stats + friend count. */
fun computeAchievements(
    stats: UserStats,
    friendsCount: Int,
): List<Achievement> {
  val result = mutableListOf<Achievement>()

  fun add(id: AchievementId) {
    result += Achievement(id)
  }

  // Helper for tiered achievements:
  // thresholds must be sorted ascending by required value.
  fun addHighestTier(value: Int, thresholds: List<Pair<Int, AchievementId>>) {
    val best = thresholds.lastOrNull { (required, _) -> value >= required }
    if (best != null) {
      add(best.second)
    }
  }

  // 1) Trip Count (totalTrips)
  addHighestTier(
      stats.totalTrips,
      listOf(
          1 to AchievementId.ROOKIE_EXPLORER,
          5 to AchievementId.WEEKEND_ADVENTURER,
          10 to AchievementId.FREQUENT_TRAVELER,
          20 to AchievementId.GLOBAL_NOMAD,
      ))

  // 2) Travel Time (totalTravelMinutes)
  addHighestTier(
      stats.totalTravelMinutes,
      listOf(
          100 to AchievementId.JUST_WARMING_UP,
          1_000 to AchievementId.MILEAGE_MAKER,
          5_000 to AchievementId.ENDURANCE_TRAVELER,
          20_000 to AchievementId.TIME_LORD,
      ))

  // 3) Unique Locations (uniqueLocations)
  addHighestTier(
      stats.uniqueLocations,
      listOf(
          3 to AchievementId.LOCAL_TOURIST,
          10 to AchievementId.CITY_HOPPER,
          20 to AchievementId.CONTINENT_CRAWLER,
          50 to AchievementId.GLOBE_TROTTER,
      ))

  // 4) Transport Mode (mostUsedTransportMode)
  // (non-tiered — only one per user)
  when (stats.mostUsedTransportMode) {
    TransportMode.TRAIN -> add(AchievementId.TRAIN_ENTHUSIAST)
    TransportMode.CAR -> add(AchievementId.ROAD_WARRIOR)
    TransportMode.BUS -> add(AchievementId.BUS_BUDDY)
    TransportMode.TRAM -> add(AchievementId.ECO_RIDER)
    else -> Unit
  }

  // 5) Longest Route Segment (longestRouteSegmentMin)
  addHighestTier(
      stats.longestRouteSegmentMin,
      listOf(
          15 to AchievementId.SHORT_HOP,
          60 to AchievementId.JOURNEYMAN,
          180 to AchievementId.LONG_HAULER,
          600 to AchievementId.IRON_ROUTE_CHAMPION,
      ))

  // 6) Social / Friends (friendsCount)
  addHighestTier(
      friendsCount,
      listOf(
          1 to AchievementId.NEW_FRIEND,
          5 to AchievementId.SOCIAL_TRAVELER,
          10 to AchievementId.POPULAR_GUIDE,
          20 to AchievementId.LEGENDARY_CONNECTOR,
      ))

  return result
}
