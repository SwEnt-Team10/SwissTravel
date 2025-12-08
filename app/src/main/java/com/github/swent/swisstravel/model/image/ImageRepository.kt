package com.github.swent.swisstravel.model.image

/** Represents a repository that manages Images. */
interface ImageRepository {

  /**
   * Adds a new image to the repository.
   *
   * @return A new unique identifier.
   */
  suspend fun addImage(base64: String)

  /**
   * Retrieves an image by its unique identifier.
   *
   * @param imageUid The unique identifier of the image to retrieve.
   * @return The image with the specified identifier.
   * @throws Exception if the image is not found.
   */
  suspend fun getImage(imageUid: String): Image

  /**
   * Deletes an image from the repository.
   *
   * @param imageUid The unique identifier of the image to delete.
   * @throws Exception if the image is not found.
   */
  suspend fun deleteImage(imageUid: String)
}
