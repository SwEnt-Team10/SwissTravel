package com.github.swent.swisstravel.model.user

interface UserRepository {
  suspend fun getCurrentUser(): User

  suspend fun updateUserPreferences(uid: String, preferences: List<Preference>)
}
