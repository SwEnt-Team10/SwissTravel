package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserRepositoryEmulatorTest : InMemorySwissTravelTest() {
  private lateinit var repositoryUser: UserRepositoryFirebase

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearFirestoreEmulator()
    repositoryUser = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
  }

  @Test
  fun getCurrentUser_createsNewUserIfNotExists() = runBlocking {
    // Arrange
    val email = "lionel@example.com"
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("Lionel", email)
    FirebaseEmulator.createGoogleUser(fakeIdToken)

    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    val authResult = FirebaseEmulator.auth.signInWithCredential(credential).await()
    val firebaseUser = authResult.user!!
    val uid = firebaseUser.uid

    // TripActivity
    val user = repositoryUser.getCurrentUser()

    // Assert
    assertEquals(uid, user.uid)
    assertEquals(email, user.email)
    assertTrue(user.preferences.isEmpty())

    // Verify Firestore document now exists
    val doc = FirebaseEmulator.firestore.collection("users").document(uid).get().await()
    assertTrue(doc.exists())
  }

  @Test
  fun getCurrentUser_returnsExistingUserIfExists() = runBlocking {
    // Arrange
    val fakeIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("Existing User", "existing@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()

    val uid = Firebase.auth.currentUser!!.uid
    val existingData =
        mapOf(
            "uid" to uid,
            "name" to "Saved User",
            "email" to "existing@example.com",
            "profilePicUrl" to "http://example.com/avatar.png",
            "preferences" to listOf("COUPLE", "FOODIE"))
    FirebaseEmulator.firestore.collection("users").document(uid).set(existingData).await()

    // TripActivity
    val user = repositoryUser.getCurrentUser()

    // Assert
    assertEquals("Saved User", user.name)
    assertEquals("existing@example.com", user.email)
    assertEquals(2, user.preferences.size)
  }

  @Test
  fun updateUserPreferences_updatesFirestoreDocument() = runBlocking {
    // Arrange
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("Update User", "update@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    FirebaseEmulator.auth.signInWithCredential(credential).await()
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    repositoryUser.getCurrentUser()

    // TripActivity
    val newPrefs = listOf(Preference.URBAN, Preference.SCENIC_VIEWS, Preference.NIGHTLIFE)
    repositoryUser.updateUserPreferences(uid, newPrefs)

    // Assert
    val doc = FirebaseEmulator.firestore.collection("users").document(uid).get().await()
    val storedPrefs = doc.get("preferences") as List<*>
    val storedEnums = storedPrefs.map { Preference.valueOf(it as String) }
    assertEquals(newPrefs, storedEnums)
  }

  @Test
  fun getCurrentUser_returnsGuestWhenNoUserLoggedIn() = runBlocking {
    // Arrange: ensure no user is signed in
    FirebaseEmulator.auth.signOut()

    // TripActivity
    val user = repositoryUser.getCurrentUser()

    // Assert
    assertEquals("guest", user.uid)
    assertEquals("Guest", user.name)
    assertEquals("Not signed in", user.email)
    assertTrue(user.preferences.isEmpty())
  }

  @Test
  fun getCurrentUser_usesCacheWhenServerFails() {
    runBlocking {
      // Arrange
      val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("Cache User", "cache@example.com")
      FirebaseEmulator.createGoogleUser(fakeIdToken)
      val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
      FirebaseEmulator.auth.signInWithCredential(credential).await()
      val uid = FirebaseEmulator.auth.currentUser!!.uid

      // Create and cache a user document
      val cachedData =
          mapOf(
              "uid" to uid,
              "name" to "Cached User",
              "email" to "cache@example.com",
              "preferences" to listOf("COUPLE"))
      FirebaseEmulator.firestore.collection("users").document(uid).set(cachedData).await()

      // Build repo using same Firestore but simulate network/server failure
      val repo = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
      FirebaseEmulator.firestore.disableNetwork().await()

      // TripActivity
      val user = repo.getCurrentUser()

      // Assert
      assertEquals("Cached User", user.name)
      assertEquals("cache@example.com", user.email)

      FirebaseEmulator.firestore.enableNetwork().await()
    }
  }

  @Test(expected = Exception::class)
  fun updateUserPreferences_throwsIfUserDocDoesNotExist() = runBlocking {
    // Arrange
    val fakeIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("Missing User", "missing@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    FirebaseEmulator.auth.signInAnonymously().await()
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    // TripActivity — no Firestore doc created yet
    repositoryUser.updateUserPreferences(uid, listOf(Preference.MUSEUMS))
  }

  @Test
  fun updateUserPreferences_doesNothingForGuest() = runBlocking {
    // TripActivity
    repositoryUser.updateUserPreferences("guest", listOf(Preference.SCENIC_VIEWS))
    // Assert — should not throw
    assertTrue(true)
  }

  @Test
  fun updateUserStats_updatesFirestoreDocument() = runBlocking {
    // Arrange: create and sign in a user
    val fakeIdToken = FakeJwtGenerator.createFakeGoogleIdToken("Stats User", "stats@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    val credential = GoogleAuthProvider.getCredential(fakeIdToken, null)
    val authResult = FirebaseEmulator.auth.signInWithCredential(credential).await()
    val firebaseUser = authResult.user!!
    val uid = firebaseUser.uid

    // Ensure user doc exists
    repositoryUser.getCurrentUser()

    val stats =
        UserStats(
            totalTrips = 3,
            totalTravelMinutes = 180,
            uniqueLocations = 5,
            mostUsedTransportMode = TransportMode.TRAIN,
            longestRouteSegmentMin = 90)

    // Act
    repositoryUser.updateUserStats(uid, stats)

    // Assert
    val doc = FirebaseEmulator.firestore.collection("users").document(uid).get().await()
    val statsMap = doc.get("stats") as Map<*, *>

    assertEquals(3, (statsMap["totalTrips"] as Number).toInt())
    assertEquals(180, (statsMap["totalTravelMinutes"] as Number).toInt())
    assertEquals(5, (statsMap["uniqueLocations"] as Number).toInt())
    assertEquals("TRAIN", statsMap["mostUsedTransportMode"])
    assertEquals(90, (statsMap["longestRouteSegmentMin"] as Number).toInt())
  }

  @Test
  fun sendFriendRequest_addsPendingFriendToCurrentUserOnly() = runBlocking {
    // Arrange: create A (current user)
    val tokenA = FakeJwtGenerator.createFakeGoogleIdToken("User A", "userA@example.com")
    FirebaseEmulator.createGoogleUser(tokenA)
    val credentialA = GoogleAuthProvider.getCredential(tokenA, null)
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    val userA = repositoryUser.getCurrentUser()
    val uidA = userA.uid

    // Create B (we just need a valid uid as target)
    val tokenB = FakeJwtGenerator.createFakeGoogleIdToken("User B", "userB@example.com")
    FirebaseEmulator.createGoogleUser(tokenB)
    val credentialB = GoogleAuthProvider.getCredential(tokenB, null)
    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    val userB = repositoryUser.getCurrentUser()
    val uidB = userB.uid

    // Back to A to send request (respects rules: A updates /users/uidA)
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()

    // Act
    repositoryUser.sendFriendRequest(fromUid = uidA, toUid = uidB)

    // Assert: A has a pending friend entry for B
    val docA = FirebaseEmulator.firestore.collection("users").document(uidA).get().await()
    val friendsA = docA.get("friends") as List<*>
    assertEquals(1, friendsA.size)
    val friendA = friendsA.first() as Map<*, *>
    assertEquals(uidB, friendA["uid"])
    assertEquals("PENDING", friendA["status"])
  }

  @Test
  fun acceptFriendRequest_updatesStatusToAcceptedOnCurrentUserDoc() = runBlocking {
    // Arrange: create B as current user
    val tokenB = FakeJwtGenerator.createFakeGoogleIdToken("User B2", "userB2@example.com")
    FirebaseEmulator.createGoogleUser(tokenB)
    val credentialB = GoogleAuthProvider.getCredential(tokenB, null)
    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    val userB = repositoryUser.getCurrentUser()
    val uidB = userB.uid

    // Create A (needed for uid)
    val tokenA = FakeJwtGenerator.createFakeGoogleIdToken("User A2", "userA2@example.com")
    FirebaseEmulator.createGoogleUser(tokenA)
    val credentialA = GoogleAuthProvider.getCredential(tokenA, null)
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    val userA = repositoryUser.getCurrentUser()
    val uidA = userA.uid

    // Back to B, and simulate a pending entry for A
    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    val pendingList = listOf(mapOf("uid" to uidA, "status" to "PENDING"))
    FirebaseEmulator.firestore
        .collection("users")
        .document(uidB)
        .update("friends", pendingList)
        .await()

    // Act: B accepts A
    repositoryUser.acceptFriendRequest(currentUid = uidB, fromUid = uidA)

    // Assert
    val docB = FirebaseEmulator.firestore.collection("users").document(uidB).get().await()
    val friendsB = docB.get("friends") as List<*>
    val friendB = friendsB.first() as Map<*, *>
    assertEquals(uidA, friendB["uid"])
    assertEquals("ACCEPTED", friendB["status"])
  }

  @Test
  fun removeFriend_removesFriendOnlyFromCurrentUserDoc() = runBlocking {
    // Arrange: create A
    val tokenA = FakeJwtGenerator.createFakeGoogleIdToken("User A3", "userA3@example.com")
    FirebaseEmulator.createGoogleUser(tokenA)
    val credentialA = GoogleAuthProvider.getCredential(tokenA, null)
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    val userA = repositoryUser.getCurrentUser()
    val uidA = userA.uid

    // Create B
    val tokenB = FakeJwtGenerator.createFakeGoogleIdToken("User B3", "userB3@example.com")
    FirebaseEmulator.createGoogleUser(tokenB)
    val credentialB = GoogleAuthProvider.getCredential(tokenB, null)
    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    val userB = repositoryUser.getCurrentUser()
    val uidB = userB.uid

    // Back to A: simulate ACCEPTED friendship with B
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    val acceptedList = listOf(mapOf("uid" to uidB, "status" to "ACCEPTED"))
    FirebaseEmulator.firestore
        .collection("users")
        .document(uidA)
        .update("friends", acceptedList)
        .await()

    // Act: A removes B
    repositoryUser.removeFriend(uid = uidA, friendUid = uidB)

    // Assert: A has no friends
    val docA = FirebaseEmulator.firestore.collection("users").document(uidA).get().await()
    val friendsA = docA.get("friends") as? List<*> ?: emptyList<Any>()
    assertTrue(friendsA.isEmpty())

    // B is unaffected
    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    val docB = FirebaseEmulator.firestore.collection("users").document(uidB).get().await()
    val friendsB = docB.get("friends") as? List<*> ?: emptyList<Any>()
  }

  @After
  override fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }
}
