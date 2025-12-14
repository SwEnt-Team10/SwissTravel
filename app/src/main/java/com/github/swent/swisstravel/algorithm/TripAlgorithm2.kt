package com.github.swent.swisstravel.algorithm

import android.content.Context
import com.github.swent.swisstravel.R
import com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal
import com.github.swent.swisstravel.algorithm.orderlocations.OrderedRoute
import com.github.swent.swisstravel.algorithm.orderlocationsv2.DurationMatrixHybrid
import com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.algorithm.tripschedule.ScheduleParams
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
import kotlin.math.min
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
const val MAX_BATCH_ADDED_TIME_HOURS = 6.0
const val BATCH_ADD_PENALTY_MINUTES = 15
const val GRAND_TOUR_ACTIVITY_DURATION_SEC = 0.5 * 3600 // 30 min
const val ADDED_DISTANCE_CITY_KM = 20
const val NUMBER_OF_ACTIVITY_NEW_CITY = 3
const val MAX_INDEX = 6
const val ACTIVITY_TIME_PER_DAY_HOURS = 8
const val MAX_INDEX_FETCH_ACTIVITIES = 4
const val RADIUS_CITIES_TO_ASSOCIATE_KM = 15

open class TripAlgorithm2 (
    private val activitySelector: SelectActivities,
   private val routeOptimizer: ProgressiveRouteOptimizer,
   private val context: Context,
   private val scheduleParams: ScheduleParams = ScheduleParams()){

    /***********************************************************
     * Init
     ***********************************************************/

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

    /***********************************************************
     * Data class
     ***********************************************************/

    /**
     * Configuration for a major city.
     *
     * @param location The location of the city.
     * @param radius The radius in km to consider an activity as being in/near the city.
     * @param maxDays The maximum number of days/cityActivities to schedule for this city.
     */
    private data class CityConfig(val location: Location, val radius: Int, val maxDays: Double)

    /**
     * This data class is used to store a tripProfile and a copy of its preferredLocations while also
     * being able to add new locations without modifying the tripProfile meaning This is useful when we
     * are adding new cities in the trip
     *
     * @param tripProfile The original tripProfile.
     * @param newPreferredLocations A mutable list of new preferred locations.
     */
    data class EnhancedTripProfile(
        val tripProfile: TripProfile,
        val newPreferredLocations: MutableList<Location>
    )

    /***********************************************************
     * Progression data class
     ***********************************************************/

    // TODO

    /***********************************************************
     * Utils val
     ***********************************************************/

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
     * The list of all the grand tour locations in the array.grand_tour
     */
    private val grandTourList =
        context.resources.getStringArray(R.array.grand_tour).map { line ->
            val parts = line.split(";")
            Location(
                coordinate = Coordinate(parts[1].toDouble(), parts[2].toDouble()),
                name = parts[0],
                imageUrl = "")
        }

    private val allBasicPreferences = PreferenceCategories.environmentPreferences + PreferenceCategories.activityTypePreferences

    /***********************************************************
     * Main computation
     ***********************************************************/

    // TODO organize the steps:
    /**
     * 1. Fetch activities near each locations of the trip
     * 2. If the start and end are the same (or very close) and there were no activities, build an ordered route with nothing and some duration like 1 minute
     * 3. Else optimize the route
     * 4. Pass the OrderedRoute to the scheduler
     * 5. If the schedule is good, finish
     * 6. Else if the schedule has too much in it try to remove the activities closest to the overtime and do it until we have a trip that finishes on the same date
     * 7. Else (If the schedule is not filled) add more activities
     */

    // TODO: Don't forget to manage the randomTrip
    // TODO: Don't forget to add the preferences from the tripSetting to the enhancedTripProfile so that if you change the preference at the start it will be stored in the tripProfile

    /***********************************************************
     * Adding in between activities
     ***********************************************************/

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

    /***********************************************************
     * Schedule and remove activities
     ***********************************************************/



    // TODO: Don't forget to not remove preferredLocations that are in the original tripProfile meaning that we only do so if we have no choice














    /***********************************************************
     * Adding activities to match the users end date
     ***********************************************************/

    // TODO:
    /**
     * 1. Add cachedActivities while it is not empty
     * 2. If the end date is correct end
     * 3. Else try adding other activities
     * 4. Begin by trying to add city activities depending on the time left (Add a function that calculate the time in hours between two dates)
     * 5. If adding all the city activities were not enough
     * 6. If the end date is not correct and we are at our first iteration, go to each preferred locations and associate them with a major city if it is in a 15km radius
     * 7. If there are some cities associated, fetch activities in the city with all the preferences
     * 8. If it was not enough we expand to a new city
     * 9. If we don't find at least 3 activities with the preferences of the user activates all the activities for the next pull
     * 10. If it is still not enough loop from the beginning again
     * NOTE: the loop will be done 6 times at most as we will add major swiss cities each round it should be enough
     */

    private suspend fun completeSchedule(
        originalSchedule: List<TripElement>,
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
        cachedActivities: MutableList<Activity>
    ): List<TripElement> {
        var index = 0
        val maxIndex = min(MAX_INDEX, dateDifference(enhancedTripProfile.tripProfile.endDate, originalSchedule.last().endDate).toInt())
        var finalSchedule = originalSchedule
        val endDate = enhancedTripProfile.tripProfile.endDate

        while (index < maxIndex){
            finalSchedule = tryAddingCachedActivities(enhancedTripProfile, activityList, finalSchedule, cachedActivities)

            if (sameDate(endDate, finalSchedule.last().endDate)){
                break
            }

            finalSchedule = tryAddingCityActivities(enhancedTripProfile, activityList, finalSchedule, cachedActivities)

            if (sameDate(endDate, finalSchedule.last().endDate)){
                break
            }

            if (index == 0) {
                finalSchedule = tryFetchingActivitiesForExistingCities(enhancedTripProfile, activityList, finalSchedule, cachedActivities)
                if (sameDate(endDate, finalSchedule.last().endDate)){
                    break
                }
            }

            finalSchedule = tryAddingCity(enhancedTripProfile, activityList, finalSchedule, cachedActivities)

            if (sameDate(endDate, finalSchedule.last().endDate)){
                break
            }

            index++
        }

        return finalSchedule
    }

    private suspend fun tryAddingCachedActivities(
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
        originalSchedule: List<TripElement>,
        cachedActivities: MutableList<Activity>
    ): List<TripElement>{
        var added = true
        var index = 0
        val maxIndex = min(MAX_INDEX, dateDifference(enhancedTripProfile.tripProfile.endDate, originalSchedule.last().endDate).toInt())
        var schedule = originalSchedule
        while(added && index < maxIndex) {
            val totalTimeNeededHours = dateDifference(enhancedTripProfile.tripProfile.endDate, schedule.last().endDate) * ACTIVITY_TIME_PER_DAY_HOURS
            if (totalTimeNeededHours > 0) {
                added = addCachedActivity(
                    enhancedTripProfile,
                    activityList,
                    cachedActivities,
                    totalTimeNeededHours.toDouble()
                )
            }
            schedule = optimizeAndSchedule(enhancedTripProfile, activityList, cachedActivities)
            index++
        }

        return schedule
    }

    private suspend fun tryAddingCityActivities(
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
        originalSchedule: List<TripElement>,
        cachedActivities: MutableList<Activity>
    ): List<TripElement> {
        var added = true
        var index = 0
        val maxIndex = min(MAX_INDEX * 2, enhancedTripProfile.newPreferredLocations.size)
        var schedule = originalSchedule
        while(added && index < maxIndex) {
            val numberOfDays = dateDifference(enhancedTripProfile.tripProfile.endDate, schedule.last().endDate) * ACTIVITY_TIME_PER_DAY_HOURS
            if (numberOfDays.toInt() == 0){
                break
            }
            val totalTimeNeededHours = (if (numberOfDays > 1) {
                numberOfDays * ACTIVITY_TIME_PER_DAY_HOURS
            } else if (numberOfDays.toInt() == 0) {
                min(3, hoursDifference(enhancedTripProfile.tripProfile.endDate, schedule.last().endDate) - 2)
            } else {
                // We take -1 because if we enter this it means we have too much activities in the
                // trip so we want to skip the two next if else
                -1
            }) * 3600

            if (totalTimeNeededHours > 0) {
                added = addCityActivity(enhancedTripProfile, activityList, totalTimeNeededHours)
            } else if (totalTimeNeededHours.toInt() == 0) {
                break
            }
            schedule = optimizeAndSchedule(enhancedTripProfile, activityList, cachedActivities)
            index++
            if (sameDate(enhancedTripProfile.tripProfile.endDate, schedule.last().endDate)) {
                break
            }
        }
        return schedule
    }

    private suspend fun tryAddingCity(
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
        originalSchedule: List<TripElement>,
        cachedActivities: MutableList<Activity>
    ): List<TripElement> {
        var schedule = originalSchedule

        val added = addCity(
            enhancedTripProfile,
            activityList,
            cachedActivities
        )

        if (added) {
            schedule = optimizeAndSchedule(enhancedTripProfile, activityList, cachedActivities)
        }

        return schedule
    }

    // Done using AI
    private suspend fun tryFetchingActivitiesForExistingCities(
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
        originalSchedule: List<TripElement>,
        cachedActivities: MutableList<Activity>
    ): List<TripElement> {
        var addedAny = false
        // Create a blacklist to avoid fetching duplicates
        val currentActivityNames = activityList.map { it.getName() }

        // Temporarily broaden preferences to find activities
        activitySelector.updatePreferences(allBasicPreferences)

        // 1. Identify valid major cities, remove duplicates, and sort by size (biggest first)
        val sortedCities = enhancedTripProfile.newPreferredLocations
            .mapNotNull { location ->
                swissMajorCities.firstOrNull { cityConfig ->
                    location.haversineDistanceTo(cityConfig.location) <= RADIUS_CITIES_TO_ASSOCIATE_KM
                }
            }
            .distinct() // Ensure we don't process the same city multiple times
            .sortedByDescending { it.maxDays } // Prioritize "bigger" cities (higher maxDays)

        // 2. Iterate over the sorted cities with a security limit
        var citiesChecked = 0

        for (associatedCity in sortedCities) {
            if (citiesChecked >= MAX_INDEX_FETCH_ACTIVITIES) break

            val newActivities = activitySelector.getActivitiesNearWithPreferences(
                coords = associatedCity.location.coordinate,
                radius = RADIUS_NEW_ACTIVITY_M,
                limit = NUMBER_OF_ACTIVITY_NEW_CITY,
                activityBlackList = currentActivityNames,
                cachedActivities = cachedActivities
            )

            if (newActivities.isNotEmpty()) {
                activityList.addAll(newActivities)
                addedAny = true
            }

            citiesChecked++
        }

        // Restore original preferences
        activitySelector.updatePreferences(enhancedTripProfile.tripProfile.preferences)

        // If we added something, recompute the schedule
        if (addedAny) {
            return optimizeAndSchedule(enhancedTripProfile, activityList, cachedActivities)
        }

        return originalSchedule
    }


    /******
     * Types of activities to add
     ******/
    // TODO:
    /**
     * 1. Add cached activities
     * 2. Add city activity
     * 3. Add a new city to the trip as well as some activities for it
     */

    /**
     * Done by AI
     *
     * @param activityList The mutable list of current activities in the trip.
     * @param cachedActivities The mutable list of backup/cached activities.
     * @return `true` if at least one activity was added, `false` otherwise.
     */
    private fun addCachedActivity(
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
        cachedActivities: MutableList<Activity>,
        totalTimeNeededHours: Double,
    ): Boolean {
        if (cachedActivities.isEmpty()) return false

        // 100 hours * 60 = 6000 minutes. This is a very safe size for an IntArray.
        val limitMinutes = (min(100.0, totalTimeNeededHours) * 60).toInt()

        // If the time needed is negligible (e.g. < 1 minute), don't add anything.
        if (limitMinutes <= 0) return false

        val validCandidates = mutableListOf<Activity>()

        getValidCandidatesCachedActivities(
            enhancedTripProfile,
            validCandidates,
            cachedActivities,
            activityList,
            limitMinutes
        )

        if (validCandidates.isEmpty()) return false

        val activities = validCandidates.toList()
        val n = activities.size
        val w = limitMinutes

        // DP table: indices represent MINUTES.
        val dp = IntArray(w + 1) { -1 }
        dp[0] = 0
        val parent = IntArray(w + 1) { -1 }

        // 3. Fill the DP table
        for (i in 0 until n) {
            val activity = activities[i]
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

                val activity = activities[activityIndex]
                toAdd.add(activity)

                // Fix 4: Must use the exact same weight calculation as the DP loop
                val weight = activity.estimatedTime / 60
                currentWeight -= weight
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
     *
     * @param activityTime time in seconds
     */
    private fun addCityActivity(
        enhancedTripProfile: EnhancedTripProfile,
        activityList: MutableList<Activity>,
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

                // If we have a city that has a visit time of less than 1 make it at most 8 hours time the ratio of days (if the ratio is 0.5 then we have activities at most 4 hours)
                val trueActivityTime = if (maxDays < 1) {
                    min((8 * maxDays * 3600).toLong(), activityTime)
                } else {
                    min(8 * 3600, activityTime)
                }
                // Create new activity
                val newActivity = cityActivity(cityLoc, trueActivityTime)

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

    private suspend fun addCity(
        enhancedTripProfile: EnhancedTripProfile,
        allActivities: MutableList<Activity>,
        cachedActivities: MutableList<Activity>
    ): Boolean {
        val validCity = findValidCity(enhancedTripProfile)

        val addedActivities = activitySelector.getActivitiesNearWithPreferences(
            coords = validCity.location.coordinate,
            limit = NUMBER_OF_ACTIVITY_NEW_CITY,
            activityBlackList = allActivities.map { it.getName() },
            cachedActivities = cachedActivities
        )
        if (addedActivities.isNotEmpty() && addedActivities.size >= NUMBER_OF_ACTIVITY_NEW_CITY){
            allActivities.addAll(addedActivities)
            enhancedTripProfile.newPreferredLocations.add(validCity.location)
            return true
        } else {
            // Retry with all the preferences
            activitySelector.updatePreferences(allBasicPreferences)
            val addedActivitiesPrefs = activitySelector.getActivitiesNearWithPreferences(
                coords = validCity.location.coordinate,
                limit = NUMBER_OF_ACTIVITY_NEW_CITY - addedActivities.size,
                activityBlackList = allActivities.map { it.getName() },
                cachedActivities = cachedActivities
            )
            // Put the old preferences back
            activitySelector.updatePreferences(enhancedTripProfile.tripProfile.preferences)
            if (addedActivitiesPrefs.isNotEmpty()) {
                allActivities.addAll(addedActivitiesPrefs)
                enhancedTripProfile.newPreferredLocations.add(validCity.location)
                return true
            }
        }
        return false
    }

    /***********************************************************
     * Utils functions
     ***********************************************************/

    /******
     * Misc
     ******/

    /** Extension function to generate a random double in a closed range */
    private fun ClosedFloatingPointRange<Double>.random(rng: Random = Random.Default): Double {
        // Guarantee closed range by adding the minimal epsilon
        return rng.nextDouble(start, endInclusive + Double.MIN_VALUE)
    }

    private fun getValidCandidatesCachedActivities(
        enhancedTripProfile: EnhancedTripProfile,
        validCandidates: MutableList<Activity>,
        cachedActivities: MutableList<Activity>,
        activityList: MutableList<Activity>,
        limitMinutes: Int
    ){
        val iterator = cachedActivities.iterator()
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
                val alreadyInTrip = activityList.any { it.location.sameLocation(activity.location) }

                // Fix 2: Ensure activity has a positive duration and fits within the total time limit
                // We convert estimatedTime (seconds) to minutes for comparison.
                val durationMinutes = activity.estimatedTime / 60
                if (!alreadyInTrip && durationMinutes > 0 && durationMinutes <= limitMinutes) {
                    validCandidates.add(activity)
                }
            }
        }

    }

    private fun findValidCity(
        enhancedTripProfile: EnhancedTripProfile
    ): CityConfig{
        // 1. Filter out cities that are already visited/near preferred locations
        val candidates = swissMajorCities.filter { cityConfig ->
            enhancedTripProfile.newPreferredLocations.none { location ->
                location.coordinate.haversineDistanceTo(cityConfig.location.coordinate) <= cityConfig.radius
            }
        }

        // 2. Calculate the minimum distance from each candidate to the current trip
        val withDistance = candidates.map { cityConfig ->
            val dist = enhancedTripProfile.newPreferredLocations.minOfOrNull { location ->
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
        val validCities = withDistance.sortedWith { (cityA, distA), (cityB, distB) ->
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
        }.map { it.first }

        val validCity = validCities.first()
        return validCity
    }

    /******
     * Activities creation
     ******/

    private fun cityActivity(
        location: Location,
        estimatedTimeSeconds: Long
    ): Activity {
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

    private fun grandTourActivity(
        location: Location,
    ): Activity{
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


    /******
     * Activities organisation
     ******/

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

    /******
     * Dates
     ******/

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

    /**
     * Computes the difference in hours between two timestamps.
     *
     * @param date1 The first timestamp.
     * @param date2 The second timestamp.
     * @param zone The time zone to consider (default system zone).
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

    /******
     * Routes
     ******/

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

    /******
     * Schedule
     ******/

    private suspend fun optimizeAndSchedule(
        enhancedTripProfile: EnhancedTripProfile,
        allActivities: MutableList<Activity>,
        cachedActivities: MutableList<Activity>
    ): List<TripElement> {
        val publicTransportMode = enhancedTripProfile.tripProfile.preferences.contains(Preference.PUBLIC_TRANSPORT)
        routeOptimizer.optimize(
            enhancedTripProfile.tripProfile.arrivalLocation!!,
            enhancedTripProfile.tripProfile.departureLocation!!,
            enhancedTripProfile.newPreferredLocations + allActivities.map { it.location },
            allActivities,
            if (publicTransportMode) TransportMode.TRAIN else TransportMode.CAR
            ) {}

        // TODO: Reschedule
        val schedule = TODO()
        return schedule
    }
}