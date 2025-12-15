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

const val DISTANCE_PER_STOP_KM = 90.0
const val RADIUS_NEW_ACTIVITY_M = 15000
const val INVALID_DURATION = -1.0
const val RESCHEDULE_PENALTY_PER_ACTIVITY_SEC = 0.25 * 3600 // 15 minutes
const val EPSILON = 1e-6f
// This is an arbitrary value to make the loop in addInBetweenActivities not run infinitely
const val MAX_INBETWEEN_ACTIVITIES_SEGMENTS = 3
const val MAX_INBETWEEN_ACTIVITES_BY_SEGMENT = 2
const val RADIUS_CACHED_ACTIVITIES_KM = 25
const val GRAND_TOUR_ACTIVITY_DURATION_SEC = 0.5 * 3600 // 30 min
const val ADDED_DISTANCE_CITY_KM = 20
const val NUMBER_OF_ACTIVITY_NEW_CITY = 3
const val MAX_INDEX = 5
const val ACTIVITY_TIME_PER_DAY_HOURS = 8
const val MAX_INDEX_FETCH_ACTIVITIES = 4
const val RADIUS_CITIES_TO_ASSOCIATE_KM = 15
const val MAX_INDEX_CACHED_ACTIVITIES = 10

/**
 * Some part of this file were made with AI but most of it was done by hands
 *
 * All the kdoc was done by AI
 *
 * Core algorithm class for generating and optimizing a travel trip.
 *
 * It handles activity selection, route optimization (TSP), adding intermediate stops, and
 * scheduling activities to fit the user's timeline.
 *
 * @property activitySelector The component responsible for finding and selecting activities.
 * @property routeOptimizer The component responsible for ordering locations to minimize travel
 *   time.
 * @property context The Android context, used primarily for resource access (e.g., city names).
 * @property scheduleParams Parameters configuring the trip scheduling logic.
 */
