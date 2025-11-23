package com.github.swent.swisstravel.model.user

interface UserRepository {
  /**
   * Retrieves the current user from Firebase Authentication. If the user is not signed in, a guest
   * user is returned. If the user is signed in, their information is retrieved from Firestore. If
   * the user's information is not found in Firestore, a new user is created.
   */
  suspend fun getCurrentUser(): User

  /**
   * Function to update the user's preferences in Firestore.
   *
   * @param uid The UID of the user.
   * @param preferences The list of preferences to update.
   * @throws IllegalStateException if the user document does not exist.
   */
  suspend fun updateUserPreferences(uid: String, preferences: List<Preference>)

  /**
   * Function to update the user's stats in Firestore.
   *
   * @param uid The UID of the user.
   */
  suspend fun updateUserStats(uid: String, stats: UserStats)
}
