package com.github.swent.swisstravel.model.user

import com.github.swent.swisstravel.utils.FakeJwtGenerator
import com.github.swent.swisstravel.utils.FirebaseEmulator
import com.github.swent.swisstravel.utils.SwissTravelTest
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserRepositoryEmulatorTest : SwissTravelTest() {
  private lateinit var repository: UserRepositoryFirebase

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearFirestoreEmulator()
    repository = UserRepositoryFirebase(FirebaseEmulator.auth, FirebaseEmulator.firestore)
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
    val user = repository.getCurrentUser()

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
            "preferences" to listOf("Hiking", "Sports"))
    FirebaseEmulator.firestore.collection("users").document(uid).set(existingData).await()

    // TripActivity
    val user = repository.getCurrentUser()

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

    repository.getCurrentUser()

    // TripActivity
    val newPrefs = listOf(Preference.URBAN, Preference.SCENIC_VIEWS, Preference.NIGHTLIFE)
    repository.updateUserPreferences(uid, newPrefs)

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
    val user = repository.getCurrentUser()

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
              "preferences" to listOf("Museums"))
      FirebaseEmulator.firestore.collection("users").document(uid).set(cachedData).await()

      // Build repo using same Firestore but simulate network/server failure
      val repo = UserRepositoryFirebase(FirebaseEmulator.auth, Firebase.firestore)
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
    repository.updateUserPreferences(uid, listOf(Preference.MUSEUMS))
  }

  @Test
  fun updateUserPreferences_doesNothingForGuest() = runBlocking {
    // TripActivity
    repository.updateUserPreferences("guest", listOf(Preference.SCENIC_VIEWS))
    // Assert — should not throw
    assertTrue(true)
  }

  @After
  override fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }
}
