package com.github.swent.swisstravel.model.user

/** Enum representing the different preferences a user can select. */
enum class Preference {
  SCENIC_VIEWS,
  SPORTS,
  MUSEUMS,
  HIKE,
  CHILDREN_FRIENDLY,
  NIGHTLIFE,
  SHOPPING,
  WELLNESS,
  FOODIE,
  URBAN,
  GROUP,
  INDIVIDUAL,
  COUPLE,
  WHEELCHAIR_ACCESSIBLE,
  PUBLIC_TRANSPORT
}

/**
 * Extension function to get a display string for a Preference.
 *
 * @return A UI-friendly string representation of the preference.
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
  }
}
