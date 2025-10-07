package com.android.swisstravel.model.user

import com.android.swisstravel.utils.FakeJwtGenerator
import com.android.swisstravel.utils.FirebaseEmulator
import com.android.swisstravel.utils.SwissTravelTest
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

class UserRepositoryEmulatorTest : SwissTravelTest() {
  private lateinit var repository: UserRepositoryFirebase

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.clearFirestoreEmulator()
    repository = UserRepositoryFirebase(Firebase.auth, Firebase.firestore)
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

    // Act
    val user = repository.getCurrentUser()

    // Assert
    assertEquals(uid, user.uid)
    assertEquals(email, user.email)
    assertTrue(user.preferences.isEmpty())

    // Verify Firestore document now exists
    val doc = Firebase.firestore.collection("users").document(uid).get().await()
    assertTrue(doc.exists())
  }

  @Test
  fun getCurrentUser_returnsExistingUserIfExists() = runBlocking {
    // Arrange
    val fakeIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("Existing User", "existing@example.com")
    FirebaseEmulator.createGoogleUser(fakeIdToken)
    FirebaseEmulator.auth.signInAnonymously().await()

    val uid = Firebase.auth.currentUser!!.uid
    val existingData =
        mapOf(
            "name" to "Saved User",
            "email" to "existing@example.com",
            "profilePicUrl" to "http://example.com/avatar.png",
            "preferences" to listOf("Hiking & Outdoor", "Skiing & Snow Sports"))
    Firebase.firestore.collection("users").document(uid).set(existingData).await()

    // Act
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
    FirebaseEmulator.auth.signInAnonymously().await()
    val uid = Firebase.auth.currentUser!!.uid

    // Act
    val newPrefs = listOf("City", "Nature", "Adventure")
    repository.updateUserPreferences(uid, newPrefs)

    // Assert
    val doc = Firebase.firestore.collection("users").document(uid).get().await()
    val storedPrefs = doc.get("preferences") as List<*>
    assertEquals(newPrefs, storedPrefs)
  }
}
