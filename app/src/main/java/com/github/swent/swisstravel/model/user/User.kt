package com.github.swent.swisstravel.model.user

import java.util.Date

/** User data model representing a user in the application.
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

/** Enum representing various user preferences.
 *
 * Add more preferences as needed.
 */
enum class UserPreference {
    HANDICAP,
    HIKING,
    MUSEUMS,
    SKIING,
    FOODIE
    // Add more preferences as needed
}

/** Extension function to get a display string for a UserPreference.
 *
 * @return A UI-friendly string representation of the preference.
 */
fun UserPreference.displayString(): String {
    return when (this) {
        UserPreference.HANDICAP -> "Handicap Accessible"
        UserPreference.HIKING -> "Hiking & Outdoor"
        UserPreference.MUSEUMS -> "Museums"
        UserPreference.SKIING -> "Skiing & Snow Sports"
        UserPreference.FOODIE -> "Food & Culinary Experiences"
        // Add more cases as needed
    }
}
