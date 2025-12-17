package com.github.swent.swisstravel.ui.trips

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.trip.Trip
import com.github.swent.swisstravel.model.trip.TripsRepository
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.ui.trips.TripsViewModel.CollaboratorUi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents the different sorting options for trips on the screen. */
enum class TripSortType {
  START_DATE_ASC,
  START_DATE_DESC,
  END_DATE_ASC,
  END_DATE_DESC,
  NAME_ASC,
  NAME_DESC,
  FAVORITES_FIRST,
}

/**
 * Base ViewModel for any screen displaying a list of trips. Handles selection, sorting, error
 * state, and bulk actions.
 *
 * @param tripsRepository The repository responsible for managing trip data.
 */
abstract class TripsViewModel(
    protected val userRepository: UserRepository = UserRepositoryFirebase(),
    protected val tripsRepository: TripsRepository
) : ViewModel() {
  data class CollaboratorUi(
      val userId: String,
      val displayName: String,
      val avatarUrl: String,
  )

  /**
   * Represents the UI state for a "Trips" screen.
   *
   * @property currentTrip The user's current active trip, if any.
   * @property tripsList The list of trips.
   * @property errorMsg An optional error message to display in the UI.
   * @property sortType The current sorting preference for upcoming trips.
   * @property isSelectionMode Whether the user is currently selecting multiple trips.
   * @property selectedTrips The set of trips currently selected in selection mode.
   * @property isLoading Whether the UI is currently loading data.
   * @property collaboratorsByTripId A map of trips to their associated collaborators.
   * @property favoriteTripsUids The set of favorite trip UIDs.
   */
  data class TripsUIState(
      val currentTrip: Trip? = null,
      val tripsList: List<Trip> = emptyList(),
      val errorMsg: String? = null,
      val sortType: TripSortType = TripSortType.START_DATE_ASC,
      val isSelectionMode: Boolean = false,
      val selectedTrips: Set<Trip> = emptySet(),
      val isLoading: Boolean = false,
      val collaboratorsByTripId: Map<String, List<CollaboratorUi>> = emptyMap(),
      val favoriteTripsUids: Set<String> = emptySet()
  )

  /**
   * Subclasses can override this to set the initial UI state (e.g. if they want to use a different
   * dataclass).
   */
  protected fun createInitialState() = TripsUIState()

  protected val _uiState = MutableStateFlow(createInitialState())
  /** Public read-only access to the UI state. */
  val uiState: StateFlow<TripsUIState> = _uiState.asStateFlow()

  /** Subclasses define how all trips from the repository are fetched. */
  abstract fun getAllTrips()

  /** Refreshes the list of trips by re-fetching them from the repository. */
  fun refreshUIState() {
    viewModelScope.launch { getAllTrips() }
  }

  /** Clears any error message currently stored in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param errorMsg The error message to display.
   */
  fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Enables or disables selection mode. When disabled, all previously selected trips are cleared.
   *
   * @param enabled Whether selection mode should be active.
   */
  fun toggleSelectionMode(enabled: Boolean) {
    _uiState.value =
        _uiState.value.copy(
            isSelectionMode = enabled,
            selectedTrips = if (!enabled) emptySet() else _uiState.value.selectedTrips)
  }

  /**
   * Toggles the selection state of a given trip.
   *
   * @param trip The trip to select or deselect.
   */
  fun toggleTripSelection(trip: Trip) {
    onToggleTripSelection(trip)
  }

  /**
   * Toggles the selection state of a given trip.
   *
   * Adds or removes the trip from the set of selected trips. If no trips remain selected, selection
   * mode is automatically disabled.
   *
   * @param trip The trip to select or deselect.
   */
  open fun onToggleTripSelection(trip: Trip) {
    val current = _uiState.value.selectedTrips.toMutableSet()
    if (current.contains(trip)) current.remove(trip) else current.add(trip)

    val newState = _uiState.value.copy(selectedTrips = current)

    _uiState.value =
        if (current.isEmpty()) {
          newState.copy(isSelectionMode = false)
        } else {
          newState
        }
  }

  /**
   * Deletes all currently selected trips from the repository.
   *
   * On success, exits selection mode and refreshes the UI state. On failure, logs the error and
   * displays a generic error message.
   */
  fun deleteSelectedTrips() {
    val toDelete = _uiState.value.selectedTrips
    viewModelScope.launch {
      try {
        toDelete.forEach { tripsRepository.deleteTrip(it.uid) }
        toggleSelectionMode(false)
        refreshUIState()
      } catch (e: Exception) {
        Log.e("MyTripsViewModel", "Error deleting trips", e)
        setErrorMsg("Failed to delete trips.")
      }
    }
  }

  /**
   * Toggles the favorite status of all selected trips.
   *
   * Updates each selected trip by inverting its `isFavorite` flag, saves changes via
   * [TripsRepository.editTrip], exits selection mode, and refreshes the UI.
   */
  fun toggleFavoriteForSelectedTrips() {
    val selected = _uiState.value.selectedTrips
    viewModelScope.launch {
      try {
        val currentUser = userRepository.getCurrentUser()
        val userFavorites = currentUser.favoriteTripsUids.toSet()
        // All currently selected trips already favorites?
        val allSelectedAreFavorite = selected.all { it.uid in userFavorites }
        if (allSelectedAreFavorite) {
          // All selected are favorite -> Unfavorite all of them
          selected.forEach { trip -> userRepository.removeFavoriteTrip(currentUser.uid, trip.uid) }
        } else {
          // None or Some are favorite -> Favorite all of them
          selected.forEach { trip -> userRepository.addFavoriteTrip(currentUser.uid, trip.uid) }
        }
        toggleSelectionMode(false)
        refreshUIState()
      } catch (e: Exception) {
        Log.e("TripsViewModel", "Error toggling favorites", e)
        setErrorMsg("Failed to update favorites.")
      }
    }
  }

  /**
   * Selects all trips.
   *
   * This is triggered when the user chooses "Select All" in selection mode.
   */
  fun selectAllTrips() {
    val allTrips = buildList {
      uiState.value.currentTrip?.let { add(it) }
      addAll(uiState.value.tripsList)
    }
    _uiState.value = _uiState.value.copy(selectedTrips = allTrips.toSet())
  }

  /**
   * Sorts the provided list of trips according to the given sort type.
   *
   * @param trips The list of trips to sort.
   * @param sortType The sorting order to apply.
   * @return The sorted list of trips.
   */
  fun sortTrips(trips: List<Trip>, sortType: TripSortType, favoriteTrips: Set<String>): List<Trip> {
    return when (sortType) {
      TripSortType.START_DATE_ASC -> trips.sortedBy { it.tripProfile.startDate }
      TripSortType.START_DATE_DESC -> trips.sortedByDescending { it.tripProfile.startDate }
      TripSortType.END_DATE_ASC -> trips.sortedBy { it.tripProfile.endDate }
      TripSortType.END_DATE_DESC -> trips.sortedByDescending { it.tripProfile.endDate }
      TripSortType.NAME_ASC -> trips.sortedBy { it.name.lowercase() }
      TripSortType.NAME_DESC -> trips.sortedByDescending { it.name.lowercase() }
      TripSortType.FAVORITES_FIRST -> trips.sortedByDescending { it.uid in favoriteTrips }
    }
  }

  /**
   * Updates the current sort type and re-sorts the list of upcoming trips.
   *
   * @param sortType The new sorting preference selected by the user.
   */
  fun updateSortType(sortType: TripSortType) {
    val trips = _uiState.value.tripsList
    val favorites = _uiState.value.favoriteTripsUids
    _uiState.value =
        _uiState.value.copy(sortType = sortType, tripsList = sortTrips(trips, sortType, favorites))
  }
}

