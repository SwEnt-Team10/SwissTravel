package com.android.swisstravel.model.user

import com.github.swent.swisstravel.model.user.User
import com.github.swent.swisstravel.model.user.UserRepositoryFirebase
import com.github.swent.swisstravel.model.user.displayString
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserRepositoryMockTest {

  @Test
  fun getCurrentUser_usesCacheAndFallsBackToDefaultWhenCacheDocMissing() = runTest {
    // Mocks
    val auth = mockk<FirebaseAuth>()
    val user = mockk<FirebaseUser>()
    val db = mockk<FirebaseFirestore>(relaxed = true)
    val docRef = mockk<DocumentReference>()
    val cacheDoc = mockk<DocumentSnapshot>()

    // Allow repo init to set Firestore settings
    every { db.firestoreSettings = any<FirebaseFirestoreSettings>() } just Runs

    every { auth.currentUser } returns user
    every { user.uid } returns "uid123"
    every { user.displayName } returns "Cacheless User"
    every { user.email } returns "cacheless@example.com"
    // Make sure the user is not anonymous
    every {user.isAnonymous} returns false
    every { db.collection("users").document("uid123") } returns docRef

    // Simulate server failure, then cache hit with a doc that does NOT exist
    every { docRef.get(Source.SERVER) } returns Tasks.forException(Exception("server down"))
    every { docRef.get(Source.CACHE) } returns Tasks.forResult(cacheDoc)
    every { cacheDoc.exists() } returns false

    val repo = UserRepositoryFirebase(auth, db)

    // TripActivity
    val result: User = repo.getCurrentUser()

    // Assert -> falls back to default built from FirebaseUser (covers the else branch)
    assertEquals("uid123", result.uid)
    assertEquals("Cacheless User", result.name)
    assertEquals("cacheless@example.com", result.email)
    assertEquals(emptyList<String>(), result.preferences.map { it.displayString() })
  }
}
