package com.github.swent.swisstravel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserPreference
import com.github.swent.swisstravel.model.user.UserRepository
import com.github.swent.swisstravel.model.user.displayString
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ProfileScreenUIState(
    val profilePicUrl: String = "",
    val name: String = "",
    val email: String = "",
    var selectedPreferences: List<String> = emptyList(),
    var errorMsg : String? = null
)

class ProfileScreenViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

        private val _uiState = MutableStateFlow(ProfileScreenUIState())
        val uiState: StateFlow<ProfileScreenUIState> = _uiState.asStateFlow()
        val allPreferences = enumValues<UserPreference>().map { it.displayString() }

        init {
            viewModelScope.launch {
                try {
                    val user = userRepository.getCurrentUser()
                    autoFill(user)
                } catch (e: Exception) {
                    _uiState.value = uiState.value.copy(
                        errorMsg = "Error fetching user data: ${e.message}"
                    )
                }
            }
        }

    fun autoFill(loggedIn: User){
        _uiState.value = ProfileScreenUIState(
            profilePicUrl = loggedIn.profilePicUrl,
            name = loggedIn.name,
            email = loggedIn.email,
            selectedPreferences = loggedIn.preferences.map { it.displayString() }
        )
    }

    fun clearErrorMsg() {
        _uiState.update { it.copy(errorMsg = null) }
    }

    fun savePreferences(selected: List<String>) {
        viewModelScope.launch {
            try {
                val uid = userRepository.getCurrentUser().uid
                userRepository.updateUserPreferences(uid, selected)
                _uiState.update { it.copy(selectedPreferences = selected) }
            } catch (e: Exception) {
                _uiState.value = uiState.value.copy(
                    errorMsg = "Error saving preferences: ${e.message}"
                )
            }
        }
    }
}

