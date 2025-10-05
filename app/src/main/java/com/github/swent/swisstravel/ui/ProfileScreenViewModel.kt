package com.github.swent.swisstravel.ui

import androidx.lifecycle.ViewModel
import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserPreference
import com.github.swent.swisstravel.model.user.displayString
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class ProfileScreenUIState(
    val profilePicUrl: String = "",
    val name: String = "",
    val email: String = "",
    var selectedPreferences: List<String> = emptyList()
)

class ProfileScreenViewModel(
    val loggedInUser: FirebaseUser? = Firebase.auth.currentUser
)
    : ViewModel() {

        private val _uiState = MutableStateFlow(ProfileScreenUIState())
        val uiState: StateFlow<ProfileScreenUIState> = _uiState.asStateFlow()

        @OptIn(ExperimentalStdlibApi::class)
        val allPreferences = UserPreference.entries.map{ it.displayString() }

        init {
            val loggedIn = fetchUser()
            autoFill(loggedIn)
        }

        fun fetchUser(): User {
            return User(
                uid = loggedInUser?.uid ?: "default_uid",
                name = loggedInUser?.displayName ?: "Default Name",
                email = loggedInUser?.email ?: "",
                profilePicUrl = loggedInUser?.photoUrl?.toString() ?: "",
                preferences = emptyList()
            )
        }

    fun autoFill(loggedIn: User){
        _uiState.value = ProfileScreenUIState(
            profilePicUrl = loggedIn.profilePicUrl,
            name = loggedIn.name,
            email = loggedIn.email,
            selectedPreferences = loggedIn.preferences.map { it.displayString() }
        )
    }
}

