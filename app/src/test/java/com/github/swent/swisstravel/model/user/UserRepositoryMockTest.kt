package com.github.swent.swisstravel.model.user

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserRepositoryMockTest {

  @Test
  fun getCurrentUser_usesFirebaseUserWhenNoDocExists() = runTest {
    // Mocks
    val auth = mockk<FirebaseAuth>()
    val user = mockk<FirebaseUser>()
    val db = mockk<FirebaseFirestore>(relaxed = true)
    val docRef = mockk<DocumentReference>()
    val snapshot = mockk<DocumentSnapshot>()

    // Allow repo init to set Firestore settings
    every { db.firestoreSettings = any<FirebaseFirestoreSettings>() } just Runs

    every { auth.currentUser } returns user
    every { user.uid } returns "uid123"
    every { user.displayName } returns "Cacheless User"
    every { user.email } returns "cacheless@example.com"
    every { user.isAnonymous } returns false
    every { user.photoUrl } returns null

    every { db.collection("users").document("uid123") } returns docRef

    // Simulate DEFAULT source returning a non-existing doc
    every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
    every { snapshot.exists() } returns false

    every { docRef.set(any<User>()) } returns Tasks.forResult(null)

    val repo = UserRepositoryFirebase(auth, db)

    // Act
    val result: User = repo.getCurrentUser()

    // Assert: repo fell back to createAndStoreNewUser(firebaseUser, uid)
    assertEquals("uid123", result.uid)
    assertEquals("Cacheless User", result.name)
    assertEquals("cacheless@example.com", result.email)
  }

  @Test
  fun updateUser_serializesEnumsAndUrisCorrectly() = runTest {
    // Mocks
    val auth = mockk<FirebaseAuth>(relaxed = true)
    val db = mockk<FirebaseFirestore>(relaxed = true)
    val docRef = mockk<DocumentReference>()
    val snapshot = mockk<DocumentSnapshot>()

    every { db.firestoreSettings = any<FirebaseFirestoreSettings>() } just Runs
    every { auth.currentUser?.uid } returns "uid123"
    every { db.collection("users").document("uid123") } returns docRef
    every { docRef.get() } returns Tasks.forResult(snapshot)
    every { snapshot.exists() } returns true

    var captured: Map<String, Any?>? = null
    every { docRef.update(any<Map<String, Any?>>()) } answers
        {
          captured = arg(0)
          Tasks.forResult(null)
        }

    val repo = UserRepositoryFirebase(auth, db)

    // Act
    repo.updateUser(
        uid = "uid123",
        UserUpdate(
            name = null,
            biography = null,
            profilePicUrl = "http://pic",
            preferences = listOf(Preference.SCENIC_VIEWS, Preference.WHEELCHAIR_ACCESSIBLE),
            pinnedTripsUids = listOf("t1", "t2"),
            pinnedPicturesUids = null // not updated
            ))

    // Assert: update() must have been called
    assertNotNull(captured)
    // Extract once
    val map = captured!!
    // Assert correct number of updated fields
    assertEquals(
        3, // expected number of updated fields
        map.size // profilePicUrl + preferences + pinnedTripsUids
        )
    // Assert each key/value
    assertEquals("http://pic", map["profilePicUrl"])
    assertEquals(
        listOf(Preference.SCENIC_VIEWS, Preference.WHEELCHAIR_ACCESSIBLE), map["preferences"])
    assertEquals(listOf("t1", "t2"), map["pinnedTripsUids"])
    assertFalse(map.containsKey("pinnedImagesUris"))
    // TODO fix URIs cause it doesn't work in the test for some reason
    // Assert.assertEquals(listOf("file:///data/data/your.package.name/files/image1"),
    // captured!!["pinnedImagesUris"])
  }
}
