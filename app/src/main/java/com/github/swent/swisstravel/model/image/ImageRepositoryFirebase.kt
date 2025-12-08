package com.github.swent.swisstravel.model.image

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

const val IMAGES_COLLECTION_PATH = "images"

class ImageRepositoryFirebase(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ImageRepository {
  val imageCollection = db.collection(IMAGES_COLLECTION_PATH)

  /**
   * Adds a new image to the repository.
   *
   * @return A new unique identifier.
   */
  override suspend fun addImage(base64: String) {
    val uid = imageCollection.document().id
    val image =
        Image(
            uid = uid,
            ownerId = auth.currentUser?.uid ?: throw Exception("User not logged in."),
            base64 = base64)
    imageCollection.document(uid).set(image).await()
  }

  /**
   * Retrieves an image by its unique identifier.
   *
   * @param imageUid The unique identifier of the image to retrieve.
   * @return The image with the specified identifier.
   * @throws Exception if the image is not found.
   */
  override suspend fun getImage(imageUid: String): Image {
    val document =
        try {
          imageCollection.document(imageUid).get().await()
        } catch (e: Exception) {
          Log.e("ImageRepositoryFirebase", "Error retrieving image", e)
          imageCollection.document(imageUid).get(Source.CACHE).await()
        }
    return documentToImage(document) ?: throw Exception("ImageRepositoryFirebase: Image not found")
  }

  /**
   * Deletes an image from the repository.
   *
   * @param imageUid The unique identifier of the image to delete.
   * @throws Exception if the image is not found.
   */
  override suspend fun deleteImage(imageUid: String) {
    try {
      imageCollection.document(imageUid).delete().await()
    } catch (e: Exception) {
      Log.e("ImageRepositoryFirebase", "Error deleting image", e)
    }
  }

  /**
   * Converts a Firestore document to an Image object.
   *
   * @param document The Firestore document to convert.
   * @return The Image object or null if conversion fails.
   */
  private fun documentToImage(document: DocumentSnapshot): Image? {
    return try {
      val uid = document.id
      val ownerId = document.getString("ownerId") ?: return null
      val base64 = document.getString("base64") ?: return null
      Image(uid, ownerId, base64)
    } catch (e: Exception) {
      Log.e("ImageRepositoryFirebase", "Error converting document to Image", e)
      null
    }
  }
}
