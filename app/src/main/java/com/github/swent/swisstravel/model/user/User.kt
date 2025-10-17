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
    val preferences: List<Preference>
)

/**
 * Enum representing various user preferences.
 *
 * Add more preferences as needed.
 */
enum class Preference {
  HANDICAP,
  HIKING,
  MUSEUMS,
  SPORTY,
  FOODIE,
  QUICK
  // Add more preferences as needed
}

/**
 * Extension function to get a display string for a UserPreference.
 *
 * @return A UI-friendly string representation of the preference.
 */
fun Preference.displayString(): String {
  return when (this) {
    Preference.HANDICAP -> "Handicap Accessible"
    Preference.HIKING -> "Hiking & Outdoor"
    Preference.MUSEUMS -> "Museums"
    Preference.SPORTY -> "Sporty"
    Preference.FOODIE -> "Food & Culinary Experiences"
    Preference.QUICK -> "Quick Traveler"
  // Add more cases as needed
  }
}
