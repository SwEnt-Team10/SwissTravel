package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocationsv2.DurationMatrixHybrid
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
import com.github.swent.swisstravel.algorithm.tripschedule.scheduleTrip
import com.github.swent.swisstravel.model.trip.Coordinate
import com.github.swent.swisstravel.model.trip.Location
import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.model.trip.TripElement
import com.github.swent.swisstravel.model.trip.TripProfile
import com.github.swent.swisstravel.model.trip.activity.Activity
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.collections.zipWithNext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val DISTANCE_PER_STOP_KM = 90.0
const val RADIUS_NEW_ACTIVITY_M = 15000
const val RADIUS_EXTENSION_M = 12500
const val INVALID_DURATION = -1.0

/**
 * Data class representing the progression weights for each step of the trip computation.
 *
 * @param selectActivities Weight for the activity selection step.
 * @param optimizeRoute Weight for the route optimization step.
 * @param fetchInBetweenActivities Weight for fetching in-between activities step.
 * @param scheduleTrip Weight for the trip scheduling step.
 * @param finalScheduling Weight for the final scheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class Progression(
    val selectActivities: Float,
    val optimizeRoute: Float,
    val fetchInBetweenActivities: Float,
    val scheduleTrip: Float,
    val finalScheduling: Float
) {
  init {
    val sum =
        selectActivities + optimizeRoute + scheduleTrip + fetchInBetweenActivities + finalScheduling
    require(sum == 1.0f) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * Main class to compute a trip based on data the user passed through the trip creation process. It
 * integrates activity selection, route optimization, and trip scheduling.
 *
 * @property activitySelector The component responsible for selecting activities based on user
 *   preferences.
 * @property routeOptimizer The component responsible for optimizing the route between locations.
 * @property scheduleParams Parameters for scheduling the trip.
 * @property progression Weights for each step of the trip computation process.
 */
