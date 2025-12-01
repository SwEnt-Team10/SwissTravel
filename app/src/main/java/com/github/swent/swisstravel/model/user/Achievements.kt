package com.github.swent.swisstravel.model.user

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.TransportMode

/** Logical ids for achievements. */
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

/** Single achievement instance. */
data class Achievement(
    val id: AchievementId,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
)

/** Enum linking logical ids to string + drawable resources. */
enum class AchievementData(
    val id: AchievementId,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
  // 1) Trip Count
  ROOKIE_EXPLORER_DATA(
      AchievementId.ROOKIE_EXPLORER,
      R.string.achievement_rookie_explorer,
      R.drawable.ic_rookie_explorer),
  WEEKEND_ADVENTURER_DATA(
      AchievementId.WEEKEND_ADVENTURER,
      R.string.achievement_weekend_adventurer,
      R.drawable.ic_weekend_adventurer),
  FREQUENT_TRAVELER_DATA(
      AchievementId.FREQUENT_TRAVELER,
      R.string.achievement_frequent_traveler,
      R.drawable.ic_frequent_traveler),
  GLOBAL_NOMAD_DATA(
      AchievementId.GLOBAL_NOMAD, R.string.achievement_global_nomad, R.drawable.ic_global_nomad),

  // 2) Travel Time
  JUST_WARMING_UP_DATA(
      AchievementId.JUST_WARMING_UP,
      R.string.achievement_just_warming_up,
      R.drawable.ic_just_warming_up),
  MILEAGE_MAKER_DATA(
      AchievementId.MILEAGE_MAKER, R.string.achievement_mileage_maker, R.drawable.ic_mileage_maker),
  ENDURANCE_TRAVELER_DATA(
      AchievementId.ENDURANCE_TRAVELER,
      R.string.achievement_endurance_traveler,
      R.drawable.ic_endurance_traveler),
  TIME_LORD_DATA(AchievementId.TIME_LORD, R.string.achievement_time_lord, R.drawable.ic_time_lord),

  // 3) Unique Locations
  LOCAL_TOURIST_DATA(
      AchievementId.LOCAL_TOURIST, R.string.achievement_local_tourist, R.drawable.ic_local_tourist),
  CITY_HOPPER_DATA(
      AchievementId.CITY_HOPPER, R.string.achievement_city_hopper, R.drawable.ic_city_hopper),
  CONTINENT_CRAWLER_DATA(
      AchievementId.CONTINENT_CRAWLER,
      R.string.achievement_continent_crawler,
      R.drawable.ic_continent_crawler),
  GLOBE_TROTTER_DATA(
      AchievementId.GLOBE_TROTTER, R.string.achievement_globe_trotter, R.drawable.ic_globe_trotter),

  // 4) Transport Mode
  TRAIN_ENTHUSIAST_DATA(
      AchievementId.TRAIN_ENTHUSIAST,
      R.string.achievement_train_enthusiast,
      R.drawable.ic_train_enthusiast),
  ROAD_WARRIOR_DATA(
      AchievementId.ROAD_WARRIOR, R.string.achievement_road_warrior, R.drawable.ic_road_warrior),
  BUS_BUDDY_DATA(AchievementId.BUS_BUDDY, R.string.achievement_bus_buddy, R.drawable.ic_bus_buddy),
  ECO_RIDER_DATA(AchievementId.ECO_RIDER, R.string.achievement_eco_rider, R.drawable.ic_eco_rider),

  // 5) Longest Route Segment
  SHORT_HOP_DATA(AchievementId.SHORT_HOP, R.string.achievement_short_hop, R.drawable.ic_short_hop),
  JOURNEYMAN_DATA(
      AchievementId.JOURNEYMAN, R.string.achievement_journeyman, R.drawable.ic_journeyman),
  LONG_HAULER_DATA(
      AchievementId.LONG_HAULER, R.string.achievement_long_hauler, R.drawable.ic_long_hauler),
  IRON_ROUTE_CHAMPION_DATA(
      AchievementId.IRON_ROUTE_CHAMPION,
      R.string.achievement_iron_route_champion,
      R.drawable.ic_iron_route_champion),

  // 6) Social / Friends
  NEW_FRIEND_DATA(
      AchievementId.NEW_FRIEND, R.string.achievement_new_friend, R.drawable.ic_new_friend),
  SOCIAL_TRAVELER_DATA(
      AchievementId.SOCIAL_TRAVELER,
      R.string.achievement_social_traveler,
      R.drawable.ic_social_traveler),
  POPULAR_GUIDE_DATA(
      AchievementId.POPULAR_GUIDE, R.string.achievement_popular_guide, R.drawable.ic_popular_guide),
  LEGENDARY_CONNECTOR_DATA(
      AchievementId.LEGENDARY_CONNECTOR,
      R.string.achievement_legendary_connector,
      R.drawable.ic_legendary_connector),
}

/** Helper: find the data for a given id. */
private fun AchievementId.toData(): AchievementData =
    AchievementData.values().first { it.id == this }

/** Compute unlocked achievements from user stats + friend count. */
fun computeAchievements(
    stats: UserStats,
    friendsCount: Int,
): List<Achievement> {
  val result = mutableListOf<Achievement>()

  fun add(id: AchievementId) {
    val data = id.toData()
    result +=
        Achievement(
            id = data.id,
            label = data.label,
            icon = data.icon,
        )
  }

  fun addHighestTier(value: Int, thresholds: List<Pair<Int, AchievementId>>) {
    val best = thresholds.lastOrNull { (required, _) -> value >= required }
    if (best != null) add(best.second)
  }

  // 1) Trip Count
  addHighestTier(
      stats.totalTrips,
      listOf(
          1 to AchievementId.ROOKIE_EXPLORER,
          5 to AchievementId.WEEKEND_ADVENTURER,
          10 to AchievementId.FREQUENT_TRAVELER,
          20 to AchievementId.GLOBAL_NOMAD,
      ))

  // 2) Travel Time
  addHighestTier(
      stats.totalTravelMinutes,
      listOf(
          100 to AchievementId.JUST_WARMING_UP,
          1_000 to AchievementId.MILEAGE_MAKER,
          5_000 to AchievementId.ENDURANCE_TRAVELER,
          20_000 to AchievementId.TIME_LORD,
      ))

  // 3) Unique Locations
  addHighestTier(
      stats.uniqueLocations,
      listOf(
          3 to AchievementId.LOCAL_TOURIST,
          10 to AchievementId.CITY_HOPPER,
          20 to AchievementId.CONTINENT_CRAWLER,
          50 to AchievementId.GLOBE_TROTTER,
      ))

  // 4) Transport Mode (non-tiered)
  when (stats.mostUsedTransportMode) {
    TransportMode.TRAIN -> add(AchievementId.TRAIN_ENTHUSIAST)
    TransportMode.CAR -> add(AchievementId.ROAD_WARRIOR)
    TransportMode.BUS -> add(AchievementId.BUS_BUDDY)
    TransportMode.TRAM -> add(AchievementId.ECO_RIDER)
    else -> Unit
  }

  // 5) Longest Route Segment
  addHighestTier(
      stats.longestRouteSegmentMin,
      listOf(
          15 to AchievementId.SHORT_HOP,
          60 to AchievementId.JOURNEYMAN,
          180 to AchievementId.LONG_HAULER,
          600 to AchievementId.IRON_ROUTE_CHAMPION,
      ))

  // 6) Social / Friends
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
