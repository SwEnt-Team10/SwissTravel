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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
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
    val firebaseUser =
        auth.currentUser
            ?: // If not signed in, return a guest user
            return User(
                uid = "guest",
                name = "Guest",
                biography = "",
                email = "Not signed in",
                profilePicUrl = "",
                preferences = emptyList(),
                friends = emptyList(),
                stats = UserStats(),
                pinnedTripsUids = emptyList(),
                pinnedPicturesUids = emptyList())

    val uid = firebaseUser.uid
    return try {
      val doc = db.collection("users").document(uid)[Source.SERVER].await()
      if (doc.exists()) createUserFromDoc(doc, uid) else createAndStoreNewUser(firebaseUser, uid)
    } catch (e: Exception) {
      try {
        val cachedDoc = db.collection("users").document(uid)[Source.CACHE].await()
        if (cachedDoc.exists()) {
          createUserFromDoc(cachedDoc, uid)
        } else {
          createAndStoreNewUser(firebaseUser, uid)
        }
      } catch (_: Exception) {
        createAndStoreNewUser(firebaseUser, uid)
      }
    }
  }

  /**
   * Retrieves a user by their UID.
   *
   * @param uid The UID of the user to retrieve.
   * @return The User object if found, null otherwise.
   */
  override suspend fun getUserByUid(uid: String): User? {
    return try {
      val doc = db.collection("users").document(uid).get().await()
      if (doc.exists()) {
        createUserFromDoc(doc, uid)
      } else {
        null
      }
    } catch (e: Exception) {
      try {
        val doc = db.collection("users").document(uid)[Source.CACHE].await()
        if (doc.exists()) {
          createUserFromDoc(doc, uid)
        } else {
          null
        }
      } catch (_: Exception) {
        null
      }
    }
  }

  /**
   * Retrieves a list of users whose name or email matches the given query.
   *
   * @param query The search query to match against user names and emails.
   * @return A list of User objects that match the query.
   */
  override suspend fun getUserByNameOrEmail(query: String): List<User> {
    if (query.isBlank()) return emptyList()

    val q = query.trim()

    return try {
      val usersRef = db.collection("users")

      // 1. Query by name
      val nameQuery = usersRef.orderBy("name").startAt(q).endAt(q + "\uf8ff").get().await()

      // 2. Query by email
      val emailQuery = usersRef.orderBy("email").startAt(q).endAt(q + "\uf8ff").get().await()

      // Merge two lists into a set to eliminate duplicates
      val docs = (nameQuery.documents + emailQuery.documents).distinctBy { it.id }

      docs.mapNotNull { doc -> createUserFromDoc(doc, doc.id) }
    } catch (e: Exception) {
      emptyList()
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
    if (fromUid == toUid) return
    if (fromUid == "guest" || currentAuthUid == null || currentAuthUid != fromUid) return

    val usersRef = db.collection("users")
    val fromRef = usersRef.document(fromUid)
    val toRef = usersRef.document(toUid)

    db.runTransaction { tx ->
          val fromSnap = tx[fromRef]
          val toSnap = tx[toRef]

          check(fromSnap.exists()) { "User document does not exist for uid: $fromUid" }
          check(toSnap.exists()) { "User document does not exist for uid: $toUid" }

          val (updatedFromFriends, updatedToFriends) =
              buildPendingFriendshipUpdate(
                  fromSnap = fromSnap, toSnap = toSnap, fromUid = fromUid, toUid = toUid)

          tx.update(fromRef, "friends", updatedFromFriends)
          tx.update(toRef, "friends", updatedToFriends)
          null
        }
        .await()
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

    val usersRef = db.collection("users")
    val currentRef = usersRef.document(currentUid)
    val fromRef = usersRef.document(fromUid)

    db.runTransaction { tx ->
          val currentSnap = tx[currentRef]
          val fromSnap = tx[fromRef]

          check(currentSnap.exists()) { "User document does not exist for uid: $currentUid" }
          check(fromSnap.exists()) { "User document does not exist for uid: $fromUid" }

          val currentFriends = parseFriends(currentSnap).toMutableList()
          val fromFriends = parseFriends(fromSnap).toMutableList()

          // On current user: ensure entry for fromUid is ACCEPTED
          val curIdx = currentFriends.indexOfFirst { it.uid == fromUid }
          if (curIdx >= 0) {
            currentFriends[curIdx] = currentFriends[curIdx].copy(status = FriendStatus.ACCEPTED)
          } else {
            currentFriends.add(Friend(uid = fromUid, status = FriendStatus.ACCEPTED))
          }

          // On other user: ensure entry for currentUid is ACCEPTED
          val fromIdx = fromFriends.indexOfFirst { it.uid == currentUid }
          if (fromIdx >= 0) {
            fromFriends[fromIdx] = fromFriends[fromIdx].copy(status = FriendStatus.ACCEPTED)
          } else {
            fromFriends.add(Friend(uid = currentUid, status = FriendStatus.ACCEPTED))
          }

          tx.update(currentRef, "friends", currentFriends)
          tx.update(fromRef, "friends", fromFriends)
          null
        }
        .await()
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

    val usersRef = db.collection("users")
    val userRef = usersRef.document(uid)
    val friendRef = usersRef.document(friendUid)

    db.runTransaction { tx ->
          val userSnap = tx[userRef]
          val friendSnap = tx[friendRef]

          if (!userSnap.exists()) {
            // Nothing to do if current user doc doesn't exist
            return@runTransaction null
          }

          val userFriends = parseFriends(userSnap).toMutableList()
          userFriends.removeAll { it.uid == friendUid }

          tx.update(userRef, "friends", userFriends)

          if (friendSnap.exists()) {
            val friendFriends = parseFriends(friendSnap).toMutableList()
            friendFriends.removeAll { it.uid == uid }
            tx.update(friendRef, "friends", friendFriends)
          }

          null
        }
        .await()
  }

  /**
   * Builds the updated friends lists for both sides of a pending friend request.
   *
   * @return Pair of (fromUserFriends, toUserFriends).
   */
  private fun buildPendingFriendshipUpdate(
      fromSnap: DocumentSnapshot,
      toSnap: DocumentSnapshot,
      fromUid: String,
      toUid: String
  ): Pair<List<Friend>, List<Friend>> {
    val fromFriends = parseFriends(fromSnap).toMutableList()
    val toFriends = parseFriends(toSnap).toMutableList()

    val fromIdx = fromFriends.indexOfFirst { it.uid == toUid }
    val toIdx = toFriends.indexOfFirst { it.uid == fromUid }

    val fromExisting = fromIdx.takeIf { it >= 0 }?.let { fromFriends[it] }
    val toExisting = toIdx.takeIf { it >= 0 }?.let { toFriends[it] }

    // If there is already a "mutual pending" relationship, upgrade to ACCEPTED
    val isMutualPending =
        (fromExisting?.status == FriendStatus.PENDING_INCOMING &&
            toExisting?.status == FriendStatus.PENDING_OUTGOING) ||
            (fromExisting?.status == FriendStatus.PENDING_OUTGOING &&
                toExisting?.status == FriendStatus.PENDING_INCOMING)
    if (isMutualPending) {
      fromFriends[fromIdx] = fromFriends[fromIdx].copy(status = FriendStatus.ACCEPTED)

      toFriends[toIdx] = toFriends[toIdx].copy(status = FriendStatus.ACCEPTED)

      return fromFriends to toFriends
    }

    ensurePendingEntry(
        friends = fromFriends, targetUid = toUid, newStatus = FriendStatus.PENDING_OUTGOING)
    ensurePendingEntry(
        friends = toFriends, targetUid = fromUid, newStatus = FriendStatus.PENDING_INCOMING)

    return fromFriends to toFriends
  }

  /**
   * Ensures there is an entry for [targetUid] with at least PENDING status. If an entry already
   * exists with PENDING or ACCEPTED, it is left untouched. Otherwise the status is set/overwritten
   * to PENDING.
   */
  private fun ensurePendingEntry(
      friends: MutableList<Friend>,
      targetUid: String,
      newStatus: FriendStatus
  ) {
    val idx = friends.indexOfFirst { it.uid == targetUid }
    if (idx < 0) {
      friends.add(Friend(uid = targetUid, status = newStatus))
      return
    }

    val existing = friends[idx]
    if (existing.status == FriendStatus.ACCEPTED) {
      return
    }

    friends[idx] = existing.copy(status = newStatus)
  }

  /**
   * Updates basic user fields in Firestore.
   *
   * @param uid The UID of the user.
   * @param name Optional new name.
   * @param biography Optional new biography.
   * @param profilePicUrl Optional new profile picture URL.
   * @param preferences Optional list of updated preferences.
   * @param pinnedTripsUids Optional updated list of pinned trip UIDs.
   * @param pinnedPicturesUids Optional updated list of pinned picture UIDs.
   */
  override suspend fun updateUser(
      uid: String,
      name: String?,
      biography: String?,
      profilePicUrl: String?,
      preferences: List<Preference>?,
      pinnedTripsUids: List<String>?,
      pinnedPicturesUids: List<String>?
  ) {
    if (uid == "guest") return

    val updates = mutableMapOf<String, Any?>()

    if (name != null) updates["name"] = name
    if (biography != null) updates["biography"] = biography
    if (profilePicUrl != null) updates["profilePicUrl"] = profilePicUrl
    if (preferences != null) updates["preferences"] = preferences.map { it.name }
    if (pinnedTripsUids != null) updates["pinnedTripsUids"] = pinnedTripsUids
    if (pinnedPicturesUids != null) updates["pinnedPicturesUids"] = pinnedPicturesUids

    // If nothing to update, skip Firestore
    if (updates.isEmpty()) return

    val docRef = db.collection("users").document(uid)
    val snapshot = docRef.get().await()

    check(snapshot.exists()) { "User document does not exist for uid: $uid" }

    docRef.update(updates).await()
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
    val pinnedTripsUids =
        (doc["pinnedTripsUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val pinnedPicturesUids =
        (doc["pinnedPictureUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    return User(
        uid = uid,
        name = doc["name"] as? String ?: "",
        biography = doc["biography"] as? String ?: "",
        email = doc["email"] as? String ?: "",
        profilePicUrl = doc["profilePicUrl"] as? String ?: "",
        preferences = prefs,
        friends = friends,
        stats = stats,
        pinnedTripsUids = pinnedTripsUids,
        pinnedPicturesUids = pinnedPicturesUids)
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
            stats = UserStats(),
            pinnedTripsUids = emptyList(),
            pinnedPicturesUids = emptyList())

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
    val rawList = doc["preferences"] as? List<*> ?: emptyList<Any>()
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
    val rawList = doc["friends"] as? List<*> ?: emptyList<Any>()
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
    val statsMap = doc["stats"] as? Map<*, *> ?: return UserStats()

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
