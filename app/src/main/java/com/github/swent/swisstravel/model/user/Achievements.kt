package com.github.swent.swisstravel.model.user

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.model.trip.TransportMode

enum class AchievementCategory(@StringRes val labelRes: Int) {
  TRIPS(R.string.achievement_category_trips),
  TIME(R.string.achievement_category_time),
  LOCATIONS(R.string.achievement_category_locations),
  TRANSPORT(R.string.achievement_category_transport),
  LONGEST_ROUTE(R.string.achievement_category_longest_route),
  SOCIAL(R.string.achievement_category_social),
}

/** Logical ids for achievements. */
enum class AchievementId(val category: AchievementCategory) {
  // 1) Trip Count
  ROOKIE_EXPLORER(AchievementCategory.TRIPS),
  WEEKEND_ADVENTURER(AchievementCategory.TRIPS),
  FREQUENT_TRAVELER(AchievementCategory.TRIPS),
  GLOBAL_NOMAD(AchievementCategory.TRIPS),

  // 2) Travel Time
  JUST_WARMING_UP(AchievementCategory.TIME),
  MILEAGE_MAKER(AchievementCategory.TIME),
  ENDURANCE_TRAVELER(AchievementCategory.TIME),
  TIME_LORD(AchievementCategory.TIME),

  // 3) Unique Locations
  LOCAL_TOURIST(AchievementCategory.LOCATIONS),
  CITY_HOPPER(AchievementCategory.LOCATIONS),
  CONTINENT_CRAWLER(AchievementCategory.LOCATIONS),
  GLOBE_TROTTER(AchievementCategory.LOCATIONS),

  // 4) Transport Mode
  TRAIN_ENTHUSIAST(AchievementCategory.TRANSPORT),
  ROAD_WARRIOR(AchievementCategory.TRANSPORT),
  BUS_BUDDY(AchievementCategory.TRANSPORT),
  ECO_RIDER(AchievementCategory.TRANSPORT),

  // 5) Longest Route Segment
  SHORT_HOP(AchievementCategory.LONGEST_ROUTE),
  JOURNEYMAN(AchievementCategory.LONGEST_ROUTE),
  LONG_HAULER(AchievementCategory.LONGEST_ROUTE),
  IRON_ROUTE_CHAMPION(AchievementCategory.LONGEST_ROUTE),

  // 6) Social / Friends
  NEW_FRIEND(AchievementCategory.SOCIAL),
  SOCIAL_TRAVELER(AchievementCategory.SOCIAL),
  POPULAR_GUIDE(AchievementCategory.SOCIAL),
  LEGENDARY_CONNECTOR(AchievementCategory.SOCIAL),
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
      R.drawable.ic_legendary_connector);

  companion object {
    private val byId: Map<AchievementId, AchievementData> = values().associateBy { it.id }

    fun fromId(id: AchievementId): AchievementData =
        byId[id] ?: error("Unknown achievement id: $id")
  }
}

/** Helper: find the data for a given id. */
fun AchievementId.toData(): AchievementData = AchievementData.fromId(this)

/** Helper: build a concrete Achievement from its id. */
fun AchievementId.toAchievement(): Achievement {
  val data = toData()
  return Achievement(
      id = data.id,
      label = data.label,
      icon = data.icon,
  )
}

/**
 * Thresholds per category, in the same order as [AchievementCategory.tiers] returns ids.
 *
 * Units:
 * - TRIPS: number of trips
 * - TIME: total travel minutes
 * - LOCATIONS: unique locations
 * - LONGEST_ROUTE: longest segment in minutes
 * - SOCIAL: number of friends
 */
private val CATEGORY_THRESHOLDS: Map<AchievementCategory, List<Int>> =
    mapOf(
        AchievementCategory.TRIPS to listOf(1, 5, 10, 20),
        AchievementCategory.TIME to listOf(100, 1_000, 5_000, 20_000),
        AchievementCategory.LOCATIONS to listOf(3, 10, 20, 50),
        AchievementCategory.LONGEST_ROUTE to listOf(15, 60, 180, 600),
        AchievementCategory.SOCIAL to listOf(1, 5, 10, 20),
    )

/** Compute unlocked achievements from user stats + friend count. */
fun computeAchievements(
    stats: UserStats,
    friendsCount: Int,
): List<Achievement> {
  val result = mutableListOf<Achievement>()

  fun MutableList<Achievement>.addHighestTier(
      category: AchievementCategory,
      value: Int,
  ) {
    val thresholds = CATEGORY_THRESHOLDS[category] ?: return
    val tierIds = category.tiers()
    val bestIndex = thresholds.indexOfLast { value >= it }
    if (bestIndex in tierIds.indices) {
      add(tierIds[bestIndex].toAchievement())
    }
  }

  // 1) Trip Count
  result.addHighestTier(AchievementCategory.TRIPS, stats.totalTrips)

  // 2) Travel Time
  result.addHighestTier(AchievementCategory.TIME, stats.totalTravelMinutes)

  // 3) Unique Locations
  result.addHighestTier(AchievementCategory.LOCATIONS, stats.uniqueLocations)

  // 4) Transport Mode (non-tiered)
  when (stats.mostUsedTransportMode) {
    TransportMode.TRAIN -> result += AchievementId.TRAIN_ENTHUSIAST.toAchievement()
    TransportMode.CAR -> result += AchievementId.ROAD_WARRIOR.toAchievement()
    TransportMode.BUS -> result += AchievementId.BUS_BUDDY.toAchievement()
    TransportMode.TRAM -> result += AchievementId.ECO_RIDER.toAchievement()
    else -> Unit
  }

  // 5) Longest Route Segment
  result.addHighestTier(
      AchievementCategory.LONGEST_ROUTE,
      stats.longestRouteSegmentMin,
  )

  // 6) Social / Friends
  result.addHighestTier(AchievementCategory.SOCIAL, friendsCount)

  return result
}

/**
 * The display string for an achievement category.
 *
 * @return The string resource ID for the display string.
 */
fun AchievementCategory.displayStringRes(): Int = labelRes

/** Filters each achievement by category */
fun AchievementCategory.tiers(): List<AchievementId> =
    AchievementId.values().filter { it.category == this }
