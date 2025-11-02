package com.github.swent.swisstravel.model.user

import androidx.annotation.StringRes
import com.github.swent.swisstravel.R

/** Enum representing the different preferences a user can select. */
enum class Preference {
  FOODIE,
  SPORTS,
  MUSEUMS,
  WELLNESS,
  HIKE,
  SHOPPING,
  INDIVIDUAL,
  GROUP,
  CHILDREN_FRIENDLY,
  COUPLE,
  URBAN,
  NIGHTLIFE,
  SCENIC_VIEWS,
  PUBLIC_TRANSPORT,
  QUICK,
  WHEELCHAIR_ACCESSIBLE
}

/** Object containing utility functions and mappings related to preference categories. */
object PreferenceCategories {
  /** Enum representing the different categories of preferences. */
  enum class Category {
    ACTIVITY_TYPE,
    TRAVEL_COMPANION,
    ENVIRONMENT,
    ACCESSIBILITY
  }

  /**
   * Maps a Category to its corresponding string resource ID.
   *
   * @param category The category to be converted.
   * @return The string resource ID corresponding to the category.
   */
  @StringRes
  fun Category.categoryToStringRes(): Int =
      when (this) {
        Category.ACTIVITY_TYPE -> R.string.preference_category_activity_type
        Category.TRAVEL_COMPANION -> R.string.preference_category_travel_companion
        Category.ENVIRONMENT -> R.string.preference_category_environment
        Category.ACCESSIBILITY -> R.string.preference_category_accessibility
      }

  /** Map linking each Preference to its corresponding Category. */
  fun Preference.category(): Category =
      when (this) {
        Preference.MUSEUMS -> Category.ACTIVITY_TYPE
        Preference.WELLNESS -> Category.ACTIVITY_TYPE
        Preference.HIKE -> Category.ACTIVITY_TYPE
        Preference.FOODIE -> Category.ACTIVITY_TYPE
        Preference.SPORTS -> Category.ACTIVITY_TYPE
        Preference.SHOPPING -> Category.ACTIVITY_TYPE
        Preference.INDIVIDUAL -> Category.TRAVEL_COMPANION
        Preference.GROUP -> Category.TRAVEL_COMPANION
        Preference.CHILDREN_FRIENDLY -> Category.TRAVEL_COMPANION
        Preference.COUPLE -> Category.TRAVEL_COMPANION
        Preference.URBAN -> Category.ENVIRONMENT
        Preference.NIGHTLIFE -> Category.ENVIRONMENT
        Preference.SCENIC_VIEWS -> Category.ENVIRONMENT
        Preference.PUBLIC_TRANSPORT -> Category.ACCESSIBILITY
        Preference.QUICK -> Category.ACCESSIBILITY
        Preference.WHEELCHAIR_ACCESSIBLE -> Category.ACCESSIBILITY
      }

  /** Lists of preferences grouped by their categories. */
  val activityTypePreferences =
      Preference.values().filter { it.category() == Category.ACTIVITY_TYPE }
  val travelCompanionPreferences =
      Preference.values().filter { it.category() == Category.TRAVEL_COMPANION }
  val environmentPreferences = Preference.values().filter { it.category() == Category.ENVIRONMENT }
  val accessibilityPreferences =
      Preference.values().filter { it.category() == Category.ACCESSIBILITY }

  /**
   * Extension function to get the list of preferences for a given Category.
   *
   * @return A list of preferences belonging to the category.
   */
  fun Category.getPreferences(): List<Preference> {
    return when (this) {
      Category.ACTIVITY_TYPE -> activityTypePreferences
      Category.TRAVEL_COMPANION -> travelCompanionPreferences
      Category.ENVIRONMENT -> environmentPreferences
      Category.ACCESSIBILITY -> accessibilityPreferences
    }
  }

  /** Extension function to convert a Category to a test tag string. */
  fun Category.toTestTagString(): String {
    return when (this) {
      Category.ACTIVITY_TYPE -> "activityType"
      Category.TRAVEL_COMPANION -> "travelCompanion"
      Category.ENVIRONMENT -> "environment"
      Category.ACCESSIBILITY -> "accessibility"
    }
  }
}

/**
 * Extension function to get a display string for a Preference.
 *
 * @return A string representation of the preference.
 */
@StringRes
fun Preference.displayStringRes(): Int {
  return when (this) {
    Preference.SCENIC_VIEWS -> R.string.preference_scenic_views
    Preference.SPORTS -> R.string.preference_sports
    Preference.MUSEUMS -> R.string.preference_museums
    Preference.HIKE -> R.string.preference_hike
    Preference.CHILDREN_FRIENDLY -> R.string.preference_children_friendly
    Preference.NIGHTLIFE -> R.string.preference_nightlife
    Preference.SHOPPING -> R.string.preference_shopping
    Preference.WELLNESS -> R.string.preference_wellness
    Preference.FOODIE -> R.string.preference_foodie
    Preference.URBAN -> R.string.preference_urban
    Preference.GROUP -> R.string.preference_group_friendly
    Preference.INDIVIDUAL -> R.string.preference_solo_friendly
    Preference.COUPLE -> R.string.preference_couple_friendly
    Preference.WHEELCHAIR_ACCESSIBLE -> R.string.preference_wheelchair_accessible
    Preference.PUBLIC_TRANSPORT -> R.string.preference_public_transport
    Preference.QUICK -> R.string.preference_quick
  }
}

