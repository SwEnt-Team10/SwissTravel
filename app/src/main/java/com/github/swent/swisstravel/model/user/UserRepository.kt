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

  /**
   * Sends a friend request to the specified user.
   *
   * @param fromUid The UID of the user sending the request.
   * @param toUid The UID of the user receiving the request.
   */
  suspend fun sendFriendRequest(fromUid: String, toUid: String)

  /**
   * Accepts a friend request from the specified user.
   *
   * @param currentUid The UID of the user accepting the request.
   * @param fromUid The UID of the user sending the request.
   */
  suspend fun acceptFriendRequest(currentUid: String, fromUid: String)

  /**
   * Removes a friend from the user's friend list.
   *
   * @param uid The UID of the user.
   * @param friendUid The UID of the friend to remove.
   */
  suspend fun removeFriend(uid: String, friendUid: String)
}