/**
 * Builds the collaborators map for each trip. Made with the help of an AI.
 *
 * @param trips The list of trips to build the collaborators map for.
 * @param userRepository The repository responsible for managing user data.
 * @return A map where each key is a trip ID and the corresponding value is a list of collaborators
 *   for that trip.
 */
internal suspend fun buildCollaboratorsByTrip(
    trips: List<Trip>,
    userRepository: UserRepository
): Map<String, List<CollaboratorUi>> = coroutineScope {
  // 0) Get current user ID to filter self out of the preview
  val currentUser =
      try {
        userRepository.getCurrentUser()
      } catch (_: Exception) {
        null
      }
  val currentUserId = currentUser?.uid ?: ""

  // 1) Get ALL unique user ids involved (Collaborators + Owners)
  // We need to fetch owners too so we can display them to collaborators
  val allUserIds = trips.flatMap { it.collaboratorsId + it.ownerId }.distinct()

  // 2) Fetch all users in parallel using getUserByUid
  val usersById: Map<String, User> =
      allUserIds
          .map { uid -> async { uid to userRepository.getUserByUid(uid) } }
          .awaitAll()
          .mapNotNull { (uid, user) -> user?.let { uid to it } }
          .toMap()

  // 3) Build tripId -> list of CollaboratorUi
  trips.associate { trip ->
    // We want to show everyone involved (Owner + Collaborators) EXCEPT the current user.
    // - If I am the Owner: I see (Owner + Collabs) - Owner = All Collaborators.
    // - If I am a Collaborator: I see (Owner + Collabs) - Me = Owner + Other Collaborators.
    val allParticipants = (listOf(trip.ownerId) + trip.collaboratorsId).distinct()

    val collaboratorsForTrip =
        allParticipants
            .filter { it != currentUserId } // Filter out yourself
            .mapNotNull { id ->
              usersById[id]?.let { user ->
                CollaboratorUi(
                    userId = user.uid,
                    displayName = user.name,
                    avatarUrl = user.profilePicUrl,
                )
              }
            }
    trip.uid to collaboratorsForTrip
  }
}
