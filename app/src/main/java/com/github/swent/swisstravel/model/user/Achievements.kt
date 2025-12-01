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

/** Enum linking logical ids to string + condition to get the achievement + drawable resources. */
enum class AchievementData(
    val id: AchievementId,
    @StringRes val label: Int,
    @StringRes val condition: Int,
    @DrawableRes val icon: Int,
) {
  // 1) Trip Count
  ROOKIE_EXPLORER_DATA(
      AchievementId.ROOKIE_EXPLORER,
      R.string.achievement_rookie_explorer,
      R.string.achievement_rookie_explorer_condition,
      R.drawable.ic_rookie_explorer),
  WEEKEND_ADVENTURER_DATA(
      AchievementId.WEEKEND_ADVENTURER,
      R.string.achievement_weekend_adventurer,
      R.string.achievement_weekend_adventurer_condition,
      R.drawable.ic_weekend_adventurer),
  FREQUENT_TRAVELER_DATA(
      AchievementId.FREQUENT_TRAVELER,
      R.string.achievement_frequent_traveler,
      R.string.achievement_frequent_traveler_condition,
      R.drawable.ic_frequent_traveler),
  GLOBAL_NOMAD_DATA(
      AchievementId.GLOBAL_NOMAD,
      R.string.achievement_global_nomad,
      R.string.achievement_global_nomad_condition,
      R.drawable.ic_global_nomad),

  // 2) Travel Time
  JUST_WARMING_UP_DATA(
      AchievementId.JUST_WARMING_UP,
      R.string.achievement_just_warming_up,
      R.string.achievement_just_warming_up_condition,
      R.drawable.ic_just_warming_up),
  MILEAGE_MAKER_DATA(
      AchievementId.MILEAGE_MAKER,
      R.string.achievement_mileage_maker,
      R.string.achievement_mileage_maker_condition,
      R.drawable.ic_mileage_maker),
  ENDURANCE_TRAVELER_DATA(
      AchievementId.ENDURANCE_TRAVELER,
      R.string.achievement_endurance_traveler,
      R.string.achievement_endurance_traveler_condition,
      R.drawable.ic_endurance_traveler),
  TIME_LORD_DATA(
      AchievementId.TIME_LORD,
      R.string.achievement_time_lord,
      R.string.achievement_time_lord_condition,
      R.drawable.ic_time_lord),

  // 3) Unique Locations
  LOCAL_TOURIST_DATA(
      AchievementId.LOCAL_TOURIST,
      R.string.achievement_local_tourist,
      R.string.achievement_local_tourist_condition,
      R.drawable.ic_local_tourist),
  CITY_HOPPER_DATA(
      AchievementId.CITY_HOPPER,
      R.string.achievement_city_hopper,
      R.string.achievement_city_hopper_condition,
      R.drawable.ic_city_hopper),
  CONTINENT_CRAWLER_DATA(
      AchievementId.CONTINENT_CRAWLER,
      R.string.achievement_continent_crawler,
      R.string.achievement_continent_crawler_condition,
      R.drawable.ic_continent_crawler),
  GLOBE_TROTTER_DATA(
      AchievementId.GLOBE_TROTTER,
      R.string.achievement_globe_trotter,
      R.string.achievement_globe_trotter_condition,
      R.drawable.ic_globe_trotter),

  // 4) Transport Mode
  TRAIN_ENTHUSIAST_DATA(
      AchievementId.TRAIN_ENTHUSIAST,
      R.string.achievement_train_enthusiast,
      R.string.achievement_train_enthusiast_condition,
      R.drawable.ic_train_enthusiast),
  ROAD_WARRIOR_DATA(
      AchievementId.ROAD_WARRIOR,
      R.string.achievement_road_warrior,
      R.string.achievement_road_warrior_condition,
      R.drawable.ic_road_warrior),
  BUS_BUDDY_DATA(
      AchievementId.BUS_BUDDY,
      R.string.achievement_bus_buddy,
      R.string.achievement_bus_buddy_condition,
      R.drawable.ic_bus_buddy),
  ECO_RIDER_DATA(
      AchievementId.ECO_RIDER,
      R.string.achievement_eco_rider,
      R.string.achievement_eco_rider_condition,
      R.drawable.ic_eco_rider),

  // 5) Longest Route Segment
  SHORT_HOP_DATA(
      AchievementId.SHORT_HOP,
      R.string.achievement_short_hop,
      R.string.achievement_short_hop_condition,
      R.drawable.ic_short_hop),
  JOURNEYMAN_DATA(
      AchievementId.JOURNEYMAN,
      R.string.achievement_journeyman,
      R.string.achievement_journeyman_condition,
      R.drawable.ic_journeyman),
  LONG_HAULER_DATA(
      AchievementId.LONG_HAULER,
      R.string.achievement_long_hauler,
      R.string.achievement_long_hauler_condition,
      R.drawable.ic_long_hauler),
  IRON_ROUTE_CHAMPION_DATA(
      AchievementId.IRON_ROUTE_CHAMPION,
      R.string.achievement_iron_route_champion,
      R.string.achievement_iron_route_champion_condition,
      R.drawable.ic_iron_route_champion),

  // 6) Social / Friends
  NEW_FRIEND_DATA(
      AchievementId.NEW_FRIEND,
      R.string.achievement_new_friend,
      R.string.achievement_new_friend_condition,
      R.drawable.ic_new_friend),
  SOCIAL_TRAVELER_DATA(
      AchievementId.SOCIAL_TRAVELER,
      R.string.achievement_social_traveler,
      R.string.achievement_social_traveler_condition,
      R.drawable.ic_social_traveler),
  POPULAR_GUIDE_DATA(
      AchievementId.POPULAR_GUIDE,
      R.string.achievement_popular_guide,
      R.string.achievement_popular_guide_condition,
      R.drawable.ic_popular_guide),
  LEGENDARY_CONNECTOR_DATA(
      AchievementId.LEGENDARY_CONNECTOR,
      R.string.achievement_legendary_connector,
      R.string.achievement_legendary_connector_condition,
      R.drawable.ic_legendary_connector),
}