open class TripAlgorithm(
    private val activitySelector: SelectActivities,
    private val routeOptimizer: ProgressiveRouteOptimizer,
    private val context: Context,
    private val scheduleParams: ScheduleParams = ScheduleParams()
) {

  /**
   * ********************************************************
   * Init
   * *********************************************************
   */
  companion object {
    /**
     * Initializes the TripAlgorithm with the necessary components, applying initial preference
     * logic.
     *
     * This method adjusts the user's preferences based on the number of travelers (adults/children)
     * and ensures at least one Activity Type and one Environment preference is selected (by adding
     * all if none are selected).
     *
     * @param tripSettings The settings for the trip (locations, dates, travelers, initial
     *   preferences).
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
   * ********************************************************
   * Data class
   * *********************************************************
   */

  /**
   * Configuration for a major city, used for adding city-based activities.
   *
   * @param location The geographical location of the city center.
   * @param radius The radius in km to consider an activity as being in/near the city.
   * @param maxDays The maximum number of days/cityActivities to schedule for this city.
   */
  private data class CityConfig(val location: Location, val radius: Int, val maxDays: Double)

  /**
   * This data class is used to store a tripProfile and a copy of its preferredLocations while also
   * being able to add new locations without modifying the tripProfile. This is useful when we are
   * adding new cities in the trip during the algorithm's execution.
   *
   * @param tripProfile The original tripProfile.
   * @param newPreferredLocations A mutable list of new preferred locations, including any
   *   dynamically added locations.
   */
  data class EnhancedTripProfile(
      val tripProfile: TripProfile,
      val newPreferredLocations: MutableList<Location>
  )

  /**
   * Holds the different categories of activities being managed by the algorithm.
   *
   * @param intermediateActivities Activities inserted between main stops.
   * @param grandTourActivities Activities specifically related to the "Grand Tour" list (for random
   *   trips).
   * @param allActivities A cumulative list of all activities in the current trip plan.
   * @param cachedActivities Activities that were initially selected but later removed (e.g., for
   *   time constraints) and can be re-added.
   */
  data class Activities(
      val intermediateActivities: MutableList<Activity>,
      val grandTourActivities: MutableList<Activity>,
      val allActivities: MutableList<Activity>,
      val cachedActivities: MutableList<Activity>
  )

  /**
   * ********************************************************
   * Progression data classes
   * *********************************************************
   */

  /**
   * Represents the progression weight distribution for the main `computeTrip` steps.
   *
   * The sum of all properties must be 1.0f.
   *
   * @param selectActivities Weight allocated for the initial activity selection phase.
   * @param optimizeRoute Weight allocated for the initial route optimization (TSP) phase.
   * @param fetchInBetweenActivities Weight allocated for fetching and inserting intermediate stops.
   * @param scheduleTrip Weight allocated for the initial trip scheduling pass.
   * @param finalScheduling Weight allocated for the final scheduling and completion phase.
   */
  data class ComputeTripProgression(
      val selectActivities: Float,
      val optimizeRoute: Float,
      val fetchInBetweenActivities: Float,
      val scheduleTrip: Float,
      val finalScheduling: Float
  ) {
    init {
      val sum =
          selectActivities +
              optimizeRoute +
              scheduleTrip +
              fetchInBetweenActivities +
              finalScheduling
      require(abs(sum - 1.0f) < EPSILON) { "Progression values must sum to 1.0, but got $sum" }
    }
  }

  /**
   * Represents the relative weights within the [ComputeTripProgression.finalScheduling] phase.
   *
   * @param loopFilling The portion of the final phase allocated to the loop that adds/completes
   *   activities.
   * @param finalOptimization The portion of the final phase allocated to the very last
   *   optimization/scheduling pass.
   */
  data class FinalSchedulingProgression(val loopFilling: Float, val finalOptimization: Float) {
    init {
      val sum = loopFilling + finalOptimization
      require(abs(sum - 1.0f) < EPSILON) { "Final scheduling progression values must sum to 1.0" }
    }
  }

  /**
   * ********************************************************
   * Utils val
   * *********************************************************
   */

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
  private val allBasicPreferences =
      PreferenceCategories.environmentPreferences + PreferenceCategories.activityTypePreferences

  /**
   * ********************************************************
   * Progression val
   * *********************************************************
   */
  private val computeProgression: ComputeTripProgression =
      ComputeTripProgression(
          selectActivities = 0.15f,
          optimizeRoute = 0.15f,
          fetchInBetweenActivities = 0.05f,
          scheduleTrip = 0.05f,
          finalScheduling = 0.60f)

  // Defines how the 0.60f of 'finalScheduling' is split internally
  private val finalSchedulingWeights: FinalSchedulingProgression =
      FinalSchedulingProgression(
          loopFilling = 0.8f, // 80% of the 60% is for the iterative adding of activities
          finalOptimization = 0.2f // 20% of the 60% is for the last optimize pass
          )

  /**
   * ********************************************************
   * Main computation
   * *********************************************************
   */

  /**
   * Computes the optimized travel plan (route and schedule) based on user settings and profile.
   *
   * The process involves:
   * 1. Selecting activities based on preferences.
   * 2. Optimizing the route (Traveling Salesperson Problem solver).
   * 3. Inserting intermediate stops if enabled.
   * 4. Scheduling activities and removing any 'ghost' or over-time activities.
   * 5. Completing the schedule by adding more activities if time allows.
   * 6. Performing a final route optimization and scheduling.
   *
   * @param tripSettings The high-level settings provided by the user (dates, locations, travelers).
   * @param tripProfile The detailed profile of the trip, including initial locations.
   * @param isRandomTrip Flag indicating if the trip is a random generation (affects Grand Tour
   *   activity creation).
   * @param cachedActivities A list of activities previously removed or fetched, available for
   *   adding back to fill the time.
   * @param onProgress A callback function to report the computation progress (from 0.0 to 1.0).
   * @return A list of [TripElement] representing the final scheduled trip.
   * @throws IllegalArgumentException if the arrival or departure location is null.
   * @throws IllegalStateException if route optimization or activity selection fails.
   */
  suspend fun computeTrip(
      tripSettings: TripSettings,
      tripProfile: TripProfile,
      isRandomTrip: Boolean = false,
      cachedActivities: MutableList<Activity> = mutableListOf(),
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    val enhancedTripProfile =
        EnhancedTripProfile(
            tripProfile.copy(preferences = tripSettings.preferences),
            tripProfile.preferredLocations.toMutableList())
    val intermediateActivities = mutableListOf<Activity>()
    val grandTourActivities =
        if (isRandomTrip) {
          enhancedTripProfile.tripProfile.preferredLocations.drop(1).map { grandTourActivity(it) }
        } else {
          mutableListOf()
        }
    val allActivities = grandTourActivities.toMutableList()
    val activities =
        Activities(
            intermediateActivities,
            grandTourActivities.toMutableList(),
            allActivities,
            cachedActivities)

    try {
      val startLocation =
          tripSettings.arrivalDeparture.arrivalLocation
              ?: throw IllegalArgumentException("Arrival location must not be null")
      val endLocation =
          tripSettings.arrivalDeparture.departureLocation
              ?: throw IllegalArgumentException("Departure location must not be null")

      onProgress(0.0f)
      val selectedActivities =
          try {
            activitySelector.addActivities(activities.cachedActivities) { progress ->
              onProgress(computeProgression.selectActivities * progress)
            }
          } catch (e: Exception) {
            throw IllegalStateException("Failed to select activities: ${e.message}", e)
          }
      onProgress(computeProgression.selectActivities)

      val fullDestinationList = buildList {
        add(startLocation)
        addAll(selectedActivities.map { it.location })
        add(endLocation)
      }
      activities.allActivities.addAll(selectedActivities)

      Log.d("TripAlgorithm", "Full destination list: $fullDestinationList")
      var optimizedRoute =
          if (endLocation.haversineDistanceTo(startLocation) >= 1 ||
              selectedActivities
                  .isNotEmpty()) { // If they are at least 1 km apart or there are activities we can
            // use optimize otherwise we build a fake one that is valid
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
                    onProgress(
                        computeProgression.selectActivities +
                            computeProgression.optimizeRoute * progress)
                  }
            } catch (e: Exception) {
              throw IllegalStateException("Route optimization failed: ${e.message}", e)
            }
          } else {
            OrderedRoute(fullDestinationList, 1.0, listOf(1.0))
          }

      onProgress(computeProgression.selectActivities + computeProgression.optimizeRoute)
      check(optimizedRoute.totalDuration > 0) { "Optimized route duration is zero or negative" }

      // ---- STEP 2b: Insert in-between activities if preference enabled ----
      if (tripSettings.preferences.contains(Preference.INTERMEDIATE_STOPS)) {
        optimizedRoute =
            addInBetweenActivities(
                optimizedRoute = optimizedRoute,
                activities = activities,
                mode =
                    if (enhancedTripProfile.tripProfile.preferences.contains(
                        Preference.PUBLIC_TRANSPORT))
                        TransportMode.TRAIN
                    else TransportMode.CAR) { progress ->
                  onProgress(
                      computeProgression.selectActivities +
                          computeProgression.optimizeRoute +
                          computeProgression.fetchInBetweenActivities * progress)
                }
      }

      var currentProgressBase =
          computeProgression.selectActivities +
              computeProgression.optimizeRoute +
              computeProgression.fetchInBetweenActivities

      onProgress(currentProgressBase)

      var schedule =
          scheduleRemove(
              enhancedTripProfile = enhancedTripProfile,
              originalOptimizedRoute = optimizedRoute,
              activities = activities)

      currentProgressBase += computeProgression.scheduleTrip

      // Calculate the progress budget for the loop filling phase
      val loopFillingBudget =
          computeProgression.finalScheduling * finalSchedulingWeights.loopFilling
      val finalOptBudget =
          computeProgression.finalScheduling * finalSchedulingWeights.finalOptimization

      if (dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate) > 0) {
        // UNDER-SCHEDULED: We have time left, let's fill it
        completeSchedule(
            schedule,
            enhancedTripProfile,
            activities,
            progressStart = currentProgressBase,
            progressEnd = currentProgressBase + loopFillingBudget,
            onProgress = onProgress)
      } else if (dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate) <
          0) {
        // OVER-SCHEDULED: We are late, let's remove some activities
        // In this case, scheduleRemove takes the place of completeSchedule for progress reporting
        scheduleRemove(
            enhancedTripProfile = enhancedTripProfile,
            originalOptimizedRoute = optimizedRoute,
            activities = activities)
      }

      currentProgressBase += loopFillingBudget
      onProgress(currentProgressBase)

      val finalRoute =
          try {
            routeOptimizer.optimize(
                enhancedTripProfile.tripProfile.arrivalLocation!!,
                enhancedTripProfile.tripProfile.departureLocation!!,
                activities.allActivities.map { it.location },
                activities.allActivities,
                if (enhancedTripProfile.tripProfile.preferences.contains(
                    Preference.PUBLIC_TRANSPORT))
                    TransportMode.TRAIN
                else TransportMode.CAR) { progress ->
                  onProgress(currentProgressBase + finalOptBudget * progress)
                }
          } catch (_: Exception) {
            optimizedRoute // Fallback
          }

      schedule =
          scheduleRemove(
              enhancedTripProfile = enhancedTripProfile,
              originalOptimizedRoute = finalRoute,
              activities = activities)

      onProgress(1.0f)
      return schedule
    } catch (e: Exception) {
      Log.e("TripAlgorithm", "Trip computation failed", e)
      throw e
    }
  }

  /**
   * ********************************************************
   * Adding in between activities
   * *********************************************************
   */

  /**
   * Adds intermediate activities between main locations in the optimized route based on distance.
   * 1. Calculates the number of stops needed per segment based on `distancePerStop`.
   * 2. Generates new activities at interpolated coordinates for the stops.
   * 3. Inserts the new activities' locations into the route's location list.
   * 4. Recomputes the segment durations using the route optimizer for the new, longer route.
   *
   * @param optimizedRoute The initial optimized route containing main locations.
   * @param mode The transport mode for route optimization (CAR or TRAIN).
   * @param distancePerStop The approximate distance in KM for which to insert a stop.
   * @param activities The [Activities] data class to which new intermediate activities will be
   *   added.
   * @param onProgress A callback function to report progress (from 0.0 to 1.0).
   * @return A new [OrderedRoute] including the in-between activities.
   */
  suspend fun addInBetweenActivities(
      optimizedRoute: OrderedRoute,
      mode: TransportMode = TransportMode.CAR,
      distancePerStop: Double = DISTANCE_PER_STOP_KM,
      activities: Activities,
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
      val newActivities = generateActivitiesBetween(startSeg, endSeg, numStops, activities)

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
    for ((startSeg, activitiesList) in intermediateActivitiesBySegment) {
      index++
      if (index >= MAX_INBETWEEN_ACTIVITIES_SEGMENTS) break
      // Get the start segment index in the optimized route
      val startIndex = newOrderedLocations.indexOfFirst { it.sameLocation(startSeg) }
      if (startIndex == -1) continue

      // Add the activities location after the start segment
      for (i in 1..min(activitiesList.size, MAX_INBETWEEN_ACTIVITES_BY_SEGMENT)) {
        val activity = activitiesList[i - 1]
        // Because each time we add a new location, the next index shifts by 1
        val insertIndex = startIndex + i
        newOrderedLocations.add(insertIndex, activity.location)
        addedIndexes.add(insertIndex)
        // Set to INVALID_DURATION so that the re-computation knows to calculate it
        newSegmentDurations[insertIndex - 1] = INVALID_DURATION
        newSegmentDurations.add(insertIndex, INVALID_DURATION)
        activities.allActivities.add(activity)
        activities.intermediateActivities.add(activity)
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

  /**
   * Generates a list of "in-between" activities along the segment from start to end.
   *
   * It calculates interpolated coordinates between the start and end point, applies a small random
   * offset, and fetches the best matching activity for that point based on user preferences.
   *
   * @param start Starting [Location].
   * @param end Ending [Location].
   * @param count Number of activities to generate.
   * @param activities The [Activities] data class to check against for existing/cached activities.
   * @return List of new [Activity] objects found between start and end.
   */
  suspend fun generateActivitiesBetween(
      start: Location,
      end: Location,
      count: Int,
      activities: Activities
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
              activities.allActivities.map { it.getName() },
              cachedActivities = activities.cachedActivities)

      if (activity.isNotEmpty()) {
        val act = activity.first()
        newActivities.add(act)
        // Get the element that corresponds to the activity in the cache and remove it if it exists
        val sameActivityInCache =
            activities.cachedActivities.firstOrNull {
              it.location.sameLocation(act.location) && it.getName() == act.getName()
            }

        if (sameActivityInCache != null) {
          activities.cachedActivities.remove(sameActivityInCache)
        }
      }
    }

    return newActivities
  }

  /**
   * ********************************************************
   * Schedule and remove activities
   * *********************************************************
   */
  /**
   * Schedules the trip and handles over-scheduling (overtime) by intelligently removing activities.
   *
   * The process is:
   * 1. Run an initial schedule.
   * 2. Remove "ghost" activities (those skipped by the scheduler due to being
   *    unreachable/impossible) and re-optimize the route.
   * 3. If the timeline still overruns (runs too late), remove activities based on the time deficit,
   *    prioritizing non-protected and larger activities.
   * 4. Re-optimize the route and run a final schedule.
   *
   * @param enhancedTripProfile The enhanced trip profile containing the latest preferences and
   *   preferred locations.
   * @param originalOptimizedRoute The current best optimized route.
   * @param activities The [Activities] state object containing all planned, intermediate, and
   *   cached activities.
   * @return The resulting list of [TripElement] representing the scheduled trip.
   */
  open suspend fun scheduleRemove(
      enhancedTripProfile: EnhancedTripProfile,
      originalOptimizedRoute: OrderedRoute,
      activities: Activities
  ): List<TripElement> {
    // Filter out intermediate activities from the activity list to avoid removing them
    val normalActivities =
        activities.allActivities.filter {
          !activities.intermediateActivities.contains(it) &&
              !activities.grandTourActivities.contains(it)
        }

    // 1) Run first scheduling pass
    var currentRoute = originalOptimizedRoute
    var firstSchedule =
        scheduleTrip(
            enhancedTripProfile.tripProfile,
            currentRoute,
            activities.allActivities,
            scheduleParams) {}

    // 2) Compute which activities were missing (The "Ghosts")
    var scheduledActs = extractActivitiesFromSchedule(firstSchedule)
    val missingActivities =
        activities.allActivities.filter { act ->
          scheduledActs.none { scheduled -> activitiesMatch(act, scheduled) }
        }

    // If scheduler skipped activities, remove THEM immediately to fix the dangling routes.
    if (missingActivities.isNotEmpty()) {
      val locationsToRemove = missingActivities.map { it.location }.toSet()

      // Remove the ghosts from your lists
      activities.allActivities.removeAll(missingActivities)
      activities.intermediateActivities.removeAll(missingActivities)
      activities.cachedActivities.addAll(missingActivities)

      // Rebuild the route without the ghosts
      val (cleanedRoute, changedIndexes) = buildRouteAfterRemovals(currentRoute, locationsToRemove)

      // Update currentRoute for the next steps
      try {
        currentRoute =
            routeOptimizer.recomputeOrderedRoute(
                cleanedRoute,
                changedIndexes,
                mode =
                    if (enhancedTripProfile.tripProfile.preferences.contains(
                        Preference.PUBLIC_TRANSPORT))
                        TransportMode.TRAIN
                    else TransportMode.CAR,
                INVALID_DURATION) {}

        // Re-run schedule on the clean route
        firstSchedule =
            scheduleTrip(
                enhancedTripProfile.tripProfile,
                currentRoute,
                activities.allActivities,
                scheduleParams) {}

        // Update scheduled acts list for the next check
        scheduledActs = extractActivitiesFromSchedule(firstSchedule)
      } catch (e: Exception) {
        Log.w("TripAlgorithm", "Recompute after cleanup failed", e)
        return firstSchedule
      }
    }

    // Now checking the clean schedule:
    // If it fits within the timeline, we are done.
    if (dateDifference(firstSchedule.last().endDate, enhancedTripProfile.tripProfile.endDate) >=
        0) {
      return firstSchedule
    }

    // 3) OVERTIME PHASE
    // If we are here, the schedule is clean, but runs too late.
    // Now we calculate deficit based on TIME OVERRUN, not missing activities.

    // Calculate how many seconds we are overtime
    val missingSumSec = missingActivities.sumOf { it.estimatedTime.toDouble().roundToLong() }
    val penaltySec = missingActivities.size * RESCHEDULE_PENALTY_PER_ACTIVITY_SEC.toLong()
    val deficitSeconds = missingSumSec + penaltySec

    // 4) Randomly remove activities
    val rand = Random.Default

    // Only look at activities that are ACTUALLY in the schedule
    val candidateList =
        normalActivities
            .filter { act -> scheduledActs.any { activitiesMatch(it, act) } }
            .toMutableList()

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
    if (activities.intermediateActivities.isNotEmpty()) {
      protectedActivities.addAll(activities.intermediateActivities)
    }

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
      if (chosen in pool) {
        pool.remove(chosen)
      }
      // If a protected activity had to be removed (fallback case), unprotect the cluster
      if (chosen in protectedActivities) {
        protectedActivities.remove(chosen)
      }
    }

    if (toRemove.isEmpty()) {
      // Could not find candidates to remove, return original result
      return firstSchedule
    }

    // 5) Update activityList by removing chosen activities
    val removedLocations = toRemove.map { it.location }.toSet()
    activities.allActivities.removeAll { act -> toRemove.any { rem -> activitiesMatch(act, rem) } }
    activities.intermediateActivities.removeAll { act ->
      toRemove.any { rem -> activitiesMatch(act, rem) }
    }
    // Put the valid activities that we had to remove due to the time to the cache in case we use
    // them later
    activities.cachedActivities.addAll(toRemove)

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
              INVALID_DURATION) {}
        } catch (e: Exception) {
          // Recompute failed — fall back to firstSchedule
          Log.w("TripAlgorithm", "Recompute after removals failed: ${e.message}")
          return firstSchedule
        }

    // 8) Re-run scheduleTrip with recomputed route and the pruned activity list
    val finalSchedule =
        try {
          scheduleTrip(
              enhancedTripProfile.tripProfile,
              finalOptimizedRoute,
              activities.allActivities,
              scheduleParams) {}
        } catch (e: Exception) {
          Log.w("TripAlgorithm", "Scheduling after recompute failed: ${e.message}")
          return firstSchedule
        }

    return finalSchedule
  }

  /**
   * ********************************************************
   * Adding activities to match the users end date
   * *********************************************************
   */

  /**
   * Completes the trip schedule by adding more activities to utilize remaining time, up to the
   * user's end date.
   *
   * The process iteratively tries:
   * 1. Adding activities from the cache (`tryAddingCachedActivities`).
   * 2. Adding activities associated with existing major cities
   *    (`tryFetchingActivitiesForExistingCities`) (Only at index 2).
   * 3. Adding general "city visit" activities for existing preferred locations
   *    (`tryAddingCityActivities`).
   * 4. Adding a brand new major Swiss city and its associated activities (`tryAddingCity`).
   *
   * The loop runs until the schedule end date matches the desired end date or a maximum number of
   * iterations is reached.
   *
   * @param originalSchedule The schedule generated by the last `scheduleRemove` call.
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @param progressStart The absolute progress value where this phase starts.
   * @param progressEnd The absolute progress value where this phase ends.
   * @param onProgress Callback to update progress within [progressStart, progressEnd].
   * @return The final list of [TripElement] representing the completed schedule.
   */
  private suspend fun completeSchedule(
      originalSchedule: List<TripElement>,
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      progressStart: Float = 0f,
      progressEnd: Float = 0f,
      onProgress: (Float) -> Unit = {}
  ): List<TripElement> {
    var index = 0
    val maxIndex =
        min(
            MAX_INDEX,
            dateDifference(originalSchedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
                .toInt())
    var finalSchedule = originalSchedule
    val endDate = enhancedTripProfile.tripProfile.endDate

    // Calculate progress step per iteration
    val progressRange = progressEnd - progressStart
    // Use MAX_INDEX as denominator because that's the upper bound of loops
    val progressStep = if (maxIndex > 0) progressRange / maxIndex else 0f

    while (index < maxIndex) {
      val iterationStart = progressStart + (index * progressStep)
      val iterationEnd = iterationStart + progressStep

      finalSchedule = tryAddingCachedActivities(enhancedTripProfile, activities, finalSchedule)

      if (sameDate(endDate, finalSchedule.last().endDate)) {
        break
      }

      if (index == 2) {
        finalSchedule =
            tryFetchingActivitiesForExistingCities(enhancedTripProfile, activities, finalSchedule)
        if (sameDate(endDate, finalSchedule.last().endDate)) {
          break
        }
      }

      finalSchedule = tryAddingCityActivities(enhancedTripProfile, activities, finalSchedule)

      if (sameDate(endDate, finalSchedule.last().endDate)) {
        break
      }

      finalSchedule = tryAddingCity(enhancedTripProfile, activities, finalSchedule)

      if (sameDate(endDate, finalSchedule.last().endDate)) {
        break
      }

      index++
      // Report completed step
      onProgress(iterationEnd)
    }

    return finalSchedule
  }

  /**
   * Iteratively attempts to add activities from the `cachedActivities` list to fill time.
   *
   * It uses a variation of the knapsack algorithm to select the best combination of cached
   * activities to fit the time remaining until the trip's end date, prioritizing activities near
   * existing preferred locations.
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @param originalSchedule The current trip schedule.
   * @return The updated list of [TripElement] after optimizing and scheduling with any added cached
   *   activities.
   */
  open suspend fun tryAddingCachedActivities(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      originalSchedule: List<TripElement>,
  ): List<TripElement> {
    var added = true
    var index = 0
    val maxIndex =
        min(
            MAX_INDEX_CACHED_ACTIVITIES,
            dateDifference(originalSchedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
                .toInt())
    var schedule = originalSchedule
    while (added && index < maxIndex) {
      val daysDiff =
          dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
      val totalTimeNeededHours =
          if (daysDiff > 0) {
            (daysDiff * ACTIVITY_TIME_PER_DAY_HOURS).toDouble()
          } else {
            // If same day, calculate remaining active hours (e.g. gap between now and 8pm)
            // But we need to be careful not to add if we are already late.
            val hours =
                hoursDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate)
            if (hours > 0) hours.toDouble() else 0.0
          }
      if (totalTimeNeededHours > 0) {
        added = addCachedActivity(enhancedTripProfile, activities, totalTimeNeededHours)
      }
      schedule = optimizeAndSchedule(enhancedTripProfile, activities)
      index++
    }
    return schedule
  }

  /**
   * Attempts to add generic "city visit" activities associated with existing preferred locations
   * that map to major Swiss cities.
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @param originalSchedule The current trip schedule.
   * @return The updated list of [TripElement] after attempting to add and reschedule.
   */
  private suspend fun tryAddingCityActivities(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      originalSchedule: List<TripElement>,
  ): List<TripElement> {
    var added = true
    var index = 0
    val maxIndex = min(MAX_INDEX * 2, enhancedTripProfile.newPreferredLocations.size)
    var schedule = originalSchedule

    while (added && index < maxIndex) {
      val numberOfDays =
          dateDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate) *
              ACTIVITY_TIME_PER_DAY_HOURS

      // If overtime, stop.
      if (numberOfDays.toInt() <= 0) break

      val totalTimeNeededHours =
          (if (numberOfDays > 1) {
            numberOfDays * ACTIVITY_TIME_PER_DAY_HOURS
          } else {
            min(
                3,
                hoursDifference(schedule.last().endDate, enhancedTripProfile.tripProfile.endDate) -
                    2)
          }) * 3600

      if (totalTimeNeededHours > 0) {
        added = addCityActivity(enhancedTripProfile, activities, totalTimeNeededHours)
        if (added) {
          schedule = optimizeOnly(enhancedTripProfile, activities)
          index++
        }
      } else {
        break
      }
    }
    return schedule
  }

  /**
   * Attempts to add a completely new major Swiss city (that is not already near a preferred
   * location) along with a batch of associated activities to the trip.
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @param originalSchedule The current trip schedule.
   * @return The updated list of [TripElement] after attempting to add the new city and reschedule.
   */
  private suspend fun tryAddingCity(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      originalSchedule: List<TripElement>,
  ): List<TripElement> {
    var schedule = originalSchedule

    val added = addCity(enhancedTripProfile, activities)

    if (added) {
      schedule = optimizeOnly(enhancedTripProfile, activities)
    }

    return schedule
  }

  /**
   * Fetches real activities for cities that were determined to be associated with one of the user's
   * preferred locations, overriding user preferences temporarily if necessary to find activities.
   * This step is only run once during the `completeSchedule` process (at index 2).
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @param originalSchedule The current trip schedule.
   * @return The updated list of [TripElement] after fetching and rescheduling.
   */
  private suspend fun tryFetchingActivitiesForExistingCities(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      originalSchedule: List<TripElement>,
  ): List<TripElement> {
    var addedAny = false
    val currentActivityNames = activities.allActivities.map { it.getName() }

    activitySelector.updatePreferences(allBasicPreferences)

    val sortedCities =
        enhancedTripProfile.newPreferredLocations
            .mapNotNull { location ->
              swissMajorCities.firstOrNull { cityConfig ->
                location.haversineDistanceTo(cityConfig.location) <= RADIUS_CITIES_TO_ASSOCIATE_KM
              }
            }
            .distinct()
            .sortedByDescending { it.maxDays }

    var citiesChecked = 0
    for (associatedCity in sortedCities) {
      if (citiesChecked >= MAX_INDEX_FETCH_ACTIVITIES) break

      val newActivities =
          activitySelector.getActivitiesNearWithPreferences(
              coords = associatedCity.location.coordinate,
              radius = RADIUS_NEW_ACTIVITY_M,
              limit = NUMBER_OF_ACTIVITY_NEW_CITY,
              activityBlackList = currentActivityNames,
              cachedActivities = activities.cachedActivities)

      if (newActivities.isNotEmpty()) {
        activities.allActivities.addAll(newActivities)
        addedAny = true
      }
      citiesChecked++
    }

    activitySelector.updatePreferences(enhancedTripProfile.tripProfile.preferences)

    if (addedAny) {
      return optimizeOnly(enhancedTripProfile, activities)
    }

    return originalSchedule
  }

  /**
   * ***
   * Types of activities to add
   * ****
   */

  /**
   * Selects a set of activities from the `cachedActivities` list to add back to the trip using a
   * dynamic programming (knapsack-like) approach to fit within the `totalTimeNeededHours`. Only
   * activities near existing preferred locations are considered.
   *
   * @param enhancedTripProfile The enhanced trip profile to check preferred locations.
   * @param activities The mutable [Activities] state object containing the cache and all
   *   activities.
   * @param totalTimeNeededHours The maximum time (in hours) available to fill with activities.
   * @return `true` if at least one activity was added, `false` otherwise.
   */
  open fun addCachedActivity(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      totalTimeNeededHours: Double,
  ): Boolean {
    if (activities.cachedActivities.isEmpty()) return false

    // 100 hours * 60 = 6000 minutes. This is a very safe size for an IntArray.
    val limitMinutes = (min(100.0, totalTimeNeededHours) * 60).toInt()

    // If the time needed is negligible (e.g. < 1 minute), don't add anything.
    if (limitMinutes <= 0) return false

    val validCandidates = mutableListOf<Activity>()

    getValidCandidatesCachedActivities(
        enhancedTripProfile, validCandidates, activities, limitMinutes)

    if (validCandidates.isEmpty()) return false

    val activitiesCandidates = validCandidates.toList()
    val n = activitiesCandidates.size
    val w = limitMinutes

    // DP table: indices represent MINUTES.
    val dp = IntArray(w + 1) { -1 }
    dp[0] = 0
    val parent = IntArray(w + 1) { -1 }

    // 3. Fill the DP table
    for (i in 0 until n) {
      val activity = activitiesCandidates[i]
      // Note: Integer division implies floor. 15min 30s -> 15min.
      val weight = activity.estimatedTime / 60

      // Skip activities effectively 0 minutes to prevent infinite loops (though filtered above)
      if (weight <= 0) continue

      for (j in w downTo weight) {
        if (dp[j - weight] != -1) {
          // Since value == weight (we just want to fill time),
          // the new "value" is simply the time achieved.
          val newTime = dp[j - weight] + weight

          if (newTime > dp[j]) {
            dp[j] = newTime
            parent[j] = i
          }
        }
      }
    }

    // 4. Reconstruction
    var bestTimeIndex = -1

    // Iterate backwards to find the largest time bucket that was reachable
    for (j in w downTo 0) {
      if (dp[j] != -1) {
        bestTimeIndex = j
        break
      }
    }

    val toAdd = mutableListOf<Activity>()

    if (bestTimeIndex != -1) {
      var currentWeight = bestTimeIndex
      while (currentWeight > 0) {
        val activityIndex = parent[currentWeight]
        if (activityIndex == -1) break

        val activity = activitiesCandidates[activityIndex]
        toAdd.add(activity)

        // Fix 4: Must use the exact same weight calculation as the DP loop
        val weight = activity.estimatedTime / 60
        currentWeight -= weight
      }
    }

    if (toAdd.isNotEmpty()) {
      activities.allActivities.addAll(toAdd)
      activities.cachedActivities.removeAll(toAdd)
      return true
    }

    return false
  }

  /**
   * Adds a generic "city visit" activity to a preferred location that is near a major Swiss city.
   *
   * The function ensures that the number of "city visit" activities for a given city location does
   * not exceed the city's configured `maxDays`.
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @param activityTime The desired estimated time for the activity (in seconds).
   * @return `true` if a new city activity was added, `false` otherwise.
   */
  private fun addCityActivity(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
      activityTime: Long
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

        // If we have a city that has a visit time of less than 1 make it at most 8 hours time the
        // ratio of days (if the ratio is 0.5 then we have activities at most 4 hours)
        val trueActivityTime =
            if (maxDays < 1) {
              min((8 * maxDays * 3600).toLong(), activityTime)
            } else {
              min(8 * 3600, activityTime)
            }
        // Create new activity
        val newActivity = cityActivity(cityLoc, trueActivityTime)

        val existingCount =
            activities.allActivities.count { it.location.sameLocation(newActivity.location) }

        if (existingCount == 0) {
          activities.allActivities.add(newActivity)
          return true
        }

        // Add to tempCandidates only if it doesn't already exist more than allowed
        if (existingCount < maxDays.toInt()) {
          tempCandidates.add(newActivity)
        }
      }
    }

    if (tempCandidates.isNotEmpty()) {
      activities.allActivities.add(tempCandidates.first())
      return true
    }

    return false
  }

  /**
   * Finds a suitable new city to add to the trip and fetches real activities for it.
   *
   * It prioritizes cities that are:
   * 1. Not already near a preferred location.
   * 2. Closer to the existing itinerary.
   * 3. Larger (higher `maxDays`) within a certain distance threshold.
   *
   * If initial fetching fails to find enough activities with user preferences, it attempts to
   * search with all basic preferences.
   *
   * @param enhancedTripProfile The enhanced trip profile, where the new city location will be added
   *   to `newPreferredLocations`.
   * @param activities The mutable [Activities] state object, where new activities will be added.
   * @return `true` if the new city and its activities were successfully added, `false` otherwise.
   */
  private suspend fun addCity(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
  ): Boolean {
    val validCity = findValidCity(enhancedTripProfile)

    val addedActivities =
        activitySelector.getActivitiesNearWithPreferences(
            coords = validCity.location.coordinate,
            limit = NUMBER_OF_ACTIVITY_NEW_CITY,
            activityBlackList = activities.allActivities.map { it.getName() },
            cachedActivities = activities.cachedActivities)
    if (addedActivities.isNotEmpty() && addedActivities.size >= NUMBER_OF_ACTIVITY_NEW_CITY) {
      activities.allActivities.addAll(addedActivities)
      enhancedTripProfile.newPreferredLocations.add(validCity.location)
      return true
    } else {
      // Retry with all the preferences
      activitySelector.updatePreferences(allBasicPreferences)
      val addedActivitiesPrefs =
          activitySelector.getActivitiesNearWithPreferences(
              coords = validCity.location.coordinate,
              limit = NUMBER_OF_ACTIVITY_NEW_CITY - addedActivities.size,
              activityBlackList = activities.allActivities.map { it.getName() },
              cachedActivities = activities.cachedActivities)
      // Put the old preferences back
      activitySelector.updatePreferences(enhancedTripProfile.tripProfile.preferences)
      if (addedActivitiesPrefs.isNotEmpty()) {
        activities.allActivities.addAll(addedActivitiesPrefs)
        enhancedTripProfile.newPreferredLocations.add(validCity.location)
        return true
      }
    }
    return false
  }

  /**
   * ********************************************************
   * Utils functions
   * *********************************************************
   */

  /**
   * ***
   * Misc
   * ****
   */

  /**
   * Extension function to generate a random double in a closed range [start, endInclusive].
   *
   * @param rng The [Random] number generator instance to use (defaults to [Random.Default]).
   * @return A random [Double] value within the specified range.
   */
  private fun ClosedFloatingPointRange<Double>.random(rng: Random = Random.Default): Double {
    // Guarantee closed range by adding the minimal epsilon
    return rng.nextDouble(start, endInclusive + Double.MIN_VALUE)
  }

  /**
   * Filters the `cachedActivities` list in place (`iterator.remove()`) and populates
   * `validCandidates` with activities that are:
   * 1. Near one of the current preferred locations (within `RADIUS_CACHED_ACTIVITIES_KM`).
   * 2. Not already present in `allActivities`.
   * 3. Have a positive duration that fits within `limitMinutes`.
   *
   * Activities that are too far from preferred locations are permanently removed from the cache.
   *
   * @param enhancedTripProfile The enhanced trip profile to check against preferred locations.
   * @param validCandidates A mutable list to be populated with activities that can be added.
   * @param activities The mutable [Activities] state object to read from and modify the cache.
   * @param limitMinutes The maximum duration an activity can have to be considered (in minutes).
   */
  private fun getValidCandidatesCachedActivities(
      enhancedTripProfile: EnhancedTripProfile,
      validCandidates: MutableList<Activity>,
      activities: Activities,
      limitMinutes: Int
  ) {
    val iterator = activities.cachedActivities.iterator()
    while (iterator.hasNext()) {
      val activity = iterator.next()

      val isNearPreferred =
          enhancedTripProfile.newPreferredLocations.any { pref ->
            activity.location.coordinate.haversineDistanceTo(pref.coordinate) <=
                RADIUS_CACHED_ACTIVITIES_KM
          }

      if (!isNearPreferred) {
        iterator.remove()
      } else {
        val alreadyInTrip =
            activities.allActivities.any { it.location.sameLocation(activity.location) }

        // Fix 2: Ensure activity has a positive duration and fits within the total time limit
        // We convert estimatedTime (seconds) to minutes for comparison.
        val durationMinutes = activity.estimatedTime / 60
        if (!alreadyInTrip && durationMinutes > 0 && durationMinutes <= limitMinutes) {
          validCandidates.add(activity)
        }
      }
    }
  }

  /**
   * Finds the best new [CityConfig] to add to the trip.
   *
   * The selection logic prioritizes cities that:
   * 1. Are not already covered by existing preferred locations.
   * 2. Are closest to the existing preferred locations.
   * 3. Among the competitive candidates (closest city + buffer distance), prioritizes larger cities
   *    (higher `maxDays`).
   *
   * @param enhancedTripProfile The enhanced trip profile containing the current preferred
   *   locations.
   * @return The best [CityConfig] candidate to add.
   */
  private fun findValidCity(enhancedTripProfile: EnhancedTripProfile): CityConfig {
    // 1. Filter out cities that are already visited/near preferred locations
    val candidates =
        swissMajorCities.filter { cityConfig ->
          enhancedTripProfile.newPreferredLocations.none { location ->
            location.coordinate.haversineDistanceTo(cityConfig.location.coordinate) <=
                cityConfig.radius
          }
        }

    // 2. Calculate the minimum distance from each candidate to the current trip
    val withDistance =
        candidates.map { cityConfig ->
          val dist =
              enhancedTripProfile.newPreferredLocations.minOfOrNull { location ->
                location.coordinate.haversineDistanceTo(cityConfig.location.coordinate)
              } ?: 0.0 // Fallback if no locations exist (unlikely)
          cityConfig to dist
        }

    // 3. Find the absolute best distance among all candidates
    val bestDistance = withDistance.minOfOrNull { it.second } ?: 0.0
    val threshold = bestDistance + ADDED_DISTANCE_CITY_KM

    // 4. This list is sorted by taking the bestDistance (the city closest to one of the
    // point in the preferredLocations) and comparing it with other cities.
    // What this does is if a city has more DaysMax (which represents the number of city
    // activities for this city at most), we allow to go a bit further since it is a
    // bigger town so that we make sure we find activities.
    val validCities =
        withDistance
            .sortedWith { (cityA, distA), (cityB, distB) ->
              val aIsCompetitive = distA <= threshold
              val bIsCompetitive = distB <= threshold

              when {
                // Case 1: Both are within the "competitive" range (best distance + 25km)
                // -> Prioritize the "bigger" city (higher maxDays)
                aIsCompetitive && bIsCompetitive -> cityB.maxDays.compareTo(cityA.maxDays)

                // Case 2: Only A is within range -> A comes first
                aIsCompetitive -> -1

                // Case 3: Only B is within range -> B comes first
                bIsCompetitive -> 1

                // Case 4: Neither is in range -> Prioritize the one that is closer
                else -> distA.compareTo(distB)
              }
            }
            .map { it.first }

    val validCity = validCities.first()
    return validCity
  }

  /**
   * ***
   * Activities creation
   * ****
   */
  /**
   * Creates a generic "city visit" [Activity] for a given location and estimated time.
   *
   * @param location The geographical location of the city.
   * @param estimatedTimeSeconds The estimated duration of the visit in seconds.
   * @return A new [Activity] representing a city visit.
   */
  private fun cityActivity(location: Location, estimatedTimeSeconds: Long): Activity {
    val name = context.getString(R.string.city_visit_name, location.name)
    return Activity(
        location = location.copy(name = name),
        estimatedTime = estimatedTimeSeconds.toInt(),
        startDate = Timestamp.now(), // placeholder
        endDate = Timestamp.now(), // placeholder
        description = context.getString(R.string.city_activity_description),
        imageUrls = emptyList(),
    )
  }

  /**
   * Creates a specific "Grand Tour" [Activity] for a given location.
   *
   * This is used for generating simple activities when `isRandomTrip` is true.
   *
   * @param location The geographical location for the Grand Tour stop.
   * @return A new [Activity] representing a Grand Tour stop.
   */
  private fun grandTourActivity(
      location: Location,
  ): Activity {
    val name = context.getString(R.string.grand_tour_activity_name, location.name)
    return Activity(
        location = location.copy(name = name),
        estimatedTime = GRAND_TOUR_ACTIVITY_DURATION_SEC.toInt(),
        startDate = Timestamp.now(), // placeholder
        endDate = Timestamp.now(), // placeholder
        description = context.getString(R.string.grand_tour_activity_description, location.name),
        imageUrls = emptyList(),
    )
  }

  /**
   * ***
   * Activities organisation
   * ****
   */

  /**
   * Extracts all scheduled activities from a list of [TripElement]s.
   *
   * @param schedule The list of [TripElement]s (route segments and activities).
   * @return A list of [Activity] objects that were successfully scheduled.
   */
  private fun extractActivitiesFromSchedule(schedule: List<TripElement>): List<Activity> {
    return schedule.mapNotNull {
      when (it) {
        is TripElement.TripActivity -> it.activity
        else -> null
      }
    }
  }

  /**
   * Checks if two activities match based on their estimated time and location identity.
   *
   * Note: This is a conservative check, ideal if the Activity class lacks a unique ID.
   *
   * @param a The first [Activity].
   * @param b The second [Activity].
   * @return True if both activities have the same estimated time and location coordinates/name.
   */
  private fun activitiesMatch(a: Activity, b: Activity): Boolean {
    return a.estimatedTime == b.estimatedTime && a.location.sameLocation(b.location)
  }

  /**
   * ***
   * Dates
   * ****
   */

  /**
   * Checks if two [Timestamp]s represent the same date, regardless of the time of day.
   *
   * @param date1 The first Timestamp to compare.
   * @param date2 The second Timestamp to compare.
   * @param zone The time zone to use for date conversion (defaults to system default).
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
   * Computes the difference between two timestamps in the specified [ChronoUnit].
   *
   * @param date1 The starting timestamp.
   * @param date2 The ending timestamp.
   * @param zone The time zone to consider for date conversion (defaults to system default).
   * @param unit The unit in which to return the difference (defaults to DAYS).
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

  /**
   * Computes the difference in hours between two timestamps, preserving time information.
   *
   * @param date1 The starting timestamp.
   * @param date2 The ending timestamp.
   * @param zone The time zone to consider (defaults to system default).
   * @return The difference in hours between date1 and date2. Positive if date2 is after date1.
   */
  private fun hoursDifference(
      date1: Timestamp,
      date2: Timestamp,
      zone: ZoneId = ZoneId.systemDefault()
  ): Long {
    // Convert to ZonedDateTime to preserve time information
    val d1 = date1.toDate().toInstant().atZone(zone)
    val d2 = date2.toDate().toInstant().atZone(zone)

    return ChronoUnit.HOURS.between(d1, d2)
  }

  /**
   * ***
   * Routes
   * ****
   */

  /**
   * Builds a new [OrderedRoute] by removing specified activity locations and invalidating the
   * adjacent segment durations.
   *
   * The invalidated segments are marked with [INVALID_DURATION] so the route optimizer can
   * recompute them based on the new route path.
   *
   * @param optimizedRoute The original [OrderedRoute].
   * @param toRemoveLocations A set of [Location]s corresponding to the activities to remove.
   * @return A [Pair] containing:
   * - The `newOrderedRoute` without the removed locations and with invalid durations.
   * - A list of `changedIndexes` (indices in the new `segmentDuration` list that need
   *   recomputation).
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
   * ***
   * Schedule
   * ****
   */
  /**
   * Re-optimizes the full route with all current activities and then runs the `scheduleRemove`
   * function.
   *
   * This utility function encapsulates the "optimize then schedule" flow.
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @return The resulting list of [TripElement] after optimization and scheduling/pruning.
   */
  private suspend fun optimizeAndSchedule(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities
  ): List<TripElement> {
    val publicTransportMode =
        enhancedTripProfile.tripProfile.preferences.contains(Preference.PUBLIC_TRANSPORT)
    val orderedRoute =
        routeOptimizer.optimize(
            enhancedTripProfile.tripProfile.arrivalLocation!!,
            enhancedTripProfile.tripProfile.departureLocation!!,
            activities.allActivities.map { it.location },
            activities.allActivities,
            if (publicTransportMode) TransportMode.TRAIN else TransportMode.CAR) {}

    val schedule = scheduleRemove(enhancedTripProfile, orderedRoute, activities)
    return schedule
  }

  /**
   * Re-optimizes the full route with all current activities and then runs a standard, single
   * scheduling pass (`scheduleTrip`).
   *
   * This is used when adding activities to fill time, where the intent is just to schedule the new,
   * longer list of activities without the complex removal logic of `scheduleRemove`.
   *
   * @param enhancedTripProfile The enhanced trip profile.
   * @param activities The mutable [Activities] state object.
   * @return The resulting list of [TripElement] after optimization and scheduling.
   */
  private suspend fun optimizeOnly(
      enhancedTripProfile: EnhancedTripProfile,
      activities: Activities,
  ): List<TripElement> {
    val publicTransportMode =
        enhancedTripProfile.tripProfile.preferences.contains(Preference.PUBLIC_TRANSPORT)

    val orderedRoute =
        routeOptimizer.optimize(
            enhancedTripProfile.tripProfile.arrivalLocation!!,
            enhancedTripProfile.tripProfile.departureLocation!!,
            activities.allActivities.map { it.location },
            activities.allActivities,
            if (publicTransportMode) TransportMode.TRAIN else TransportMode.CAR) {}

    return scheduleTrip(
        enhancedTripProfile.tripProfile, orderedRoute, activities.allActivities, scheduleParams) {}
  }
}
