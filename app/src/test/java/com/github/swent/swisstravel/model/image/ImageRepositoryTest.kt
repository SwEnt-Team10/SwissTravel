package com.github.swent.swisstravel.model.image

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Some of these tests were made with the help of AI */
class ImageRepositoryFirebaseTest {
  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocumentRef: DocumentReference
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private lateinit var repo: ImageRepositoryFirebase

  @Before
  fun setup() {
    mockDb = mockk()
    mockCollection = mockk()
    mockDocumentRef = mockk()
    mockAuth = mockk()
    mockUser = mockk()

    every { mockDb.collection(IMAGES_COLLECTION_PATH) } returns mockCollection
    repo = ImageRepositoryFirebase(mockDb, mockAuth)
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  // addImage tests
  @Test
  fun addImageSuccessfullyAddsImageWhenUserIsLoggedIn() = runTest {
    // Given
    val base64 = "validBase64String"
    val newUid = "new-image-uid"
    val userId = "user-123"

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns userId
    every { mockCollection.document() } returns mockDocumentRef
    every { mockDocumentRef.id } returns newUid

    // We capture the slot to verify what was actually sent to Firestore
    val slot = slot<Image>()
    every { mockCollection.document(newUid).set(capture(slot)) } returns Tasks.forResult(null)

    // When
    repo.addImage(base64)

    // Then
    verify { mockCollection.document(newUid).set(any()) }
    assertEquals(newUid, slot.captured.uid)
    assertEquals(userId, slot.captured.ownerId)
    assertEquals(base64, slot.captured.base64)
  }

  @Test
  fun addImageThrowsExceptionWhenUserIsNotLoggedIn() = runTest {
    // Given
    every { mockAuth.currentUser } returns null
    every { mockCollection.document() } returns mockDocumentRef
    every { mockDocumentRef.id } returns "some-id"

    // Then
    assertFailsWith<Exception> { repo.addImage("base64") }
  }

  // getImage tests
  @Test
  fun getImageReturnsImageFromNetworkWhenAvailable() = runTest {
    // Given
    val imageUid = "img-123"
    val docSnapshot = mockk<DocumentSnapshot>()

    // Mock successful network fetch
    every { mockCollection.document(imageUid).get() } returns Tasks.forResult(docSnapshot)

    // Mock document data
    every { docSnapshot.id } returns imageUid
    every { docSnapshot.getString("ownerId") } returns "owner-1"
    every { docSnapshot.getString("base64") } returns "base64-data"

    // When
    val result = repo.getImage(imageUid)

    // Then
    assertEquals(imageUid, result.uid)
    assertEquals("owner-1", result.ownerId)
    assertEquals("base64-data", result.base64)
  }

  @Test
  fun getImageFallsBackToCacheWhenNetworkFails() = runTest {
    // Given
    val imageUid = "img-offline"
    val docSnapshot = mockk<DocumentSnapshot>()

    // 1. Mock Network Failure
    every { mockCollection.document(imageUid).get() } returns
        Tasks.forException(Exception("Network error"))

    // 2. Mock Cache Success
    every { mockCollection.document(imageUid).get(Source.CACHE) } returns
        Tasks.forResult(docSnapshot)

    // Mock document data
    every { docSnapshot.id } returns imageUid
    every { docSnapshot.getString("ownerId") } returns "owner-1"
    every { docSnapshot.getString("base64") } returns "cached-base64"

    // When
    val result = repo.getImage(imageUid)

    // Then
    assertEquals("cached-base64", result.base64)
    // Verify we actually tried the cache
    verify { mockCollection.document(imageUid).get(Source.CACHE) }
  }

  @Test
  fun getImageThrowsExceptionWhenBothNetworkAndCacheFail() = runTest {
    // Given
    val imageUid = "img-ghost"

    // 1. Mock Network Failure
    every { mockCollection.document(imageUid).get() } returns
        Tasks.forException(Exception("Network fail"))

    // 2. Mock Cache Failure
    every { mockCollection.document(imageUid).get(Source.CACHE) } returns
        Tasks.forException(Exception("Cache fail"))

    // Then
    assertFailsWith<Exception> { repo.getImage(imageUid) }
  }

  @Test
  fun getImageThrowsExceptionWhenDataIsMalformed() = runTest {
    // Given
    val imageUid = "img-corrupt"
    val docSnapshot = mockk<DocumentSnapshot>()

    every { mockCollection.document(imageUid).get() } returns Tasks.forResult(docSnapshot)

    // Missing ownerId, so documentToImage returns null
    every { docSnapshot.id } returns imageUid
    every { docSnapshot.getString("ownerId") } returns null
    every { docSnapshot.getString("base64") } returns "data"

    // Then
    assertFailsWith<Exception> { repo.getImage(imageUid) }
  }

  // deleteImage tests
  @Test
  fun deleteImageCallsDeleteOnDocument() = runTest {
    // Given
    val imageUid = "img-delete-me"
    every { mockCollection.document(imageUid).delete() } returns Tasks.forResult(null)

    // When
    repo.deleteImage(imageUid)

    // Then
    verify { mockCollection.document(imageUid).delete() }
  }

  @Test
  fun deleteImagePropagatesExceptions() = runTest {
    // Given
    val imageUid = "img-fail-delete"
    every { mockCollection.document(imageUid).delete() } returns
        Tasks.forException(RuntimeException("Delete failed"))

    // Then
    assertFailsWith<RuntimeException> { repo.deleteImage(imageUid) }
  }
}
