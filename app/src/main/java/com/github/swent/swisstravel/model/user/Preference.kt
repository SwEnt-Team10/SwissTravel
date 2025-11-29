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
  SLOW_PACE,
  WHEELCHAIR_ACCESSIBLE,
  EARLY_BIRD,
  NIGHT_OWL,
  INTERMEDIATE_STOPS
}

/** Object containing utility functions and mappings related to preference categories. */
object PreferenceCategories {
  /** Enum representing the different categories of preferences. */
  enum class Category {
    ACTIVITY_TYPE,
    TRAVEL_COMPANION,
    ENVIRONMENT,
    TRAVEL_STYLE,
    ACCESSIBILITY,
    DEFAULT // Should never have preferences in the default category
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
        Category.TRAVEL_STYLE -> R.string.preference_category_travel_style
        Category.ACCESSIBILITY -> R.string.preference_category_accessibility
        else -> R.string.preference_category_default
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
        Preference.QUICK -> Category.TRAVEL_STYLE
        Preference.EARLY_BIRD -> Category.TRAVEL_STYLE
        Preference.NIGHT_OWL -> Category.TRAVEL_STYLE
        Preference.SLOW_PACE -> Category.TRAVEL_STYLE
        Preference.INTERMEDIATE_STOPS -> Category.TRAVEL_STYLE
        Preference.PUBLIC_TRANSPORT -> Category.ACCESSIBILITY
        Preference.WHEELCHAIR_ACCESSIBLE -> Category.ACCESSIBILITY
      }

  /** Lists of preferences grouped by their categories. */
  val activityTypePreferences =
      Preference.values().filter { it.category() == Category.ACTIVITY_TYPE }
  val travelCompanionPreferences =
      Preference.values().filter { it.category() == Category.TRAVEL_COMPANION }
  val environmentPreferences = Preference.values().filter { it.category() == Category.ENVIRONMENT }
  val travelStylePreferences = Preference.values().filter { it.category() == Category.TRAVEL_STYLE }
  val accessibilityPreferences =
      Preference.values().filter { it.category() == Category.ACCESSIBILITY }
  // Should always be empty
  val defaultPreferences = Preference.values().filter { it.category() == Category.DEFAULT }

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
      Category.TRAVEL_STYLE -> travelStylePreferences
      Category.ACCESSIBILITY -> accessibilityPreferences
      else -> defaultPreferences
    }
  }

  /** Extension function to convert a Category to a test tag string. */
  fun Category.toTestTagString(): String {
    return when (this) {
      Category.ACTIVITY_TYPE -> "activityType"
      Category.TRAVEL_COMPANION -> "travelCompanion"
      Category.ENVIRONMENT -> "environment"
      Category.TRAVEL_STYLE -> "travelStyle"
      Category.ACCESSIBILITY -> "accessibility"
      else -> "default"
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
    Preference.SLOW_PACE -> R.string.preference_slow_pace
    Preference.EARLY_BIRD -> R.string.preference_early_bird
    Preference.NIGHT_OWL -> R.string.preference_night_owl
    Preference.INTERMEDIATE_STOPS -> R.string.preference_intermediate_stops
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
    Preference.SLOW_PACE -> "slowPace"
    Preference.EARLY_BIRD -> "earlyBird"
    Preference.NIGHT_OWL -> "nightOwl"
    Preference.INTERMEDIATE_STOPS -> "intermediateStops"
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
    Preference.SLOW_PACE -> ""
    Preference.EARLY_BIRD -> ""
    Preference.NIGHT_OWL -> ""
    Preference.INTERMEDIATE_STOPS -> ""
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
    Preference.SLOW_PACE -> ""
    Preference.EARLY_BIRD -> ""
    Preference.NIGHT_OWL -> ""
    Preference.INTERMEDIATE_STOPS -> ""
  }
}

object PreferenceRules {

  /** Preferences that cannot coexist (mutually exclusive groups). */
  val MUTUALLY_EXCLUSIVE_GROUPS: List<Set<Preference>> =
      listOf(
          setOf(Preference.QUICK, Preference.SLOW_PACE),
          setOf(Preference.NIGHT_OWL, Preference.EARLY_BIRD))

  /** Return all prefs that conflict with [pref]. */
  fun conflictsFor(pref: Preference): Set<Preference> =
      MUTUALLY_EXCLUSIVE_GROUPS.firstOrNull { pref in it }?.minus(pref) ?: emptySet()

  /** True if a and b cannot coexist. */
  fun isMutuallyExclusive(a: Preference, b: Preference): Boolean =
      MUTUALLY_EXCLUSIVE_GROUPS.any { a in it && b in it && a != b }

  /** Keep at most one per exclusive group. Later items win (stable add order respected). */
  fun enforceMutualExclusivity(prefs: Collection<Preference>): List<Preference> {
    val out = LinkedHashSet<Preference>()
    for (p in prefs) {
      out.removeAll(conflictsFor(p))
      out.add(p)
    }
    return out.toList()
  }

  /** Toggle a single pref while enforcing exclusivity (remove conflicting ones first). */
  fun toggleWithExclusivity(current: Collection<Preference>, pref: Preference): List<Preference> {
    val set = LinkedHashSet(current)
    if (set.contains(pref)) {
      set.remove(pref)
    } else {
      set.removeAll(conflictsFor(pref))
      set.add(pref)
    }
    return set.toList()
  }
}
