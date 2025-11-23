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
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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
    every { docRef.get(Source.DEFAULT) } returns Tasks.forResult(snapshot)
    every { snapshot.exists() } returns false

    every { docRef.set(any<User>()) } returns Tasks.forResult(null)

    val repo = UserRepositoryFirebase(auth, db)

    // Act
    val result: User = repo.getCurrentUser()

    // Assert: repo fell back to createAndStoreNewUser(firebaseUser, uid)
    Assert.assertEquals("uid123", result.uid)
    Assert.assertEquals("Cacheless User", result.name)
    Assert.assertEquals("cacheless@example.com", result.email)
  }
}
