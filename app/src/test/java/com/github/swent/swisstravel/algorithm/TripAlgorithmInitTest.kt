package com.github.swent.swisstravel.algorithm

import android.content.Context
import com.github.swent.swisstravel.algorithm.selectactivities.SelectActivities
import com.github.swent.swisstravel.model.trip.activity.ActivityRepository
import com.github.swent.swisstravel.model.user.Preference
import com.github.swent.swisstravel.model.user.PreferenceCategories
import com.github.swent.swisstravel.ui.tripcreation.TripSettings
import com.github.swent.swisstravel.ui.tripcreation.TripTravelers
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// This file was made by AI
class TripAlgorithmInitTest {

    private lateinit var context: Context
    private lateinit var repository: ActivityRepository

    @Before
    fun setup() {
        // Relaxed mocks to avoid NullPointerExceptions on method calls
        context = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        // Mock constructors of dependencies instantiated inside init()
        // This prevents them from executing real logic or crashing due to the mocked Context
        mockkConstructor(com.github.swent.swisstravel.algorithm.cache.DurationCacheLocal::class)
        mockkConstructor(com.github.swent.swisstravel.algorithm.orderlocationsv2.DurationMatrixHybrid::class)
        mockkConstructor(com.github.swent.swisstravel.algorithm.orderlocationsv2.ProgressiveRouteOptimizer::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init adds CHILDREN_FRIENDLY preference when children present`() {
        // Given: 1 child
        val settings = TripSettings(
            travelers = TripTravelers(adults = 2, children = 1),
            preferences = emptyList()
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then
        assertTrue(prefs.contains(Preference.CHILDREN_FRIENDLY))
    }

    @Test
    fun `init adds INDIVIDUAL preference when single adult and no children`() {
        // Given: 1 adult, 0 children
        val settings = TripSettings(
            travelers = TripTravelers(adults = 1, children = 0),
            preferences = emptyList()
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then
        assertTrue(prefs.contains(Preference.INDIVIDUAL))
    }

    @Test
    fun `init adds GROUP preference when 3+ adults and no children`() {
        // Given: 3 adults, 0 children
        val settings = TripSettings(
            travelers = TripTravelers(adults = 3, children = 0),
            preferences = emptyList()
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then
        assertTrue(prefs.contains(Preference.GROUP))
    }

    @Test
    fun `init adds nothing specific for 2 adults (neutral case)`() {
        // Given: 2 adults (Couples/Friends - neutral case)
        val settings = TripSettings(
            travelers = TripTravelers(adults = 2, children = 0),
            preferences = listOf(Preference.MUSEUMS, Preference.URBAN) // Provide basic prefs to avoid auto-fill logic
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then
        assertFalse(prefs.contains(Preference.INDIVIDUAL))
        assertFalse(prefs.contains(Preference.GROUP))
        assertFalse(prefs.contains(Preference.CHILDREN_FRIENDLY))
    }

    @Test
    fun `init fills all preferences when input is empty`() {
        // Given: No preferences provided
        val settings = TripSettings(
            travelers = TripTravelers(adults = 2, children = 0),
            preferences = emptyList()
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then: It should populate defaults for both Activity and Environment
        assertTrue(prefs.containsAll(PreferenceCategories.activityTypePreferences))
        assertTrue(prefs.containsAll(PreferenceCategories.environmentPreferences))
    }

    @Test
    fun `init fills missing environment preferences when only activity type is present`() {
        // Given: Only an Activity Type (MUSEUMS) is selected
        val settings = TripSettings(
            travelers = TripTravelers(adults = 2, children = 0),
            preferences = listOf(Preference.MUSEUMS)
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then
        assertTrue(prefs.contains(Preference.MUSEUMS))
        // Should fill Environments because none were present
        assertTrue(prefs.containsAll(PreferenceCategories.environmentPreferences))
        // Should NOT fill all Activity Types (we already had one)
        assertFalse(prefs.contains(Preference.SPORTS))
    }

    @Test
    fun `init fills missing activity type preferences when only environment is present`() {
        // Given: Only an Environment (LAKES) is selected
        val settings = TripSettings(
            travelers = TripTravelers(adults = 2, children = 0),
            preferences = listOf(Preference.URBAN)
        )

        // When
        val algo = TripAlgorithm.init(settings, repository, context)
        val prefs = getPreferencesFromAlgorithm(algo)

        // Then
        assertTrue(prefs.contains(Preference.URBAN))
        // Should fill Activity Types
        assertTrue(prefs.containsAll(PreferenceCategories.activityTypePreferences))
        // Should NOT fill all Environments
        assertFalse(prefs.contains(Preference.NIGHTLIFE))
    }

    /**
     * Helper to extract the TripSettings using reflection,
     * allowing us to verify the state inside the private SelectActivities instance.
     */
    private fun getPreferencesFromAlgorithm(algo: TripAlgorithm): List<Preference> {
        // 1. Get private 'activitySelector' field from TripAlgorithm
        val activitySelectorField = TripAlgorithm::class.java.getDeclaredField("activitySelector")
        activitySelectorField.isAccessible = true
        val selector = activitySelectorField.get(algo) as SelectActivities

        // 2. Get private 'tripSettings' field from SelectActivities
        val settingsField = SelectActivities::class.java.getDeclaredField("tripSettings")
        settingsField.isAccessible = true
        val internalSettings = settingsField.get(selector) as TripSettings

        return internalSettings.preferences
    }
}