/**
 * Extension function to get the string representation of a Preference as stored in the firebase.
 *
 * @return A string representation of the preference.
 */
fun Preference.displayString(): String {
  return when (this) {
    Preference.SCENIC_VIEWS -> "Scenic Views"
    Preference.SPORTS -> "Sports"
    Preference.MUSEUMS -> "Museums"
    Preference.HIKE -> "Hiking"
    Preference.CHILDREN_FRIENDLY -> "Children Friendly"
    Preference.NIGHTLIFE -> "Nightlife & Party"
    Preference.SHOPPING -> "Shopping"
    Preference.WELLNESS -> "Wellness"
    Preference.FOODIE -> "Food & Culinary Experiences"
    Preference.URBAN -> "Urban Sightseeing"
    Preference.GROUP -> "Group Friendly"
    Preference.INDIVIDUAL -> "Solo Traveler Friendly"
    Preference.COUPLE -> "Couple Friendly"
    Preference.WHEELCHAIR_ACCESSIBLE -> "Wheelchair Accessible"
    Preference.PUBLIC_TRANSPORT -> "Public Transport Accessibility"
    Preference.QUICK -> "Fast Trip"
  }
}

/**
 * Extension function to convert a Preference to a test tag string.
 *
 * @return A string representing the test tag for the given preference.
 */
fun Preference.toTestTagString(): String {
  return when (this) {
    Preference.SCENIC_VIEWS -> "scenicViews"
    Preference.SPORTS -> "sports"
    Preference.MUSEUMS -> "museums"
    Preference.HIKE -> "hike"
    Preference.CHILDREN_FRIENDLY -> "childrenFriendly"
    Preference.NIGHTLIFE -> "nightlife"
    Preference.SHOPPING -> "shopping"
    Preference.WELLNESS -> "wellness"
    Preference.FOODIE -> "foodie"
    Preference.URBAN -> "urban"
    Preference.GROUP -> "group"
    Preference.INDIVIDUAL -> "individual"
    Preference.COUPLE -> "couple"
    Preference.WHEELCHAIR_ACCESSIBLE -> "wheelchairAccessible"
    Preference.PUBLIC_TRANSPORT -> "publicTransport"
    Preference.QUICK -> "quick"
  }
}

/**
 * Extension function to convert a Preference to a Swiss Tourism facet.
 *
 * @return A string representing the facet for the given preference.
 */
fun Preference.toSwissTourismFacet(): String {
  return when (this) {
    Preference.SCENIC_VIEWS -> "views"
    Preference.SPORTS -> "sporttype"
    Preference.MUSEUMS -> "museumtype"
    Preference.HIKE -> "sporttype"
    Preference.CHILDREN_FRIENDLY -> "suitablefortype"
    Preference.NIGHTLIFE -> "outgoingtype"
    Preference.SHOPPING -> "shoppingtype"
    Preference.WELLNESS -> "wellnesstype"
    Preference.FOODIE -> "experiencetype"
    Preference.URBAN -> "experiencetype"
    Preference.GROUP -> "suitablefortype"
    Preference.INDIVIDUAL -> "suitablefortype"
    Preference.COUPLE -> "suitablefortype"
    Preference.WHEELCHAIR_ACCESSIBLE -> "wheelchairaccessibleclassifications"
    Preference.PUBLIC_TRANSPORT -> "reachabilitylocation"
    Preference.QUICK -> ""
  }
}

/**
 * Extension function to convert a Preference to a Swiss Tourism facet filter.
 *
 * @return A string representing the facet filter for the given preference.
 */
fun Preference.toSwissTourismFacetFilter(): String {
  return when (this) {
    Preference.SCENIC_VIEWS -> "%2A"
    Preference.SPORTS -> "%2A"
    Preference.MUSEUMS -> "%2A"
    Preference.HIKE -> "hike"
    Preference.CHILDREN_FRIENDLY -> "family"
    Preference.NIGHTLIFE -> "%2A"
    Preference.SHOPPING -> "%2A"
    Preference.WELLNESS -> "%2A"
    Preference.FOODIE -> "culinary"
    Preference.URBAN -> "urban"
    Preference.GROUP -> "group"
    Preference.INDIVIDUAL -> "individual"
    Preference.COUPLE -> "couples"
    Preference.WHEELCHAIR_ACCESSIBLE -> "%2A"
    Preference.PUBLIC_TRANSPORT -> "closetopublictransport"
    Preference.QUICK -> ""
  }
}
