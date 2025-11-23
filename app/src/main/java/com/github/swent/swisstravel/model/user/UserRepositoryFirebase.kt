package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.TransportMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class UserRepositoryFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

  init {
    db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
  }

  /**
   * Retrieves the current user from Firebase Authentication. If the user is not signed in, a guest
   * user is returned. If the user is signed in, their information is retrieved from Firestore. If
   * the user's information is not found in Firestore, a new user is created.
   */
  override suspend fun getCurrentUser(): User {
    val firebaseUser = auth.currentUser

    if (firebaseUser == null || firebaseUser.isAnonymous) {
      return User(
          uid = "guest",
          name = "Guest",
          biography = "",
          email = "Not signed in",
          profilePicUrl = "",
          preferences = emptyList(),
          friends = emptyList(),
          stats = UserStats())
    }

    val uid = firebaseUser.uid
    return try {
      val doc = db.collection("users").document(uid).get(Source.SERVER).await()
      if (doc.exists()) createUserFromDoc(doc, uid) else createAndStoreNewUser(firebaseUser, uid)
    } catch (e: Exception) {
      try {
        val cachedDoc = db.collection("users").document(uid).get(Source.CACHE).await()
        if (cachedDoc.exists()) {
          createUserFromDoc(cachedDoc, uid)
        } else {
          createAndStoreNewUser(firebaseUser, uid)
        }
      } catch (e: Exception) {
        createAndStoreNewUser(firebaseUser, uid)
      }
    }
  }

  /**
   * Function to update the user's preferences in Firestore.
   *
   * @param uid The UID of the user.
   * @param preferences The list of preferences to update.
   * @throws IllegalStateException if the user document does not exist.
   */
  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {
    if (uid == "guest") return
    val docRef = db.collection("users").document(uid)
    val userDoc = docRef.get().await()
    check(userDoc.exists()) {
      throw IllegalStateException("User document does not exist for uid: $uid")
    }

    val names = preferences.map { it.name }
    docRef.update("preferences", names).await()
  }

  /**
   * Function to update the user's stats in Firestore.
   *
   * @param uid The UID of the user.
   */
  override suspend fun updateUserStats(uid: String, stats: UserStats) {
    if (uid == "guest") return
    db.collection("users").document(uid).update("stats", stats).await()
  }

  /**
   * Sends a friend request to the specified user.
   *
   * @param fromUid The UID of the user sending the request.
   * @param toUid The UID of the user receiving the request.
   */
  override suspend fun sendFriendRequest(fromUid: String, toUid: String) {
    val currentAuthUid = auth.currentUser?.uid
    if (fromUid == "guest" || currentAuthUid == null || currentAuthUid != fromUid) return

    val docRef = db.collection("users").document(fromUid)
    val doc = docRef.get().await()
    check(doc.exists()) { "User document does not exist for uid: $fromUid" }

    val friends = parseFriends(doc).toMutableList()

    // Don't duplicate existing friend entry
    val already = friends.any { it.uid == toUid }
    if (!already) {
      friends.add(Friend(uid = toUid, status = FriendStatus.PENDING))
      docRef.update("friends", friends).await()
    }
  }

  /**
   * Accepts a friend request from the specified user.
   *
   * @param currentUid The UID of the user accepting the request.
   * @param fromUid The UID of the user sending the request.
   */
  override suspend fun acceptFriendRequest(currentUid: String, fromUid: String) {
    val currentAuthUid = auth.currentUser?.uid
    if (currentUid == "guest" || currentAuthUid == null || currentAuthUid != currentUid) return

    val docRef = db.collection("users").document(currentUid)
    val doc = docRef.get().await()
    check(doc.exists()) { "User document does not exist for uid: $currentUid" }

    val friends = parseFriends(doc).toMutableList()

    val idx = friends.indexOfFirst { it.uid == fromUid }
    if (idx >= 0) {
      friends[idx] = friends[idx].copy(status = FriendStatus.ACCEPTED)
    } else {
      // If no entry yet, just add it as ACCEPTED
      friends.add(Friend(uid = fromUid, status = FriendStatus.ACCEPTED))
    }

    docRef.update("friends", friends).await()
  }

  /**
   * Removes a friend from the user's friend list.
   *
   * @param uid The UID of the user.
   * @param friendUid The UID of the friend to remove.
   */
  override suspend fun removeFriend(uid: String, friendUid: String) {
    val currentAuthUid = auth.currentUser?.uid
    if (uid == "guest" || currentAuthUid == null || currentAuthUid != uid) return

    val docRef = db.collection("users").document(uid)
    val doc = docRef.get().await()
    if (!doc.exists()) return

    val friends = parseFriends(doc).toMutableList()
    friends.removeAll { it.uid == friendUid }

    docRef.update("friends", friends).await()
  }

  /**
   * Helper function to create a User object from a DocumentSnapshot.
   *
   * @param doc The DocumentSnapshot to create the User from.
   * @param uid The UID of the user.
   * @return The created User object.
   */
  private fun createUserFromDoc(doc: DocumentSnapshot, uid: String): User {
    val prefs = parsePreferences(doc)
    val friends = parseFriends(doc)
    val stats = parseStats(doc)

    return User(
        uid = uid,
        name = doc.getString("name").orEmpty(),
        biography = doc.getString("biography").orEmpty(),
        email = doc.getString("email").orEmpty(),
        profilePicUrl = doc.getString("profilePicUrl").orEmpty(),
        preferences = prefs,
        friends = friends,
        stats = stats)
  }

  /**
   * Helper function to create a new User object and store it in Firestore.
   *
   * @param firebaseUser The FirebaseUser to create the User from.
   * @param uid The UID of the user.
   * @return The created User object.
   */
  private suspend fun createAndStoreNewUser(firebaseUser: FirebaseUser, uid: String): User {
    val newUser =
        User(
            uid = uid,
            name = firebaseUser.displayName.orEmpty(),
            biography = "",
            email = firebaseUser.email.orEmpty(),
            profilePicUrl = firebaseUser.photoUrl?.toString().orEmpty(),
            preferences = emptyList(),
            friends = emptyList(),
            stats = UserStats())

    db.collection("users").document(uid).set(newUser).await()
    return newUser
  }

  /**
   * Helper to parse a list of preferences from a DocumentSnapshot.
   *
   * @param doc The DocumentSnapshot to parse the preferences from.
   * @return The parsed list of preferences.
   */
  private fun parsePreferences(doc: DocumentSnapshot): List<Preference> {
    val rawList = (doc["preferences"] as? List<*>) ?: emptyList<Any>()
    val prefs = mutableListOf<Preference>()

    for (item in rawList) {
      val str = item?.toString() ?: continue
      val pref =
          try {
            Preference.valueOf(str)
          } catch (_: IllegalArgumentException) {
            null
          }
      if (pref != null) prefs.add(pref)
    }
    return prefs
  }

  /**
   * Helper to parse a list of friends from a DocumentSnapshot.
   *
   * @param doc The DocumentSnapshot to parse the friends from.
   * @return The parsed list of friends.
   */
  private fun parseFriends(doc: DocumentSnapshot): List<Friend> {
    val rawList = doc.get("friends") as? List<*> ?: emptyList<Any>()
    val friends = mutableListOf<Friend>()

    for (item in rawList) {
      val map = item as? Map<*, *> ?: continue
      val friendUid = map["uid"]?.toString() ?: continue
      val statusStr = map["status"]?.toString() ?: continue

      val status =
          try {
            FriendStatus.valueOf(statusStr)
          } catch (_: IllegalArgumentException) {
            continue
          }

      friends.add(Friend(uid = friendUid, status = status))
    }

    return friends
  }

  /**
   * Helper to parse the user's stats from a DocumentSnapshot.
   *
   * @param doc The DocumentSnapshot to parse the stats from.
   * @return The parsed UserStats object.
   */
  private fun parseStats(doc: DocumentSnapshot): UserStats {
    val statsMap = doc.get("stats") as? Map<*, *> ?: return UserStats()

    fun <T : Number> num(key: String, default: Double = 0.0): Double {
      val value = statsMap[key] as? Number ?: return default
      return value.toDouble()
    }

    val totalTrips = (statsMap["totalTrips"] as? Number)?.toInt() ?: 0
    val totalTravelMinutes = (statsMap["totalTravelMinutes"] as? Number)?.toInt() ?: 0
    val uniqueLocations = (statsMap["uniqueLocations"] as? Number)?.toInt() ?: 0

    val transportModeStr = statsMap["mostUsedTransportMode"]?.toString()
    val mostUsedTransportMode =
        transportModeStr?.let {
          try {
            TransportMode.valueOf(it)
          } catch (_: IllegalArgumentException) {
            null
          }
        }

    val longestRouteSegmentMin = (statsMap["longestRouteSegmentMin"] as? Number)?.toInt() ?: 0

    return UserStats(
        totalTrips = totalTrips,
        totalTravelMinutes = totalTravelMinutes,
        uniqueLocations = uniqueLocations,
        mostUsedTransportMode = mostUsedTransportMode,
        longestRouteSegmentMin = longestRouteSegmentMin)
  }
}