class TripAlgorithm(
    private val activitySelector: SelectActivities,
    private val routeOptimizer: ProgressiveRouteOptimizer,
    private val scheduleParams: ScheduleParams = ScheduleParams(),
    private val progression: Progression =
        Progression(
            selectActivities = 0.20f,
            optimizeRoute = 0.40f,
            fetchInBetweenActivities = 0.10f,
            scheduleTrip = 0.20f,
            finalScheduling = 0.10f)
) {
  companion object {
    /**
     * Initializes the TripAlgorithm with the necessary components.
     *
     * @param tripSettings The settings for the trip.
     * @param activityRepository The repository to fetch activities from.
     * @param context The Android context.
     * @return An instance of TripAlgorithm.
     */
    fun init(
        tripSettings: TripSettings,
        activityRepository: ActivityRepository,
        context: Context
    ): TripAlgorithm {

      val activitySelector =
          SelectActivities(tripSettings = tripSettings, activityRepository = activityRepository)

      val cacheManager = DurationCacheLocal(context)
      val durationMatrix = DurationMatrixHybrid(context)
      val penalty = ProgressiveRouteOptimizer.PenaltyConfig()
      val optimizer =
          ProgressiveRouteOptimizer(
              cacheManager = cacheManager, matrixHybrid = durationMatrix, penaltyConfig = penalty)

      return TripAlgorithm(activitySelector, optimizer)
    }
  }

  /**
   * Computes a trip based on the provided settings and profile.
   *
   * @param tripSettings The settings for the trip.
   * @param tripProfile The profile of the trip.
   * @param onProgress A callback function to report the progress of the computation (from 0.0 to
   *   1.0).
   * @return A list of [TripElement] representing the computed trip.
   */
  suspend fun computeTrip(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    try {
      val startLocation =
          tripSettings.arrivalDeparture.arrivalLocation
              ?: throw IllegalArgumentException("Arrival location must not be null")
      val endLocation =
          tripSettings.arrivalDeparture.departureLocation
              ?: throw IllegalArgumentException("Departure location must not be null")

      // ---- STEP 1: Select activities ----
      onProgress(0.0f)
      val selectedActivities =
          try {
            activitySelector.addActivities { progress ->
              onProgress(progression.selectActivities * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Failed to select activities: ${e.message}", e)
          }
      onProgress(progression.selectActivities)
      val activityList = selectedActivities.toMutableList()
      val fullDestinationList = buildList {
        addAll(tripSettings.destinations)
        addAll(selectedActivities.map { it.location })
      }

      Log.d("TripAlgorithm", "Full destination list: $fullDestinationList")

      // ---- STEP 2: Optimize route ----
      var optimizedRoute =
          try {
            routeOptimizer.optimize(
                start = startLocation,
                end = endLocation,
                allLocations = fullDestinationList,
                activities = selectedActivities,
                mode =
                    if (tripSettings.preferences.contains(Preference.PUBLIC_TRANSPORT)) {
                      TransportMode.TRAIN
                    } else TransportMode.CAR) { progress ->
                  onProgress(progression.selectActivities + progression.optimizeRoute * progress)
                }
          } catch (e: Exception) {
            throw IllegalStateException("Route optimization failed: ${e.message}", e)
          }

      check(optimizedRoute.totalDuration > 0) { "Optimized route duration is zero or negative" }

      // ---- STEP 2b: Insert in-between activities if preference enabled ----
      if (tripSettings.preferences.contains(Preference.INTERMEDIATE_STOPS)) {
        optimizedRoute =
            addInBetweenActivities(optimizedRoute = optimizedRoute, activityList) { progress ->
              onProgress(
                  progression.selectActivities +
                      progression.optimizeRoute +
                      progression.fetchInBetweenActivities * progress)
            }
      }

      onProgress(
          progression.selectActivities +
              progression.optimizeRoute +
              progression.fetchInBetweenActivities)

      // ---- STEP 3: Schedule trip ----
      val schedule =
          try {
            scheduleTrip(tripProfile, optimizedRoute, activityList, scheduleParams) { progress ->
              onProgress(
                  progression.selectActivities +
                      progression.optimizeRoute +
                      progression.fetchInBetweenActivities +
                      progression.scheduleTrip * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Trip scheduling failed: ${e.message}", e)
          }

      check(schedule.isNotEmpty()) { "Scheduled trip is empty" }

      // ---- STEP 4: Recomplete schedule if needed ----
      val finalSchedule =
          adjustFinalSchedule(
              originalSchedule = schedule,
              originalOrderedRoute = optimizedRoute,
              tripProfile = tripProfile,
              // isRandom = isRandom // TODO: Uncomment this once the other PR has been merged
          ) { progress ->
            onProgress(
                progression.selectActivities +
                    progression.optimizeRoute +
                    progression.fetchInBetweenActivities +
                    progression.scheduleTrip +
                    progression.finalScheduling * progress)
          }

      onProgress(1.0f)
      return finalSchedule
    } catch (e: Exception) {
      Log.e("TripAlgorithm", "Trip computation failed", e)
      throw e
    }
  }

  /**
   * Generates a list of "in-between" activities along the segment from start to end.
   *
   * @param start Starting Location.
   * @param end Ending Location.
   * @param count Number of activities to generate.
   * @return List of new Activities between start and end.
   */
  suspend fun generateActivitiesBetween(
      start: Location,
      end: Location,
      count: Int
  ): List<Activity> {
    if (count <= 0) return emptyList()

    val newActivities = mutableListOf<Activity>()
    val latStep = (end.coordinate.latitude - start.coordinate.latitude) / (count + 1)
    val lonStep = (end.coordinate.longitude - start.coordinate.longitude) / (count + 1)

    for (i in 1..count) {
      // Base coordinates for this stop
      val baseLat = start.coordinate.latitude + latStep * i
      val baseLon = start.coordinate.longitude + lonStep * i

      // Add a small random offset to avoid perfect line
      val randomOffsetLat = (-0.02..0.02).random()
      val randomOffsetLon = (-0.02..0.02).random()

      val coord =
          Coordinate(latitude = baseLat + randomOffsetLat, longitude = baseLon + randomOffsetLon)

      val activity =
          activitySelector.getOneActivityNearWithPreferences(
              coords = coord, radius = RADIUS_NEW_ACTIVITY_M)

      if (activity != null) newActivities.add(activity)
    }

    return newActivities
  }

  /**
   * Adds intermediate activities between main locations in the optimized route based on distance.
   *
   * @param optimizedRoute The optimized route containing main locations.
   * @param activities The mutable list of activities to which new activities will be added.
   * @param mode The transport mode for route optimization.
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return A new OrderedRoute including the in-between activities.
   */
  suspend fun addInBetweenActivities(
      optimizedRoute: OrderedRoute,
      activities: MutableList<Activity>,
      mode: TransportMode = TransportMode.CAR,
      onProgress: (Float) -> Unit = {}
  ): OrderedRoute {
    var totalProgress = 0f
    // 1. Get the optimized ordered main locations
    val optimizedMainLocations = optimizedRoute.orderedLocations

    // 2. Build segments along the optimized route
    val segmentPairs = optimizedMainLocations.zipWithNext()

    // 3. Decide how many new stops to add per segment
    val stopsPerSegment =
        segmentPairs.map { (a, b) ->
          val distKm = a.coordinate.haversineDistanceTo(b.coordinate)
          (distKm / DISTANCE_PER_STOP_KM).toInt()
        }

    // 4. Insert intermediate activities along each segment
    // Create a map: startSeg -> List<Activity>
    val intermediateActivitiesBySegment = mutableMapOf<Location, MutableList<Activity>>()

    segmentPairs.forEachIndexed { index, (startSeg, endSeg) ->
      val numStops = stopsPerSegment[index]
      if (numStops <= 0) return@forEachIndexed

      // Generate activities for this segment
      val newActivities = generateActivitiesBetween(startSeg, endSeg, numStops)
      activities.addAll(newActivities)

      // Store in the map under its start segment
      intermediateActivitiesBySegment.getOrPut(startSeg) { mutableListOf() }.addAll(newActivities)

      // Report progress
      for (i in 1..numStops) {
        onProgress(totalProgress)
        totalProgress = i.toFloat() / (numStops.toFloat() * 2)
      }
    }

    // If nothing to add, return early
    if (intermediateActivitiesBySegment.isEmpty()) return optimizedRoute

    // 5. Build new OrderedRoute with the new activities inserted
    val newSegmentDurations = optimizedRoute.segmentDuration.toMutableList()
    val newOrderedLocations = optimizedRoute.orderedLocations.toMutableList()
    // List of indexes where new activities were added to adjust segment durations later
    val addedIndexes = mutableListOf<Int>()
    // Add elements to the lists of our original OrderedRoute at the correct place
    for ((startSeg, activities) in intermediateActivitiesBySegment) {
      // Get the start segment index in the optimized route
      val startIndex = newOrderedLocations.indexOfFirst { it.sameLocation(startSeg) }
      if (startIndex == -1) continue

      // Add the activities location after the start segment
      for (i in 1..activities.size) {
        val activity = activities[i - 1]
        // Because each time we add a new location, the next index shifts by 1
        val insertIndex = startIndex + i
        newOrderedLocations.add(insertIndex, activity.location)
        addedIndexes.add(insertIndex)
        // Set to INVALID_DURATION so that the re-computation knows to calculate it
        newSegmentDurations[insertIndex - 1] = INVALID_DURATION
        newSegmentDurations.add(insertIndex, INVALID_DURATION)
      }
    }

    // New OrderedRoute with the new locations and placeholder durations
    val newOptimizedRoute =
        OrderedRoute(
            orderedLocations = newOrderedLocations,
            totalDuration = optimizedRoute.totalDuration,
            segmentDuration = newSegmentDurations)

    // 6. Recompute the time segments properly with the route optimizer
    val finalOptimizedRoute =
        routeOptimizer.recomputeOrderedRoute(
            newOptimizedRoute, addedIndexes, mode, INVALID_DURATION) {
              onProgress(it + totalProgress)
            }

    return finalOptimizedRoute
  }

  /** Extension function to generate a random double in a closed range */
  private fun ClosedFloatingPointRange<Double>.random(rng: Random = Random.Default): Double {
    // Guarantee closed range by adding the minimal epsilon
    return rng.nextDouble(start, endInclusive + Double.MIN_VALUE)
  }

  /**
   * Adjusts the final schedule to ensure it ends on the correct day.
   *
   * @param originalSchedule The original schedule to be adjusted.
   * @param originalOrderedRoute The original ordered route.
   * @param tripProfile The profile of the trip.
   * @param isRandom Whether the trip is random.
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return The adjusted schedule.
   *
   * Done with AI
   */
  private suspend fun adjustFinalSchedule(
      originalSchedule: List<TripElement>,
      originalOrderedRoute: OrderedRoute,
      tripProfile: TripProfile,
      isRandom: Boolean = false,
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {

    // Already ending on the correct day → nothing to change
    if (sameDate(originalSchedule.last().endDate, tripProfile.endDate)) {
      return originalSchedule
    }

    val newSchedule = originalSchedule.toMutableList()

    // ---- Extract last segment ----
    val last = newSchedule.last()
    val lastSegment =
        last as? TripElement.TripSegment
            ?: throw IllegalStateException("Last TripElement must be a TripSegment")

    val startLoc = lastSegment.route.from
    val endLoc = lastSegment.route.to

    // ---- Compute midpoint ----
    val midLat = (startLoc.coordinate.latitude + endLoc.coordinate.latitude) / 2.0
    val midLon = (startLoc.coordinate.longitude + endLoc.coordinate.longitude) / 2.0
    val midPoint = Location(coordinate = Coordinate(midLat, midLon), name = "Midpoint")

    val listOfLocations = listOf(endLoc, midPoint, startLoc)

    // This will hold the activities that we add during the extension
    val addedActivities = mutableListOf<Activity>()

    var nearWhich = 0
    var radius = RADIUS_NEW_ACTIVITY_M
    var index = 0
    val maxIndex =
        computeIndex(
            targetEndDate = tripProfile.endDate, currentEndDate = originalSchedule.last().endDate)

    // ---- Loop until the final processed schedule reaches correct end date ----
    try {
      var ordered = originalOrderedRoute.copy()
      while (!sameDate(newSchedule.last().endDate, tripProfile.endDate) && index < maxIndex) {

        val targetLoc = listOfLocations[nearWhich].coordinate

        // TODO modify this with a blackList of activities that we already have in the trip
        val newAct = activitySelector.getOneActivityNearWithPreferences(
            targetLoc,
            radius,
            )

        if (newAct != null) {
          // Verify that the new activity is not already in the trip even though it should never
          // happen
          if (addedActivities.any { it.location.sameLocation(newAct.location) } ||
              originalSchedule.any {
                it is TripElement.TripActivity && it.activity.location.sameLocation(newAct.location)
              }) {
            index += 1
            continue
          }
          addedActivities.add(newAct)

          // ---- Build a temporary OrderedRoute ----
          ordered =
              ordered.copy(
                  orderedLocations = ordered.orderedLocations + addedActivities.last().location,
                  segmentDuration = ordered.segmentDuration + listOf(INVALID_DURATION),
                  totalDuration = ordered.totalDuration + INVALID_DURATION)

          val startIndex = ordered.orderedLocations.size - addedActivities.size
          val indexes = (startIndex until ordered.orderedLocations.size).toList()

          // ---- Recompute new ordered route ----
          val recomputed =
              routeOptimizer.recomputeOrderedRoute(
                  orderedLocations = ordered,
                  addedIndexes = indexes,
                  mode = TransportMode.CAR,
                  invalidDuration = INVALID_DURATION) {}

          // ---- Reschedule using this new route ----
          val reScheduled =
              scheduleTrip(
                  tripProfile = tripProfile,
                  ordered = recomputed,
                  activities = addedActivities.toMutableList(),
                  params = scheduleParams) {}

          // Replace newSchedule so we can test endDate again
          newSchedule.clear()
          newSchedule.addAll(reScheduled)
        }

        // If we completed a full cycle without finishing => widen radius
        if (nearWhich == 2) {
          index += 1
          radius += (RADIUS_EXTENSION_M / index)
        }

        nearWhich = (nearWhich + 1) % 3

        if (addedActivities.size % 2 == 0 && addedActivities.size < 10) {
          onProgress(addedActivities.size / 2 * 0.1f)
        }
      }
    } catch (e: Exception) {
      Log.e("TripAlgorithm", "Final schedule extension failed, using original schedule", e)
      onProgress(1.0f)
      return originalSchedule
    }

    // var startingLocation: Location
    val allLocations = mutableListOf<Location>()
    val allActivities = mutableListOf<Activity>()

    // Reoptimize using the last three locations of the original OrderedRoute (if there are 4 or
    // more)
    //        val numberOfElementsToTake = minOf(4, originalOrderedRoute.orderedLocations.size - 1)
    //        val threeBeforeLast =
    // originalOrderedRoute.orderedLocations.dropLast(1).takeLast(numberOfElementsToTake)
    //        allLocations.addAll(threeBeforeLast)
    //        startingLocation = threeBeforeLast.first()

    // Fetch the activities associated with the last three locations
    val oldActivities = originalSchedule.filterIsInstance<TripElement.TripActivity>()
    // var oldActivities = originalSchedule.filterIsInstance<TripElement.TripActivity>()
    // oldActivities = oldActivities.filter { it.activity.location in threeBeforeLast }
    allActivities.addAll(oldActivities.map { it.activity })
    allActivities.addAll(addedActivities)

    allLocations.addAll(allActivities.map { it.location })
    if (isRandom) {
      allLocations.addAll(tripProfile.preferredLocations)
    }
    // Should not be too heavy because most of it should be cached already
    val finalOrderedRoute =
        routeOptimizer.optimize(
            start = startLoc,
            // start = startingLocation,
            end = endLoc,
            allLocations = allLocations,
            activities = allActivities,
            mode =
                if (tripProfile.preferences.contains(Preference.PUBLIC_TRANSPORT)) {
                  TransportMode.TRAIN
                } else TransportMode.CAR,
        ) {
          onProgress(addedActivities.size / 2 * 0.1f + it)
        }

    // Remove the locations that we extracted before as well and remove the end location
    // Remove the segmentDurations associated with them (we keep the one that connects our starting
    // location
    // with the location before it as this one won't change)
    //        val modifiedOrderedRoute =
    //            OrderedRoute(
    //                orderedLocations =
    // originalOrderedRoute.orderedLocations.dropLast(numberOfElementsToTake),
    //                segmentDuration =
    // originalOrderedRoute.segmentDuration.dropLast(numberOfElementsToTake - 1),
    //                totalDuration = originalOrderedRoute.totalDuration)

    // Merge our small orderedRoute to the original one
    // The totalDuration is wrong but it is not needed except in the scheduleTrip in which it is
    // computed
    //        val finalOrderedRoute = OrderedRoute(
    //            orderedLocations = modifiedOrderedRoute.orderedLocations +
    // orderedRoute.orderedLocations,
    //            segmentDuration = modifiedOrderedRoute.segmentDuration +
    // orderedRoute.segmentDuration,
    //            totalDuration = modifiedOrderedRoute.totalDuration + orderedRoute.totalDuration
    //        )

    val finalSchedule =
        scheduleTrip( // TODO: Change this to the new function that will arrive with the other PR
            tripProfile = tripProfile,
            ordered = finalOrderedRoute,
            //            ordered =
            //                finalOrderedRoute.copy(totalDuration =
            // finalOrderedRoute.segmentDuration.sum()),
            activities = allActivities,
            params = scheduleParams) {}

    onProgress(1.0f)
    return finalSchedule
  }

  /**
   * Function to compute an index for the while loop in [adjustFinalSchedule] based on the number of
   * missing days
   *
   * @param targetEndDate The target end date for the trip.
   * @param currentEndDate The current end date of the trip.
   * @param zone The time zone to use for date comparison.
   * @return The computed index.
   */
  private fun computeIndex(
      targetEndDate: Timestamp,
      currentEndDate: Timestamp,
      zone: ZoneId = ZoneId.systemDefault()
  ): Int {
    val currentLocal = currentEndDate.toDate().toInstant().atZone(zone).toLocalDate()
    val targetLocal = targetEndDate.toDate().toInstant().atZone(zone).toLocalDate()

    // Number of calendar days difference (0 if same day, 1 if target is next day, etc.)
    val missingDays = ChronoUnit.DAYS.between(currentLocal, targetLocal).coerceAtLeast(0)

    // min 2 iterations, cap at 5 (your earlier requirement)
    return max(2, min(5, missingDays.toInt()))
  }

  /**
   * Checks if two Timestamps represent the same date.
   *
   * @param date1 The first Timestamp to compare.
   * @param date2 The second Timestamp to compare.
   * @param zone The time zone to use for date comparison.
   * @return True if the Timestamps represent the same date, false otherwise.
   */
  private fun sameDate(
      date1: Timestamp,
      date2: Timestamp,
      zone: ZoneId = ZoneId.systemDefault()
  ): Boolean {
    val d1 = date1.toDate().toInstant().atZone(zone).toLocalDate()
    val d2 = date2.toDate().toInstant().atZone(zone).toLocalDate()

    return d1 == d2
  }
}
