package com.github.swent.swisstravel.ui.mytrips

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyTripsUIState(
    val currentTrip: Trip,
    val pastTrips: List<Trips> = emptyList(),
    val upcomingTrips: List<Trips> = emptyList(),
    val errorMsg: String? = null,
)

class MyTripsViewModel(
    private val tripsRepository: TripsRepository = TripsRepositoryProvider.repository,
    private val authRepository: AuthRepository = AuthRepositoryFirebase(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTripsUIState())
    val uiState: StateFlow<MyTripsUIState> = _uiState.asStateFlow()

    init {
        getAllTrips()
    }

    /** Clears the error message in the UI state. */
    fun clearErrorMsg() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    /** Sets an error message in the UI state. */
    private fun setErrorMsg(errorMsg: String) {
        _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
    }

    /** Refreshes the UI state by fetching all Trips items from the repository. */
    fun refreshUIState() {
        getAllTrips()
    }

    /** Fetches all Trips from the repository and updates the UI state. */
    private fun getAllTrips() {
        viewModelScope.launch {
            try {
                val trips = tripsRepository.getAllTrips()
                _uiState.value = MyTripsUIState(trips = trips)
            } catch (e: Exception) {
                Log.e("MyTripsViewModel", "Error fetching trips", e)
                setErrorMsg("Failed to load trips: ${e.message}")
            }
        }
    }

}