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
 */
data class Activity(
    val startDate: Timestamp,
    val endDate: Timestamp,
    val location: Location,
    val description: String,
    val imageUrls: List<String> = emptyList(),
    val estimatedTime: Int = 0
) {
  /** Gets the name of the activity */
  fun getName(): String {
    return location.name
  }

  /**
   * Computes the estimated time of an activity
   *
   * @return The duration of the activity in minutes.
   */
  fun estimatedTime(): Int {
    return ((endDate.seconds - startDate.seconds) / 60).toInt()
  }
}
