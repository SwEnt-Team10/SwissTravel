package com.github.swent.swisstravel.algorithm

import android.content.Context
import android.util.Log
import com.github.swent.swisstravel.R
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
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.model.user.PreferenceCategories.category
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.google.firebase.Timestamp
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.collections.zipWithNext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random

// Most of this file was done AI

const val DISTANCE_PER_STOP_KM = 90.0
const val RADIUS_NEW_ACTIVITY_M = 15000
const val RADIUS_EXTENSION_M = 12500
const val INVALID_DURATION = -1.0
const val RESCHEDULE_PENALTY_PER_ACTIVITY_SEC = (0.25 * 3600) // 15 minutes
const val EPSILON = 1e-6f
// This is an arbitrary value to make the loop in addInBetweenActivities not run infinitely
const val MAX_INBETWEEN_ACTIVITIES_SEGMENTS = 4
const val MAX_INBETWEEN_ACTIVITES_BY_SEGMENT = 3
const val RADIUS_CACHED_ACTIVITIES_KM = 20
const val MAX_BATCH_ADDED_TIME_HOURS = 6.0
const val BATCH_ADD_PENALTY_MINUTES = 30

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
    require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * Data class representing the progression weights for each step of the trip computation.
 *
 * @param schedule Weight for the trip scheduling step.
 * @param analyzeAndRemove Weight for analyzing and removing activities step.
 * @param recomputeRoute Weight for recomputing the route step.
 * @param reschedule Weight for the rescheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class RescheduleProgression(
    val schedule: Float,
    val analyzeAndRemove: Float,
    val recomputeRoute: Float,
    val reschedule: Float
) {
  init {
    val sum = schedule + analyzeAndRemove + recomputeRoute + reschedule
    require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * Data class representing the progression weights for the final scheduling step.
 *
 * @param selectNewActivities Weight for selecting new activities step.
 * @param optimize Weight for optimizing the route step.
 * @param finalScheduling Weight for the final scheduling step.
 *
 * The sum of all weights must equal 1.0.
 */
data class FinalSchedulingProgression(
    val selectNewActivities: Float,
    val optimize: Float,
    val finalScheduling: Float
) {
  init {
    val sum = selectNewActivities + optimize + finalScheduling
    require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
  }
}

/**
 * This data class is used to store a tripProfile and a copy of its preferredLocations while also
 * being able to add new locations without modifying the tripProfile meaning This is useful when we
 * are adding grand tour activities
 *
 * @param tripProfile The original tripProfile.
 * @param newPreferredLocations A mutable list of new preferred locations.
 */
data class EnhancedTripProfile(
    val tripProfile: TripProfile,
    val newPreferredLocations: MutableList<Location>
)

/**
 * kdoc done with AI verified by the designer
 *
 * The `TripAlgorithm` is the central engine for generating, optimizing, and scheduling personalized
 * travel itineraries. It orchestrates the complex process of selecting activities, ordering
 * destinations, and fitting them into a valid time schedule.
 *
 * ## Core Responsibilities
 * 1. **Activity Selection:** Fetches relevant activities based on user preferences (e.g., Museums,
 *    Hiking) and travel context (Solo, Family).
 * 2. **Route Optimization:** Solves a variant of the Traveling Salesperson Problem (TSP) to order
 *    locations efficiently, minimizing travel time.
 * 3. **Intermediate Stops:** Dynamically injects stops (e.g., scenic breaks) between long travel
 *    segments if requested.
 * 4. **Scheduling & Rescheduling:** Assigns precise start/end times to activities. If the schedule
 *    overflows (too many activities for the available days), it intelligently prunes less critical
 *    activities and re-optimizes the route.
 * 5. **Gap Filling:** If the generated trip is too short, it iteratively fetches new activities
 *    (from major cities or scenic spots) to fill the remaining days.
 *
 * ## Algorithm Phases & Progression
 * The computation is divided into distinct phases, each contributing to the total progress report
 * (0.0 to 1.0):
 *
 * ### 1. Initialization & Preference Injection
 * - Before starting, the algorithm inspects the `TripSettings`.
 * - **Companion Logic:** Automatically adds preferences like `CHILDREN_FRIENDLY` (if children are
 *   present), `INDIVIDUAL` (solo traveler), or `GROUP`.
 * - **Default Fallback:** If no specific activity types are selected, it injects a default set to
 *   ensure a diverse trip.
 *
 * ### 2. Activity Selection (`SelectActivities`)
 * - **Weight:** `progression.selectActivities`
 * - Fetches candidate activities around the user's chosen destinations.
 * - Respects API rate limits (1s delay between calls) to avoid quotas issues.
 *
 * ### 3. Route Optimization (`ProgressiveRouteOptimizer`)
 * - **Weight:** `progression.optimizeRoute`
 * - Computes a travel duration matrix between all selected locations.
 * - Uses a genetic or heuristic approach to find an optimal visiting order.
 * - Supports different transport modes (`CAR` vs `TRAIN`).
 *
 * ### 4. Intermediate Stops (`addInBetweenActivities`)
 * - **Weight:** `progression.fetchInBetweenActivities`
 * - If `INTERMEDIATE_STOPS` is enabled, breaks long travel segments (> 90km) by injecting new
 *   stops.
 * - Fetches activities at calculated midpoints and re-inserts them into the route.
 *
 * ### 5. Initial Scheduling (`scheduleTrip`)
 * - **Weight:** `progression.scheduleTrip`
 * - Attempts to build a valid timeline.
 * - **Rescheduling Logic:** If activities don't fit (too much activities):
 * 1. Calculates the "time deficit" (how much overtime the trip is).
 * 2. Randomly removes activities to meet the deficit, prioritizing keeping "clusters" (multiple
 *    activities in one spot).
 * 3. Re-optimizes the route with the reduced set of locations.
 *
 * ### 6. Final Gap Filling (`addMoreActivities`)
 * - **Weight:** `progression.finalScheduling`
 * - If the trip ends earlier than the user's end date, this phase loops to add more content.
 * - **Strategies:**
 * - **Cache Reuse:** Tries to use previously fetched but unused activities (`cachedActivities`).
 * - **City Visits:** Adds full-day or half-day visits to major Swiss cities (`swissMajorCities`).
 * - **Grand Tour:** Adds scenic stops along the Grand Tour of Switzerland route.
 * - **Dynamic Preference Expansion:** If stuck, adds new categories (e.g., `ENVIRONMENT` types) to
 *   widen the search.
 * - **Batching:** Can add multiple small activities at once if they fit within a time window
 *   (`MAX_BATCH_ADDED_TIME_HOURS`).
 *
 * @property activitySelector The component responsible for selecting activities based on user
 *   preferences.
 * @property routeOptimizer The component responsible for optimizing the route between locations.
 * @property scheduleParams Parameters for scheduling the trip.
 * @property progression Weights for each step of the trip computation process.
 */
open class TripAlgorithm(
    private val activitySelector: SelectActivities,
    private val routeOptimizer: ProgressiveRouteOptimizer,
    private val context: Context,
    private val scheduleParams: ScheduleParams = ScheduleParams(),
    private val progression: Progression =
        Progression(
            selectActivities = 0.20f,
            optimizeRoute = 0.20f,
            fetchInBetweenActivities = 0.10f,
            scheduleTrip = 0.15f,
            finalScheduling = 0.35f),
    private val rescheduleProgression: RescheduleProgression =
        RescheduleProgression(
            schedule = 0.30f, analyzeAndRemove = 0.10f, recomputeRoute = 0.40f, reschedule = 0.20f),
    private val finalSchedulingProgression: FinalSchedulingProgression =
        FinalSchedulingProgression(
            selectNewActivities = 0.25f, optimize = 0.25f, finalScheduling = 0.50f)
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
      var settingsToUse = tripSettings
      val currentPrefs = settingsToUse.preferences.toMutableList()

      // Apply Travel Companion Logic based on user's input
      val adults = settingsToUse.travelers.adults
      val children = settingsToUse.travelers.children

      // 1. One or more children => Add CHILDREN_FRIENDLY
      if (children >= 1) {
        if (!currentPrefs.contains(Preference.CHILDREN_FRIENDLY)) {
          currentPrefs.add(Preference.CHILDREN_FRIENDLY)
        }
      } else {
        // No children cases:
        // 2. Only one adult => Add INDIVIDUAL
        if (adults == 1) {
          if (!currentPrefs.contains(Preference.INDIVIDUAL)) {
            currentPrefs.add(Preference.INDIVIDUAL)
          }
        }
        // 3. A lot of adults (>= 3) => Add GROUP
        // We use >= 3 to treat 2 adults as a neutral case (could be couple or friends),
        // whereas 3+ is definitely a group.
        else if (adults >= 3) {
          if (!currentPrefs.contains(Preference.GROUP)) {
            currentPrefs.add(Preference.GROUP)
          }
        }
      }

      // Check how many "Activity Type" and "Environment" preferences are selected
      val activityTypeCount =
          currentPrefs.count { it.category() == PreferenceCategories.Category.ACTIVITY_TYPE }
      val environmentCount =
          currentPrefs.count { it.category() == PreferenceCategories.Category.ENVIRONMENT }

      // If no environment => any is good
      // If no activity type => any is good
      if (activityTypeCount == 0 && environmentCount == 0) {
        currentPrefs.addAll(PreferenceCategories.activityTypePreferences)
        currentPrefs.addAll(PreferenceCategories.environmentPreferences)
      } else if (activityTypeCount == 0) {
        currentPrefs.addAll(PreferenceCategories.activityTypePreferences)
      } else if (environmentCount == 0) {
        currentPrefs.addAll(PreferenceCategories.environmentPreferences)
      }

      settingsToUse = settingsToUse.copy(preferences = currentPrefs)

      val activitySelector =
          SelectActivities(tripSettings = settingsToUse, activityRepository = activityRepository)

      val cacheManager = DurationCacheLocal(context)
      val durationMatrix = DurationMatrixHybrid(context)
      val penalty = ProgressiveRouteOptimizer.PenaltyConfig()
      val optimizer =
          ProgressiveRouteOptimizer(
              cacheManager = cacheManager, matrixHybrid = durationMatrix, penaltyConfig = penalty)

      return TripAlgorithm(activitySelector, optimizer, context)
    }
  }

  /**
   * Configuration for a major city.
   *
   * @param location The location of the city.
   * @param radius The radius in km to consider an activity as being in/near the city.
   * @param maxDays The maximum number of days/cityActivities to schedule for this city.
   */
  private data class CityConfig(val location: Location, val radius: Int, val maxDays: Double)

  /**
   * This list was generated by AI A list of pairs representing Swiss major cities and their
   * corresponding coordinates.
   *
   * Each pair contains a [Location] object representing the city and a radius in km from the
   * center.
   */
  private val swissMajorCities: List<CityConfig> =
      listOf(
          CityConfig(Location(Coordinate(47.3769, 8.5417), "Zürich"), 15, 2.0), // Zürich
          CityConfig(Location(Coordinate(46.2044, 6.1432), "Genève"), 12, 2.0), // Geneva
          CityConfig(Location(Coordinate(47.5596, 7.5886), "Basel"), 10, 2.0), // Basel
          CityConfig(Location(Coordinate(46.5197, 6.6323), "Lausanne"), 8, 1.0), // Lausanne
          CityConfig(Location(Coordinate(46.9480, 7.4474), "Bern"), 10, 2.0), // Bern
          CityConfig(Location(Coordinate(47.4988, 8.7241), "Winterthur"), 6, 1.0), // Winterthur
          CityConfig(Location(Coordinate(47.0502, 8.3093), "Luzern"), 7, 2.0), // Lucerne
          CityConfig(Location(Coordinate(47.4239, 9.3744), "St. Gallen"), 6, 1.0), // St. Gallen
          CityConfig(Location(Coordinate(46.0048, 8.9511), "Lugano"), 7, 2.0), // Lugano
          CityConfig(Location(Coordinate(47.1379, 7.2464), "Biel/Bienne"), 5, 0.5), // Biel/Bienne
          CityConfig(Location(Coordinate(46.7578, 7.6206), "Thun"), 5, 0.5), // Thun
          CityConfig(Location(Coordinate(46.1959, 9.0220), "Bellinzona"), 5, 0.5), // Bellinzona
          CityConfig(Location(Coordinate(46.8133, 7.4189), "Köniz"), 5, 0.5), // Köniz (near Bern)
          CityConfig(Location(Coordinate(46.8065, 7.1513), "Fribourg"), 5, 1.0), // Fribourg
          CityConfig(Location(Coordinate(47.6970, 8.6383), "Schaffhausen"), 4, 0.5), // Schaffhausen
          CityConfig(Location(Coordinate(46.8490, 9.5300), "Chur"), 5, 1.0), // Chur
          CityConfig(
              Location(Coordinate(47.3490, 8.7186), "Uster"), 4, 0.5), // Uster (ZH agglomeration)
          CityConfig(Location(Coordinate(46.2335, 7.3573), "Sion"), 5, 0.5), // Sion
          CityConfig(Location(Coordinate(46.4300, 6.9100), "Vevey"), 3, 0.5), // Vevey
          CityConfig(Location(Coordinate(46.4310, 6.9110), "Montreux"), 4, 0.5), // Montreux
          CityConfig(Location(Coordinate(46.1697, 8.7971), "Locarno"), 5, 0.5), // Locarno
          CityConfig(
              Location(Coordinate(47.0980, 6.8319), "La Chaux-de-Fonds"),
              4,
              0.5), // La Chaux-de-Fonds
          CityConfig(Location(Coordinate(46.5828, 7.0827), "Gruyères"), 3, 0.5),
          CityConfig(Location(Coordinate(47.6596, 8.8789), "Stein am Rhein"), 3, 0.5),
          CityConfig(Location(Coordinate(46.9189, 7.1189), "Murten"), 3, 0.5),
          CityConfig(Location(Coordinate(46.6861, 7.6694), "Spiez"), 4, 0.5),
          CityConfig(Location(Coordinate(46.6833, 7.8500), "Interlaken"), 4, 0.5),
          CityConfig(Location(Coordinate(46.5944, 7.9014), "Lauterbrunnen"), 3, 0.5),
          CityConfig(Location(Coordinate(47.3308, 9.4005), "Appenzell"), 3, 0.5),
          CityConfig(Location(Coordinate(45.9328, 8.9328), "Morcote"), 3, 0.5))

  /**
   * A data class used to store the result of [addGrandTourActivities]
   *
   * @param added Whether at least one activity was successfully added.
   * @param shouldRetryCenter Whether the center should be retried next time.
   */
  private data class AddGrandTourResult(val added: Boolean, val shouldRetryCenter: Boolean)

  /**
   * Computes a trip based on the provided settings and profile.
   *
   * @param tripSettings The settings for the trip.
   * @param tripProfile The profile of the trip.
   * @param isRandomTrip Whether the trip is random or not.
   * @param cachedActivities A list of all the activities that were pulled but we didn't use
   * @param onProgress A callback function to report the progress of the computation (from 0.0 to
   *   1.0).
   * @return A list of [TripElement] representing the computed trip.
   */
  suspend fun computeTrip(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
      isRandomTrip: Boolean = false,
      cachedActivities: MutableList<Activity> = mutableListOf(),
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    val enhancedTripProfile =
        EnhancedTripProfile(tripProfile, tripProfile.preferredLocations.toMutableList())

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
            activitySelector.addActivities(cachedActivities) { progress ->
              onProgress(progression.selectActivities * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Failed to select activities: ${e.message}", e)
          }
      onProgress(progression.selectActivities)
      val activityList = selectedActivities.toMutableList()
      val originalActivityList = selectedActivities
      val fullDestinationList = buildList {
        // If the trip is random, still go to the grand tour spots
        if (isRandomTrip) {
          addAll(tripSettings.destinations)
        } else {
          add(startLocation)
          add(endLocation)
        }
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
            addInBetweenActivities(
                optimizedRoute = optimizedRoute,
                activities = activityList,
                mode =
                    if (enhancedTripProfile.tripProfile.preferences.contains(
                        Preference.PUBLIC_TRANSPORT))
                        TransportMode.TRAIN
                    else TransportMode.CAR,
                cachedActivities = cachedActivities) { progress ->
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
      val intermediateActivities =
          activityList.filter { act -> !originalActivityList.contains(act) }
      val schedule =
          attemptRescheduleIfNeeded(
              enhancedTripProfile = enhancedTripProfile,
              originalOptimizedRoute = optimizedRoute,
              activityList = activityList,
              intermediateActivities = intermediateActivities,
              cachedActivities = cachedActivities) { progress ->
                onProgress(
                    progression.selectActivities +
                        progression.optimizeRoute +
                        progression.fetchInBetweenActivities +
                        progression.scheduleTrip * progress)
              }

      check(schedule.isNotEmpty()) { "Rescheduled trip is empty" }
      // ---- STEP 4: Change schedule if needed ----
      val finalSchedule =
          adjustFinalSchedule(
              originalSchedule = schedule,
              originalOrderedRoute = optimizedRoute,
              enhancedTripProfile = enhancedTripProfile,
              intermediateActivities = intermediateActivities,
              isRandom = isRandomTrip,
              cachedActivities = cachedActivities) { progress ->
                onProgress(
                    progression.selectActivities +
                        progression.optimizeRoute +
                        progression.fetchInBetweenActivities +
                        progression.scheduleTrip +
                        progression.finalScheduling * progress)
              }

      check(finalSchedule.isNotEmpty()) { "Final scheduled trip is empty" }

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
   * @param cachedActivities A list of all the activities that were pulled but we didn't use
   * @return List of new Activities between start and end.
   */
  suspend fun generateActivitiesBetween(
      start: Location,
      end: Location,
      count: Int,
      cachedActivities: MutableList<Activity>,
      allActivities: MutableList<Activity>
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
          activitySelector.getActivitiesNearWithPreferences(
              coords = coord,
              radius = RADIUS_NEW_ACTIVITY_M,
              1,
              allActivities.map { it.getName() },
              cachedActivities = cachedActivities)

      if (activity.isNotEmpty()) {
        val act = activity.first()
        newActivities.add(act)
        // Get the element that corresponds to the activity in the cache and remove it if it exists
        val sameActivityInCache =
            cachedActivities.firstOrNull {
              it.location.sameLocation(act.location) && it.getName() == act.getName()
            }

        if (sameActivityInCache != null) {
          cachedActivities.remove(sameActivityInCache)
        }
      }
    }

    return newActivities
  }

  /**
   * Adds intermediate activities between main locations in the optimized route based on distance.
   *
   * @param optimizedRoute The optimized route containing main locations.
   * @param activities The mutable list of activities to which new activities will be added.
   * @param mode The transport mode for route optimization.
   * @param distancePerStop The approximate distance for which we do a stop
   * @param cachedActivities A list of all the activities that were pulled but we didn't use
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return A new OrderedRoute including the in-between activities.
   */
  suspend fun addInBetweenActivities(
      optimizedRoute: OrderedRoute,
      activities: MutableList<Activity>,
      mode: TransportMode = TransportMode.CAR,
      distancePerStop: Double = DISTANCE_PER_STOP_KM,
      cachedActivities: MutableList<Activity>,
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
          (distKm / distancePerStop).toInt()
        }

    // 4. Insert intermediate activities along each segment
    // Create a map: startSeg -> List<Activity>
    val intermediateActivitiesBySegment = mutableMapOf<Location, MutableList<Activity>>()

    segmentPairs.forEachIndexed { index, (startSeg, endSeg) ->
      val numStops = stopsPerSegment[index]
      if (numStops <= 0) return@forEachIndexed

      // Generate activities for this segment
      val newActivities =
          generateActivitiesBetween(startSeg, endSeg, numStops, cachedActivities, activities)
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
    var index = 0
    for ((startSeg, activities) in intermediateActivitiesBySegment) {
      index++
      if (index >= MAX_INBETWEEN_ACTIVITIES_SEGMENTS) break
      // Get the start segment index in the optimized route
      val startIndex = newOrderedLocations.indexOfFirst { it.sameLocation(startSeg) }
      if (startIndex == -1) continue

      // Add the activities location after the start segment
      for (i in 1..min(activities.size, MAX_INBETWEEN_ACTIVITES_BY_SEGMENT)) {
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
  /** Extract scheduled activities from a produced schedule. */
  private fun extractActivitiesFromSchedule(schedule: List<TripElement>): List<Activity> {
    return schedule.mapNotNull {
      when (it) {
        is TripElement.TripActivity -> it.activity
        else -> null
      }
    }
  }

  /**
   * Very conservative activity equality/matching: match by location identity and estimatedTime.
   * Replace with id-comparison if Activity has an identifier field.
   */
  private fun activitiesMatch(a: Activity, b: Activity): Boolean {
    return a.estimatedTime == b.estimatedTime && a.location.sameLocation(b.location)
  }

  /**
   * Build a new OrderedRoute by removing given activity locations and marking affected segments
   * durations as INVALID_DURATION so that the route optimizer will recompute them.
   *
   * Returns Pair(newOrderedRoute, changedIndexes) where changedIndexes are the indices that were
   * marked invalid/need recomputation.
   */
  private fun buildRouteAfterRemovals(
      optimizedRoute: OrderedRoute,
      toRemoveLocations: Set<Location>
  ): Pair<OrderedRoute, List<Int>> {
    val newOrderedLocations = optimizedRoute.orderedLocations.toMutableList()
    val newSegmentDurations = optimizedRoute.segmentDuration.toMutableList()
    val changedIndexes = mutableListOf<Int>()

    // Remove each location (only the first occurrence) and mark adjacent durations invalid.
    for (loc in toRemoveLocations) {
      val idx = newOrderedLocations.indexOfFirst { it.sameLocation(loc) }
      if (idx == -1) continue

      // Mark previous segment as invalid
      if (idx - 1 >= 0 && idx - 1 < newSegmentDurations.size) {
        newSegmentDurations[idx - 1] = INVALID_DURATION
        changedIndexes.add(idx - 1)
      }

      // If the next segment exists, remove its duration entry (we merge segments)
      if (idx < newSegmentDurations.size) {
        // We remove the next segmentDuration entry, but mark the resulting merged segment as
        // invalid
        // only if the previous segment index exists; otherwise set the current to INVALID.
        newSegmentDurations.removeAt(idx)
        // After removal, ensure the previous index (if exists) is invalid (we already added it).
      }

      // Remove the location itself
      newOrderedLocations.removeAt(idx)
    }

    val newRoute =
        OrderedRoute(
            orderedLocations = newOrderedLocations,
            totalDuration = optimizedRoute.totalDuration,
            segmentDuration = newSegmentDurations)

    return Pair(newRoute, changedIndexes.distinct())
  }

  /**
   * Done with AI Perform the rescheduling stage:
   * - Compute deficitSeconds = sum(missing estimatedTime) + missingCount * penalty (the overtime of
   *   the trip)
   * - Randomly remove activities from the full activity list until removedSeconds >= deficitSeconds
   * - Update the OrderedRoute by removing those locations and invalidating the affected segment
   *   durations
   * - Recompute route with routeOptimizer.recomputeOrderedRoute
   * - Re-run scheduleTrip on the recomputed route and updated activity list
   *
   * @param enhancedTripProfile The enhanced trip profile containing the original trip profile
   * @param originalOptimizedRoute The original optimized route before scheduling.
   * @param activityList The mutable list of activities to schedule.
   * @param intermediateActivities The list of intermediate activities added between main locations.
   *   (should not remove them)
   * @param cachedActivities A list of all the activities that were pulled but we didn't use.
   * @param onProgress Progression of the function.
   * @return The final scheduled trip after rescheduling attempt.
   */
  open suspend fun attemptRescheduleIfNeeded(
      enhancedTripProfile: EnhancedTripProfile,
      originalOptimizedRoute: OrderedRoute,
      activityList: MutableList<Activity>,
      intermediateActivities: List<Activity>,
      cachedActivities: MutableList<Activity>,
      onProgress: (Float) -> Unit
  ): List<TripElement> {
    // Filter out intermediate activities from the activity list to avoid removing them
    val normalActivities = activityList.filter { !intermediateActivities.contains(it) }

    // 1) Run first scheduling pass
    val firstSchedule =
        scheduleTrip(
            enhancedTripProfile.tripProfile, originalOptimizedRoute, activityList, scheduleParams) {
              onProgress(rescheduleProgression.schedule * it)
            }

    // 2) Compute which activities were missing compared to original activity list
    val scheduledActs = extractActivitiesFromSchedule(firstSchedule)
    val missingActivities =
        activityList.filter { act ->
          scheduledActs.none { scheduled -> activitiesMatch(act, scheduled) }
        }

    if (missingActivities.isEmpty()) {
      // Nothing missing — keep the first schedule
      return firstSchedule
    }

    // 3) Compute deficit seconds based on missing activities and penalty-per-activity
    val missingSumSec = missingActivities.sumOf { it.estimatedTime.toDouble().roundToLong() }
    val penaltySec = missingActivities.size * RESCHEDULE_PENALTY_PER_ACTIVITY_SEC.toLong()
    val deficitSeconds = missingSumSec + penaltySec

    // 4) Randomly remove activities across the whole trip until we cover the deficit
    val rand = Random.Default

    val candidateList = normalActivities.toMutableList()
    candidateList.shuffle(rand)

    data class Cluster(val center: Location, val acts: MutableList<Activity>)

    // Build map: preferredLocation -> closest activities
    val clusters =
        enhancedTripProfile.newPreferredLocations.map { prefLoc ->
          Cluster(center = prefLoc, acts = mutableListOf())
        }

    // Assign each candidate activity to the nearest preferred location
    for (act in candidateList) {
      val loc = act.location
      val nearestCluster =
          clusters.minByOrNull { cluster ->
            loc.coordinate.haversineDistanceTo(cluster.center.coordinate)
          }!!
      nearestCluster.acts.add(act)
    }

    // Identify clusters that have only ONE activity → “protected”
    val protectedActivities =
        clusters
            .filter { it.acts.size <= 1 } // clusters with <= 1 activity
            .flatMap { it.acts }
            .toMutableSet()

    // Also protect intermediate activities
    protectedActivities.addAll(intermediateActivities)

    val toRemove = mutableListOf<Activity>()
    var remainingDeficit = deficitSeconds
    val pool = candidateList.toMutableList()

    while (remainingDeficit > 0 && pool.isNotEmpty()) {

      // Respect protected clusters unless impossible
      val allowedPool =
          pool
              .filter { it !in protectedActivities }
              .ifEmpty { pool } // fallback to full pool if needed

      // 1) overshoots
      val overshoots = allowedPool.filter { it.estimatedTime.toLong() >= remainingDeficit }
      val bestOvershoot = overshoots.minByOrNull { it.estimatedTime.toLong() - remainingDeficit }

      // 2) undershoot fallback
      val bestUndershoot =
          allowedPool
              .filter { it.estimatedTime.toLong() < remainingDeficit }
              .maxByOrNull { it.estimatedTime.toLong() }

      val chosen = bestOvershoot ?: bestUndershoot ?: break

      toRemove.add(chosen)
      remainingDeficit -= chosen.estimatedTime.toLong()
      pool.remove(chosen)

      // If a protected activity had to be removed (fallback case), unprotect the cluster
      if (chosen in protectedActivities) {
        protectedActivities.remove(chosen)
      }

      onProgress(
          rescheduleProgression.schedule +
              rescheduleProgression.analyzeAndRemove *
                  (1f - remainingDeficit.toFloat() / deficitSeconds))
    }

    if (toRemove.isEmpty()) {
      // Could not find candidates to remove, return original result
      return firstSchedule
    }

    // 5) Update activityList by removing chosen activities
    val removedLocations = toRemove.map { it.location }.toSet()
    activityList.removeAll { act ->
      !intermediateActivities.contains(act) && toRemove.any { rem -> activitiesMatch(act, rem) }
    }
    // Put the valid activities that we had to remove due to the time to the cache in case we use
    // them later
    cachedActivities.addAll(toRemove)

    // 6) Build new OrderedRoute removing those locations and invalidating durations
    val (routeAfterRemoval, changedIndexes) =
        buildRouteAfterRemovals(originalOptimizedRoute, removedLocations)

    // 7) Recompute route durations for invalid segments
    val finalOptimizedRoute =
        try {
          routeOptimizer.recomputeOrderedRoute(
              routeAfterRemoval,
              changedIndexes,
              mode =
                  if (enhancedTripProfile.tripProfile.preferences.contains(
                      Preference.PUBLIC_TRANSPORT))
                      TransportMode.TRAIN
                  else TransportMode.CAR,
              INVALID_DURATION) {
                onProgress(
                    rescheduleProgression.schedule +
                        rescheduleProgression.analyzeAndRemove +
                        rescheduleProgression.recomputeRoute * it)
              }
        } catch (e: Exception) {
          // Recompute failed — fall back to firstSchedule
          Log.w("TripAlgorithm", "Recompute after removals failed: ${e.message}")
          return firstSchedule
        }

    // 8) Re-run scheduleTrip with recomputed route and the pruned activity list
    val finalSchedule =
        try {
          scheduleTrip(
              enhancedTripProfile.tripProfile, finalOptimizedRoute, activityList, scheduleParams) {
                onProgress(
                    rescheduleProgression.schedule +
                        rescheduleProgression.analyzeAndRemove +
                        rescheduleProgression.recomputeRoute +
                        rescheduleProgression.reschedule * it)
              }
        } catch (e: Exception) {
          Log.w("TripAlgorithm", "Scheduling after recompute failed: ${e.message}")
          return firstSchedule
        }

    return finalSchedule
  }

  /**
   * Adjusts the final schedule to ensure it ends on the correct day.
   *
   * @param originalSchedule The original schedule to be adjusted.
   * @param originalOrderedRoute The original ordered route.
   * @param enhancedTripProfile The enhanced trip profile containing the original trip profile. //
   * @param intermediateActivities The list of activities between distant locations.
   * @param isRandom Whether the trip is random.
   * @param cachedActivities A list of all the activities that were pulled but we didn't use
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return The adjusted schedule.
   *
   * Done with AI
   */
  private suspend fun adjustFinalSchedule(
      originalSchedule: List<TripElement>,
      originalOrderedRoute: OrderedRoute,
      enhancedTripProfile: EnhancedTripProfile,
      intermediateActivities: List<Activity> = emptyList(),
      isRandom: Boolean = false,
      cachedActivities: MutableList<Activity>,
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    onProgress(0.0f)

    // TODO: Make sure that when the trip is random, we keep "visiting the grandTour spot" as an
    // activity
    // Already ending on the correct day → nothing to change
    if (sameDate(originalSchedule.last().endDate, enhancedTripProfile.tripProfile.endDate)) {
      return originalSchedule
    }

    val allActivities =
        originalSchedule.filterIsInstance<TripElement.TripActivity>().map { it.activity }

    onProgress(finalSchedulingProgression.selectNewActivities)

    // Final scheduling by adding even more activities if needed
    val finalSchedule =
        addMoreActivities(
            originalOrderedRoute,
            allActivities,
            enhancedTripProfile,
            intermediateActivities,
            cachedActivities) {
              onProgress(
                  finalSchedulingProgression.selectNewActivities +
                      finalSchedulingProgression.optimize +
                      finalSchedulingProgression.finalScheduling * it)
            }

    onProgress(1.0f)
    return finalSchedule
  }

  /**
   * Done by AI Attempts to add an activity from the [cachedActivities] list to the [activityList].
   * * Logic:
   * 1. Invalidates and removes cached activities that are not near any preferred location.
   * 2. Selects the valid candidates based on the strategy (oneDay or normal).
   * 3. Adds multiple activities if they fit within the time limit.
   *
   * @param activityList The mutable list of current activities in the trip.
   * @param cachedActivities The mutable list of backup/cached activities.
   * @param preferredLocations The list of user's preferred locations (to validate relevance).
   * @param oneDay Whether we are on the last day (prioritize small activities).
   * @return `true` if at least one activity was added, `false` otherwise.
   */
  private fun addCachedActivity(
      activityList: MutableList<Activity>,
      cachedActivities: MutableList<Activity>,
      preferredLocations: List<Location>,
      oneDay: Boolean
  ): Boolean {
    if (cachedActivities.isEmpty()) return false

    val iterator = cachedActivities.iterator()
    val validCandidates = mutableListOf<Activity>()

    // 1. Remove activities not near any preferred location
    while (iterator.hasNext()) {
      val activity = iterator.next()

      val isNearPreferred =
          preferredLocations.any { pref ->
            activity.location.coordinate.haversineDistanceTo(pref.coordinate) <=
                RADIUS_CACHED_ACTIVITIES_KM
          }

      if (!isNearPreferred) {
        // Remove invalid activity from cache permanently
        iterator.remove()
      } else {
        // Only consider if not already in the trip
        val alreadyInTrip = activityList.any { it.location.sameLocation(activity.location) }
        if (!alreadyInTrip) {
          validCandidates.add(activity)
        }
      }
    }

    if (validCandidates.isEmpty()) return false

    // 2. Sort candidates based on the strategy
    val sortedCandidates =
        if (oneDay) {
          // If it's the last day, prioritize the smallest activities to fill gaps
          validCandidates.sortedBy { it.estimatedTime }
        } else {
          // Otherwise, prioritize the ones closest to preferred locations
          validCandidates.sortedBy { candidate ->
            preferredLocations.minOfOrNull { loc ->
              candidate.location.coordinate.haversineDistanceTo(loc.coordinate)
            } ?: Double.MAX_VALUE
          }
        }

    // 3. Batch add activities up to the limit
    var currentBatchTime = 0.0
    val toAdd = mutableListOf<Activity>()

    for (candidate in sortedCandidates) {
      val durationHours = candidate.estimatedTime / 3600.0
      // Calculate cost including penalty
      val cost = durationHours + (BATCH_ADD_PENALTY_MINUTES / 60.0)

      if (currentBatchTime + cost <= MAX_BATCH_ADDED_TIME_HOURS) {
        toAdd.add(candidate)
        currentBatchTime += cost
      }
    }

    if (toAdd.isNotEmpty()) {
      activityList.addAll(toAdd)
      cachedActivities.removeAll(toAdd)
      return true
    }

    return false
  }

  /**
   * kdoc done with AI Iteratively adds activities to a trip until the trip reaches its target end
   * date.
   *
   * This function enriches the trip schedule by adding city visits and Grand Tour activities
   * according to the user's preferences, while continuously rescheduling and optimizing the route.
   * It ensures that intermediate and urban preferences are respected and avoids duplicate or
   * conflicting activities.
   *
   * **Algorithm overview:**
   * 1. Start with the original list of activities and reschedule them using
   *    [attemptRescheduleIfNeeded], reporting initial progress via [onProgress].
   * 2. While the last scheduled trip element does not reach the trip's end date:
   * - If the user has both INTERMEDIATE_STOPS and URBAN preferences:
   * - Randomly choose to add either a city activity or a Grand Tour activity.
   * - If only INTERMEDIATE_STOPS is present, attempt to add a Grand Tour activity.
   * - Otherwise, attempt to add a city activity.
   * - Keep track of which activities were successfully added to avoid duplicates.
   * 3. After each addition, optimize the route using [routeOptimizer.optimize].
   * 4. Recompute the schedule using [attemptRescheduleIfNeeded] to ensure timing and ordering
   *    remain valid.
   * 5. Update progress after each cycle.
   * 6. Repeat until the trip's end date is reached or a maximum number of cycles is exceeded.
   *
   * @param originalOrderedRoute The initial [OrderedRoute] containing the trip's ordered locations.
   * @param activityList The current list of [Activity] objects to schedule in the trip.
   * @param enhancedTripProfile The enhanced trip profile.
   * @param intermediateActivities Optional list of [Activity] that must remain in the schedule
   *   during rescheduling.
   * @param cachedActivities A list of all the activities that were pulled but we didn't use
   * @param onProgress Lambda to receive progress updates as a [Float] between 0 and 1.
   * @return A [List] of [TripElement] representing the fully scheduled trip with all added
   *   activities.
   */
  private suspend fun addMoreActivities(
      originalOrderedRoute: OrderedRoute,
      activityList: List<Activity>,
      enhancedTripProfile: EnhancedTripProfile,
      intermediateActivities: List<Activity>,
      cachedActivities: MutableList<Activity>,
      onProgress: (Float) -> Unit = {},
  ): List<TripElement> {
    val allActivities = activityList.toMutableList()

    // Schedule trip
    var schedule =
        attemptRescheduleIfNeeded(
            enhancedTripProfile,
            originalOrderedRoute,
            allActivities,
            intermediateActivities,
            cachedActivities) {
              onProgress(it * 0.25f)
            }

    var addedFromCache = true
    while (addedFromCache &&
        !sameDate(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)) {
      addedFromCache = false
      val numberOfDays =
          dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
      val oneDay = numberOfDays <= 1
      val limit = min(7, numberOfDays)
      for (i in 1..limit) {
        val added =
            addCachedActivity(
                allActivities, cachedActivities, enhancedTripProfile.newPreferredLocations, oneDay)
        if (added) {
          addedFromCache = true
        } else {
          // If we failed to add an activity, stop trying
          break
        }
      }
      if (addedFromCache) {
        // Re-optimize route with the new cached activity included
        val updatedNewActivities =
            allActivities.filter { act ->
              activityList.none { it.location.sameLocation(act.location) }
            }
        val optimizedRoute =
            routeOptimizer.optimize(
                enhancedTripProfile.tripProfile.arrivalLocation!!,
                enhancedTripProfile.tripProfile.departureLocation!!,
                (originalOrderedRoute.orderedLocations + updatedNewActivities.map { it.location }),
                allActivities,
                if (enhancedTripProfile.tripProfile.preferences.contains(
                    Preference.PUBLIC_TRANSPORT))
                    TransportMode.TRAIN
                else TransportMode.CAR,
            ) {}

        // Re-schedule
        schedule =
            attemptRescheduleIfNeeded(
                enhancedTripProfile,
                optimizedRoute,
                allActivities,
                intermediateActivities,
                cachedActivities) {}

        // Update progress (using a rough estimate logic similar to the loop below)
        onProgress(
            (0.25f +
                (0.75f /
                    (1 +
                        dateDifference(
                            schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)))))
      }
    }

    // If we are on the same day, we finished
    if (sameDate(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)) return schedule

    // Else we need to add even more activities
    // Use enhancedTripProfile.tripProfile for preference checks and date checks
    val hasUrbanPreference = enhancedTripProfile.tripProfile.preferences.contains(Preference.URBAN)

    var addCityWorks = true
    var addGrandTourWorks = true
    var stayAtCenterRetry = true
    var addPreferences = true
    val visitedLocations = mutableListOf<Location>()
    var numberOfCycle = 0
    val missingDays =
        dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
    val limit = min(14, missingDays * 2) // arbitrary value to not have an infinite loop
    // List of all the locations we already fetched activities in the addPreferences loop
    val preferencesAddedVisited = mutableListOf<Location>()
    while ((addCityWorks || addGrandTourWorks || addPreferences) &&
        numberOfCycle < limit &&
        !sameDate(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)) {
      numberOfCycle++
      // If we could not use addCity or addGrandTour or if we already used a third of the cycles,
      // add more preferences and try to fetch new activities with this (This can be then countered
      // by the swipe activities if the activities are not pleasing the user)
      if (addPreferences && ((!addCityWorks && !addGrandTourWorks) || numberOfCycle >= limit / 3)) {
        val missingDays =
            dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
        val activityToAdd =
            PreferenceCategories.activityTypePreferences +
                PreferenceCategories.environmentPreferences
        activitySelector.updatePreferences(
            enhancedTripProfile.tripProfile.preferences.union(activityToAdd).toList())

        // Select a location to fetch from
        // Candidates are locations in preferredLocations that haven't been processed yet
        val candidates =
            enhancedTripProfile.newPreferredLocations.filter { loc ->
              preferencesAddedVisited.none { it.sameLocation(loc) }
            }

        if (candidates.isNotEmpty()) {
          // Identify which candidates are "Major Cities"
          val majorCityCandidates =
              candidates.mapNotNull { candidate ->
                val config =
                    swissMajorCities.firstOrNull { cityConfig ->
                      candidate.coordinate.haversineDistanceTo(cityConfig.location.coordinate) <=
                          cityConfig.radius
                    }
                if (config != null) candidate to config.maxDays else null
              }

          // First try to see if it is a big city because most activities are in cities
          // If not just take one randomly
          val selectedLocation =
              if (majorCityCandidates.isNotEmpty()) {
                // Prioritize by maxDays descending
                majorCityCandidates.maxByOrNull { it.second }!!.first
              } else {
                // Random fallback
                candidates.random()
              }

          preferencesAddedVisited.add(selectedLocation)

          // Add an arbitrary upper bound limit to make sure we don't fetch too much
          val fetchLimit = min(10, missingDays.toInt())

          val newActs =
              activitySelector.getActivitiesNearWithPreferences(
                  coords = selectedLocation.coordinate,
                  limit = fetchLimit,
                  activityBlackList = allActivities.map { it.getName() },
                  cachedActivities = cachedActivities)

          if (newActs.isNotEmpty()) {
            allActivities.addAll(newActs)
          }
        } else {
          addPreferences = false
        }
      }

      val rand = Random.Default

      when {
        // Only city
        addCityWorks && hasUrbanPreference -> {
          addCityWorks = addCityActivity(enhancedTripProfile, allActivities)
        }

        // Any of the two since the user doesn't have any preferences
        addGrandTourWorks && addCityWorks -> {
          if (rand.nextBoolean()) {
            addCityWorks = addCityActivity(enhancedTripProfile, allActivities)
          } else {
            val result =
                addGrandTourActivities(
                    enhancedTripProfile,
                    allActivities,
                    visitedLocations,
                    stayAtCenterRetry,
                    cachedActivities)
            // If we were at the center, we can still retry without the center
            addGrandTourWorks = result.added || stayAtCenterRetry
            // If we successfully added a new grand tour activity, try to add a city activity (it
            // costs nothing to compute)
            if (result.added) {
              addCityWorks = addCityActivity(enhancedTripProfile, allActivities)
            }
            stayAtCenterRetry = result.shouldRetryCenter
          }
        }

        // Fallback: try to add a city activity first since it doesn't cost anything
        else -> {
          if (addCityWorks) {
            addCityWorks = addCityActivity(enhancedTripProfile, allActivities)
          } else {
            val result =
                addGrandTourActivities(
                    enhancedTripProfile,
                    allActivities,
                    visitedLocations,
                    stayAtCenterRetry,
                    cachedActivities)
            addGrandTourWorks = result.added || stayAtCenterRetry
            // If we successfully added a new grand tour activity, try to add a city activity (it
            // costs nothing to compute)
            if (result.added) {
              addCityWorks = addCityActivity(enhancedTripProfile, allActivities)
              addPreferences = true
            }
            stayAtCenterRetry = result.shouldRetryCenter
          }
        }
      }

      // Cost nothing to call and could add activities if when we fetched some for a new city that
      // we added for example we got too much
      val currentDiff =
          dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
      val oneDay = currentDiff <= 1
      addCachedActivity(
          allActivities, cachedActivities, enhancedTripProfile.newPreferredLocations, oneDay)
      val newActivities =
          allActivities.filter { act ->
            activityList.none { it.location.sameLocation(act.location) }
          }
      val optimizedRoute =
          routeOptimizer.optimize(
              enhancedTripProfile.tripProfile.arrivalLocation!!,
              enhancedTripProfile.tripProfile.departureLocation!!,
              (originalOrderedRoute.orderedLocations + newActivities.map { it.location }),
              allActivities,
              mode =
                  if (enhancedTripProfile.tripProfile.preferences.contains(
                      Preference.PUBLIC_TRANSPORT))
                      TransportMode.TRAIN
                  else TransportMode.CAR) {}

      schedule =
          attemptRescheduleIfNeeded(
              enhancedTripProfile,
              optimizedRoute,
              allActivities,
              intermediateActivities,
              cachedActivities) {}

      onProgress(
          (0.25f +
              (0.75f /
                  (1 +
                      dateDifference(
                          schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)))))
    }

    onProgress(1.0f)
    return schedule
  }

  /**
   * Done with AI Attempts to add a “Visit <City>” activity to the trip based on the user’s
   * preferred locations.
   *
   * The function works as follows:
   * 1. Shuffle the user's preferred locations.
   * 2. For each location in the shuffled list, check whether it falls inside the radius of any
   *    major Swiss city (defined in `swissMajorCities`).
   * 3. If it matches:
   * - Create an activity for visiting that city (8 hours).
   * - If an identical activity is NOT already in the activity list, add it immediately.
   * - If such an activity already exists, store the new one as a fallback candidate.
   * 4. After scanning all preferred locations, if at least one fallback candidate exists, add the
   *    first one.
   *
   * This ensures:
   * - At most **one** city-visit activity is added.
   * - Duplicate city-visit activities are avoided.
   * - A fallback exists in case the first matched city is already present.
   *
   * @param enhancedTripProfile The enhanced trip profile containing preferred locations.
   * @param activityList The mutable list of activities where the new city activity may be added.
   * @return `true` if a city activity was added (either directly or via fallback), `false`
   *   otherwise.
   */
  private fun addCityActivity(
      enhancedTripProfile: EnhancedTripProfile,
      activityList: MutableList<Activity>,
  ): Boolean {
    // Shuffle user preferred locations for randomness
    val shuffled = enhancedTripProfile.newPreferredLocations.shuffled()

    val tempCandidates = mutableListOf<Activity>() // store duplicates as fallback

    for (loc in shuffled) {
      // Try to match the location to a major Swiss city
      val matchingCityConfig =
          swissMajorCities.firstOrNull { (cityLoc, radiusKm, _) ->
            loc.coordinate.haversineDistanceTo(cityLoc.coordinate) <= radiusKm
          }

      if (matchingCityConfig != null) {
        val (cityLoc, _, maxDays) = matchingCityConfig

        val activityName = context.getString(R.string.city_visit_name, cityLoc.name)

        val visitDurationHours = if (maxDays >= 1.0) 8 else 4
        val estimatedTimeSeconds = visitDurationHours * 3600

        // Create new activity
        val newActivity =
            Activity(
                location = cityLoc.copy(name = activityName),
                estimatedTime = estimatedTimeSeconds,
                startDate = Timestamp.now(), // placeholder
                endDate = Timestamp.now(), // placeholder
                description = context.getString(R.string.city_activity_description),
                imageUrls = emptyList(),
            )

        val existingCount = activityList.count { it.location.sameLocation(newActivity.location) }

        if (existingCount == 0) {
          activityList.add(newActivity)
          return true
        }

        // Add to tempCandidates only if it doesn't already exist more than allowed
        if (existingCount < maxDays.toInt()) {
          tempCandidates.add(newActivity)
        }
      }
    }

    if (tempCandidates.isNotEmpty()) {
      activityList.add(tempCandidates.first())
      return true
    }

    return false
  }

  /**
   * Done with AI
   *
   * @param enhancedTripProfile The enhanced trip profile, containing preferred locations and trip
   *   preferences.
   * @param activityList The mutable list of activities; newly created activities will be added
   *   here.
   * @param visitedList The list of already visited Grand Tour locations; prevents duplicate stops.
   * @param stayAtCenter Flag determining which selection strategy to use:
   * - `true`: dynamic reference along start-to-end line.
   * - `false`: closest city to either start or end of trip.
   *
   * @param cachedActivities A list of all the activities that were pulled but we didn't use
   * @return `true` if at least one activity was successfully added, `false` otherwise.
   */
  private suspend fun addGrandTourActivities(
      enhancedTripProfile: EnhancedTripProfile,
      activityList: MutableList<Activity>,
      visitedList: MutableList<Location>,
      stayAtCenter: Boolean,
      cachedActivities: MutableList<Activity>
  ): AddGrandTourResult {
    // 1. Load Grand Tour cities
    val grandTour =
        context.resources.getStringArray(R.array.grand_tour).map { line ->
          val parts = line.split(";")
          Location(
              coordinate = Coordinate(parts[1].toDouble(), parts[2].toDouble()),
              name = parts[0],
              imageUrl = "")
        }

    val startCoord = enhancedTripProfile.tripProfile.arrivalLocation!!.coordinate
    val endCoord = enhancedTripProfile.tripProfile.departureLocation!!.coordinate

    // Detect circular trip
    val isCircularTrip = startCoord.haversineDistanceTo(endCoord) < 5.0 // Arbitrary estimate

    // 2. Define strategy helper
    suspend fun tryStrategy(useCenterStrategy: Boolean): Boolean {

      val availableCities =
          grandTour.filter { gtCity ->
            // A. Check if the raw GT point is already used
            val isGtUsed =
                visitedList.any { it.sameLocation(gtCity) } ||
                    enhancedTripProfile.newPreferredLocations.any { it.sameLocation(gtCity) }

            if (isGtUsed) return@filter false

            // B. Check if the associated Major City is already used
            val associatedCityConfig =
                swissMajorCities.firstOrNull { majorCity ->
                  gtCity.coordinate.haversineDistanceTo(majorCity.location.coordinate) <=
                      majorCity.radius
                }

            if (associatedCityConfig != null) {
              val majorCityLoc = associatedCityConfig.location
              val isCityUsed =
                  visitedList.any { it.sameLocation(majorCityLoc) } ||
                      enhancedTripProfile.newPreferredLocations.any {
                        it.sameLocation(majorCityLoc)
                      }

              if (isCityUsed) return@filter false
            }

            true // Passes all checks
          }

      if (availableCities.isEmpty()) return false

      // 3. Select Candidates
      val candidatesToTry =
          if (isCircularTrip) {
            // Pick the 2 closest valid cities and shuffle them for a bit of randomness.
            availableCities
                .sortedBy { it.coordinate.haversineDistanceTo(startCoord) }
                .take(2)
                .shuffled()
          } else {
            val scoredCities =
                availableCities.map { city ->
                  val distToStart = city.coordinate.haversineDistanceTo(startCoord)
                  val distToEnd = city.coordinate.haversineDistanceTo(endCoord)

                  val cost =
                      if (useCenterStrategy) {
                        distToStart + distToEnd // Intermediate
                      } else {
                        min(distToStart, distToEnd) // Hub
                      }
                  city to cost
                }
            scoredCities.sortedBy { it.second }.take(2).map { it.first }.shuffled()
          }

      if (candidatesToTry.isEmpty()) {
        return false
      }

      val gtCity = candidatesToTry.shuffled().first()
      // 4. Try to add activities
      // Resolve to Major City if applicable
      val associatedCity =
          swissMajorCities
              .firstOrNull { majorCity ->
                gtCity.coordinate.haversineDistanceTo(majorCity.location.coordinate) <=
                    majorCity.radius
              }
              ?.location

      // This is the location we will actually add to the trip
      val finalCandidate = associatedCity ?: gtCity

      // Fetch activities near the FINAL candidate
      val newActivities =
          activitySelector.getActivitiesNearWithPreferences(
              coords = finalCandidate.coordinate,
              radius = RADIUS_NEW_ACTIVITY_M,
              limit = 3,
              activityBlackList = activityList.map { it.getName() },
              cachedActivities = cachedActivities)

      if (newActivities.isNotEmpty()) {
        activityList.addAll(newActivities)
        visitedList.add(finalCandidate)
        // Add to preferred locations so the rest of the algorithm treats it as a hub
        enhancedTripProfile.newPreferredLocations.add(finalCandidate)
        return true
      }

      return false
    }

    // 5. Execute Strategies
    if (tryStrategy(stayAtCenter)) {
      return AddGrandTourResult(true, !stayAtCenter) // Toggle strategy
    }

    if (tryStrategy(!stayAtCenter)) {
      return AddGrandTourResult(true, stayAtCenter)
    }

    return AddGrandTourResult(false, stayAtCenter)
  }

  /**
   * Done with AI Checks if two Timestamps represent the same date.
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

  /**
   * Done with AI Computes the difference between two timestamps in the specified [ChronoUnit].
   *
   * @param date1 The first timestamp.
   * @param date2 The second timestamp.
   * @param zone The time zone to consider for date conversion (default system zone).
   * @param unit The unit in which to return the difference (default DAYS).
   * @return The difference between date1 and date2 in the given [unit]. Positive if date2 is after
   *   date1.
   */
  private fun dateDifference(
      date1: Timestamp,
      date2: Timestamp,
      zone: ZoneId = ZoneId.systemDefault(),
      unit: ChronoUnit = ChronoUnit.DAYS
  ): Long {
    val d1 = date1.toDate().toInstant().atZone(zone).toLocalDate()
    val d2 = date2.toDate().toInstant().atZone(zone).toLocalDate()

    return unit.between(d1, d2)
  }
}