/** An enum representing the different categories of achievements. */
enum class AchievementCategory {
  TRIPS,
  TIME,
  LOCATIONS,
  TRANSPORT,
  LONGEST_ROUTE,
  SOCIAL
}

/** Filters achievements into categories */
fun AchievementId.category(): AchievementCategory =
    when (this) {
      AchievementId.ROOKIE_EXPLORER,
      AchievementId.WEEKEND_ADVENTURER,
      AchievementId.FREQUENT_TRAVELER,
      AchievementId.GLOBAL_NOMAD -> AchievementCategory.TRIPS
      AchievementId.JUST_WARMING_UP,
      AchievementId.MILEAGE_MAKER,
      AchievementId.ENDURANCE_TRAVELER,
      AchievementId.TIME_LORD -> AchievementCategory.TIME
      AchievementId.LOCAL_TOURIST,
      AchievementId.CITY_HOPPER,
      AchievementId.CONTINENT_CRAWLER,
      AchievementId.GLOBE_TROTTER -> AchievementCategory.LOCATIONS
      AchievementId.TRAIN_ENTHUSIAST,
      AchievementId.ROAD_WARRIOR,
      AchievementId.BUS_BUDDY,
      AchievementId.ECO_RIDER -> AchievementCategory.TRANSPORT
      AchievementId.SHORT_HOP,
      AchievementId.JOURNEYMAN,
      AchievementId.LONG_HAULER,
      AchievementId.IRON_ROUTE_CHAMPION -> AchievementCategory.LONGEST_ROUTE
      AchievementId.NEW_FRIEND,
      AchievementId.SOCIAL_TRAVELER,
      AchievementId.POPULAR_GUIDE,
      AchievementId.LEGENDARY_CONNECTOR -> AchievementCategory.SOCIAL
    }

/** thresholds in same units you used in computeAchievements */
fun AchievementCategory.tiers(): List<AchievementId> =
    when (this) {
      AchievementCategory.TRIPS ->
          listOf(
              AchievementId.ROOKIE_EXPLORER,
              AchievementId.WEEKEND_ADVENTURER,
              AchievementId.FREQUENT_TRAVELER,
              AchievementId.GLOBAL_NOMAD,
          )
      AchievementCategory.TIME ->
          listOf(
              AchievementId.JUST_WARMING_UP,
              AchievementId.MILEAGE_MAKER,
              AchievementId.ENDURANCE_TRAVELER,
              AchievementId.TIME_LORD,
          )
      AchievementCategory.LOCATIONS ->
          listOf(
              AchievementId.LOCAL_TOURIST,
              AchievementId.CITY_HOPPER,
              AchievementId.CONTINENT_CRAWLER,
              AchievementId.GLOBE_TROTTER,
          )
      AchievementCategory.TRANSPORT ->
          listOf(
              AchievementId.TRAIN_ENTHUSIAST,
              AchievementId.ROAD_WARRIOR,
              AchievementId.BUS_BUDDY,
              AchievementId.ECO_RIDER,
          )
      AchievementCategory.LONGEST_ROUTE ->
          listOf(
              AchievementId.SHORT_HOP,
              AchievementId.JOURNEYMAN,
              AchievementId.LONG_HAULER,
              AchievementId.IRON_ROUTE_CHAMPION,
          )
      AchievementCategory.SOCIAL ->
          listOf(
              AchievementId.NEW_FRIEND,
              AchievementId.SOCIAL_TRAVELER,
              AchievementId.POPULAR_GUIDE,
              AchievementId.LEGENDARY_CONNECTOR,
          )
    }

/** Helper: find the data for a given id. */
fun AchievementId.toData(): AchievementData = AchievementData.values().first { it.id == this }

/** Convert an achievement category to a string resource. */
fun AchievementCategory.displayStringRes(): Int =
    when (this) {
      AchievementCategory.TRIPS -> R.string.achievement_category_trips
      AchievementCategory.TIME -> R.string.achievement_category_time
      AchievementCategory.LOCATIONS -> R.string.achievement_category_locations
      AchievementCategory.TRANSPORT -> R.string.achievement_category_transport
      AchievementCategory.LONGEST_ROUTE -> R.string.achievement_category_longest_route
      AchievementCategory.SOCIAL -> R.string.achievement_category_social
    }

/**
 * Compute achievements for a given user.
 *
 * @param stats The user's stats
 * @param friendsCount The number of friends the user has
 * @return A list of achievements for the user
 */
fun computeAchievements(
    stats: UserStats,
    friendsCount: Int,
): List<Achievement> {
  val result = mutableListOf<Achievement>()

  /** Adds a given achievement to the user along with its data */
  fun add(id: AchievementId) {
    val data = id.toData()
    result +=
        Achievement(
            id = data.id,
            label = data.label,
            icon = data.icon,
        )
  }

  /** Adds the highest medal tier only */
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
