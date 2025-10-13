package com.github.swent.swisstravel.model.user

/**
 * User data model representing a user in the application.
 *
 * @property uid Unique identifier for the user.
 * @property name Full name of the user.
 * @property email Email address of the user.
 * @property profilePicUrl URL to the user's profile picture.
 * @property preferences List of user preferences.
 */
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val profilePicUrl: String,
    val preferences: List<UserPreference>
)

/**
 * Enum representing various user preferences.
 *
 * Add more preferences as needed.
 */
enum class UserPreference {
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
 * Extension function to get a display string for a UserPreference.
 *
 * @return A UI-friendly string representation of the preference.
 */
fun UserPreference.displayString(): String {
  return when (this) {
    UserPreference.SCENIC_VIEWS -> "Scenic Views"
    UserPreference.SPORTS -> "Sports"
    UserPreference.MUSEUMS -> "Museums"
    UserPreference.HIKE -> "Hiking"
    UserPreference.CHILDREN_FRIENDLY -> "Children Friendly"
    UserPreference.NIGHTLIFE -> "Nightlife & Party"
    UserPreference.SHOPPING -> "Shopping"
    UserPreference.WELLNESS -> "Wellness"
    UserPreference.FOODIE -> "Food & Culinary Experiences"
    UserPreference.URBAN -> "Urban Sightseeing"
    UserPreference.GROUP -> "Group Friendly"
    UserPreference.INDIVIDUAL -> "Solo Traveler Friendly"
    UserPreference.COUPLE -> "Couple Friendly"
    UserPreference.WHEELCHAIR_ACCESSIBLE -> "Wheelchair Accessible"
    UserPreference.PUBLIC_TRANSPORT -> "Public Transport Accessibility"
  }
}

fun UserPreference.toSwissTourismFacet(): String {
  return when (this) {
    UserPreference.SCENIC_VIEWS -> "views"
    UserPreference.SPORTS -> "sporttype"
    UserPreference.MUSEUMS -> "museumtype"
    UserPreference.HIKE -> "sporttype"
    UserPreference.CHILDREN_FRIENDLY -> "suitablefortype"
    UserPreference.NIGHTLIFE -> "outgoingtype"
    UserPreference.SHOPPING -> "shoppingtype"
    UserPreference.WELLNESS -> "wellnesstype"
    UserPreference.FOODIE -> "experiencetype"
    UserPreference.URBAN -> "experiencetype"
    UserPreference.GROUP -> "suitablefortype"
    UserPreference.INDIVIDUAL -> "suitablefortype"
    UserPreference.COUPLE -> "suitablefortype"
    UserPreference.WHEELCHAIR_ACCESSIBLE -> "wheelchairaccessibleclassifications"
    UserPreference.PUBLIC_TRANSPORT -> "reachabilitylocation"
  }
}

fun UserPreference.toSwissTourismFacetFilter(): String {
  return when (this) {
    UserPreference.SCENIC_VIEWS -> "%2A"
    UserPreference.SPORTS -> "%2A"
    UserPreference.MUSEUMS -> "%2A"
    UserPreference.HIKE -> "hike"
    UserPreference.CHILDREN_FRIENDLY -> "family"
    UserPreference.NIGHTLIFE -> "%2A"
    UserPreference.SHOPPING -> "%2A"
    UserPreference.WELLNESS -> "%2A"
    UserPreference.FOODIE -> "culinary"
    UserPreference.URBAN -> "urban"
    UserPreference.GROUP -> "group"
    UserPreference.INDIVIDUAL -> "individual"
    UserPreference.COUPLE -> "couples"
    UserPreference.WHEELCHAIR_ACCESSIBLE -> "%2A"
    UserPreference.PUBLIC_TRANSPORT -> "closetopublictransport"
  }
}
