package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.model.trip.TransportMode
import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.InMemorySwissTravelTest
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import junit.framework.TestCase.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
    repositoryUser = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
  }

  private data class TestUser(val uid: String, val credential: AuthCredential)

  private suspend fun createGoogleUserAndSignIn(
      name: String,
      email: String,
      createDocViaRepo: Boolean = false
  ): TestUser {
    val token = FakeJwtGenerator.createFakeGoogleIdToken(name, email)
    FirebaseEmulator.createGoogleUser(token)
    val credential = GoogleAuthProvider.getCredential(token, null)

    val authResult: AuthResult = FirebaseEmulator.auth.signInWithCredential(credential).await()
    val firebaseUser = authResult.user ?: error("User should not be null after sign-in")

    // Optionally create the Firestore user doc via repository
    if (createDocViaRepo) {
      repositoryUser.getCurrentUser()
    }

    return TestUser(uid = firebaseUser.uid, credential = credential)
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

      // First call: network enabled, doc created & cached
      val userOnline = repositoryUser.getCurrentUser()
      assertEquals("cache@example.com", userOnline.email)

      // Now disable network
      FirebaseEmulator.firestore.disableNetwork().await()

      // Act: call again while offline, should return from cache (DEFAULT source)
      val userOffline = repositoryUser.getCurrentUser()

      // Assert
      assertEquals(uid, userOffline.uid)
      assertEquals("cache@example.com", userOffline.email)

      // Cleanup
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

    // Ensure user doc exists (created by repository)
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
  fun sendFriendRequest_addsPendingFriendToBothUsers() = runBlocking {
    // Arrange: create A (current user)
    val (uidA, credentialA) =
        createGoogleUserAndSignIn(
            name = "User A", email = "userA@example.com", createDocViaRepo = true)

    // Create B (need uid + doc as well)
    val (uidB, credentialB) =
        createGoogleUserAndSignIn(
            name = "User B", email = "userB@example.com", createDocViaRepo = true)

    // Back to A to send request (A is the caller)
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()

    // Act
    repositoryUser.sendFriendRequest(fromUid = uidA, toUid = uidB)

    // Assert: A has a PENDING friend entry for B
    val docA = FirebaseEmulator.firestore.collection("users").document(uidA).get().await()
    val friendsA = docA.get("friends") as? List<*> ?: emptyList<Any>()
    assertEquals(1, friendsA.size)
    val friendA = friendsA.first() as Map<*, *>
    assertEquals(uidB, friendA["uid"])
    assertEquals("PENDING_OUTGOING", friendA["status"])

    // Assert: B also has a PENDING entry for A
    val docB = FirebaseEmulator.firestore.collection("users").document(uidB).get().await()
    val friendsB = docB.get("friends") as? List<*> ?: emptyList<Any>()
    assertEquals(1, friendsB.size)
    val friendB = friendsB.first() as Map<*, *>
    assertEquals(uidA, friendB["uid"])
    assertEquals("PENDING_INCOMING", friendB["status"])
  }

  @Test
  fun acceptFriendRequest_updatesStatusToAcceptedForBothUsers() = runBlocking {
    // Arrange: create A and B
    val (uidA, credentialA) =
        createGoogleUserAndSignIn(
            name = "User A2", email = "userA2@example.com", createDocViaRepo = true)
    val (uidB, credentialB) =
        createGoogleUserAndSignIn(
            name = "User B2", email = "userB2@example.com", createDocViaRepo = true)

    // A sends a friend request to B → seeds PENDING on both sides
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    repositoryUser.sendFriendRequest(fromUid = uidA, toUid = uidB)

    // Now B accepts A
    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    repositoryUser.acceptFriendRequest(currentUid = uidB, fromUid = uidA)

    // Assert: B's entry for A is ACCEPTED
    val docB = FirebaseEmulator.firestore.collection("users").document(uidB).get().await()
    val friendsB = docB.get("friends") as? List<*> ?: emptyList<Any>()
    assertEquals(1, friendsB.size)
    val friendB = friendsB.first() as Map<*, *>
    assertEquals(uidA, friendB["uid"])
    assertEquals("ACCEPTED", friendB["status"])

    // Assert: A's entry for B is also ACCEPTED
    val docA = FirebaseEmulator.firestore.collection("users").document(uidA).get().await()
    val friendsA = docA.get("friends") as? List<*> ?: emptyList<Any>()
    assertEquals(1, friendsA.size)
    val friendA = friendsA.first() as Map<*, *>
    assertEquals(uidB, friendA["uid"])
    assertEquals("ACCEPTED", friendA["status"])
  }

  @Test
  fun removeFriend_removesFriendFromBothUsers() = runBlocking {
    // Arrange: create A and B
    val (uidA, credentialA) =
        createGoogleUserAndSignIn(
            name = "User A3", email = "userA3@example.com", createDocViaRepo = true)
    val (uidB, credentialB) =
        createGoogleUserAndSignIn(
            name = "User B3", email = "userB3@example.com", createDocViaRepo = true)

    // A sends request to B and B accepts → ACCEPTED on both sides
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    repositoryUser.sendFriendRequest(fromUid = uidA, toUid = uidB)

    FirebaseEmulator.auth.signInWithCredential(credentialB).await()
    repositoryUser.acceptFriendRequest(currentUid = uidB, fromUid = uidA)

    // Back to A to remove B
    FirebaseEmulator.auth.signInWithCredential(credentialA).await()
    repositoryUser.removeFriend(uid = uidA, friendUid = uidB)

    // Assert: A has no friends
    val docA = FirebaseEmulator.firestore.collection("users").document(uidA).get().await()
    val friendsA = docA.get("friends") as? List<*> ?: emptyList<Any>()
    assertTrue(friendsA.isEmpty())

    // Assert: B also has no friends
    val docB = FirebaseEmulator.firestore.collection("users").document(uidB).get().await()
    val friendsB = docB.get("friends") as? List<*> ?: emptyList<Any>()
    assertTrue(friendsB.isEmpty())
  }

  @Test
  fun getUserByUid_returnsUserWhenDocumentExists() = runBlocking {
    // Arrange: create and sign in a user, and create the Firestore doc via repository
    val (uid, _) =
        createGoogleUserAndSignIn(
            name = "Lookup User", email = "lookup@example.com", createDocViaRepo = true)

    // Act
    val user = repositoryUser.getUserByUid(uid)

    // Assert
    assertNotNull(user, "Expected non-null user when Firestore document exists")
    assertEquals(uid, user!!.uid)
    assertEquals("lookup@example.com", user.email)
    assertEquals("Lookup User", user.name)
  }

  @Test
  fun getUserByUid_returnsNullWhenDocumentDoesNotExist() = runBlocking {
    // Arrange: pick a UID that definitely has no document
    val missingUid = "non_existing_uid"

    // Act
    val user = repositoryUser.getUserByUid(missingUid)

    // Assert
    assertNull(user, "Expected null when Firestore document does not exist")
  }

  @Test
  fun getUserByUid_returnsNullWhenFirestoreThrows() = runBlocking {
    // Arrange: disable network so get() will fail and throw inside await()
    FirebaseEmulator.firestore.disableNetwork().await()
    val uid = "offline_uid"

    // Act
    val user = repositoryUser.getUserByUid(uid)

    // Assert: exception should be caught and translated to null
    assertNull(user, "Expected null when Firestore throws an exception")

    // Cleanup
    FirebaseEmulator.firestore.enableNetwork().await()

    Unit
  }

  @Test
  fun getUserByNameOrEmail_findsUsersByNameOrEmail() = runBlocking {
    // Arrange: create three real users via auth + repository
    val (uidAlex, credAlex) =
        createGoogleUserAndSignIn(
            name = "Alex Müller", email = "alex@example.com", createDocViaRepo = true)

    val (uidAlice, credAlice) =
        createGoogleUserAndSignIn(
            name = "Alice Meyer", email = "alice@example.com", createDocViaRepo = true)

    val (uidBob, credBob) =
        createGoogleUserAndSignIn(
            name = "Bob Dupont", email = "bob@example.com", createDocViaRepo = true)

    // Sign back in as Alex (any authenticated user is fine for reads)
    FirebaseEmulator.auth.signInWithCredential(credAlex).await()

    // Act + Assert: search by name
    val byName = repositoryUser.getUserByNameOrEmail("Alex")
    assertTrue(byName.any { it.uid == uidAlex })
    // Bob must not appear
    assertFalse(byName.any { it.uid == uidBob })

    // Act + Assert: search by email
    val byEmail = repositoryUser.getUserByNameOrEmail("alice@example.com")
    assertEquals(1, byEmail.size)
    assertEquals(uidAlice, byEmail.first().uid)
  }

  @Test
  fun getUserByNameOrEmail_returnsEmptyListForNoMatchOrBlankQuery() = runBlocking {
    // Arrange: one existing user
    val (uidCharlie, credCharlie) =
        createGoogleUserAndSignIn(
            name = "Charlie Example", email = "charlie@example.com", createDocViaRepo = true)

    // Sign in as Charlie
    FirebaseEmulator.auth.signInWithCredential(credCharlie).await()

    // Act: queries that should not match
    val noMatch = repositoryUser.getUserByNameOrEmail("does-not-exist")
    val blankQuery = repositoryUser.getUserByNameOrEmail("")

    // Assert
    assertTrue(noMatch.isEmpty(), "Expected empty list for non-matching query")
    assertTrue(blankQuery.isEmpty(), "Expected empty list for blank query")
  }

  @After
  override fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }
}
