package com.github.swent.swisstravel.model.trip.activity

import com.github.swent.swisstravel.model.trip.Location
import com.google.firebase.Timestamp

/**
 * Represents an activity.
 *
 * @property startDate The start date of the activity.
 * @property endDate The end date of the activity.
 * @property location The location of the activity.
 * @property description The description of the activity.
 * @property imageUrls The image URLs of the activity.
 * @property estimatedTime The estimated time of the activity.
 */
data class Activity(
    val startDate: Timestamp,
    val endDate: Timestamp,
    val location: Location,
    val description: String,
    val imageUrls: List<String>,
    val estimatedTime: Int
) {
  /** Gets the name of the activity */
  fun getName(): String {
    return location.name
  }

  /**
   * Returns the estimated duration of the activity in minutes.
   *
   * @return The duration of the activity in minutes.
   */
  fun estimatedTime(): Int {
    return (estimatedTime / 60)
  }

  /**
   * Checks if an activity is valid.
   *
   * @param blacklistedActivityNames A set of blacklisted activity names.
   * @return True if the activity is valid, false otherwise.
   */
  fun isValid(blacklistedActivityNames: Set<String>): Boolean {
    // Remove activities with blacklisted names
    if (location.name in blacklistedActivityNames) return false

    // Normal validity logic
    if (estimatedTime <= 0) return false

    return true
  }
}
