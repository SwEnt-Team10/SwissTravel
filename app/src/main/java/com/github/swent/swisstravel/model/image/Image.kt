package com.github.swent.swisstravel.model.image

/**
 * Represents an image.
 *
 * @property uid The unique identifier of the image.
 * @property ownerId The unique identifier of the owner of the image.
 * @property base64 The base64-encoded representation of the image.
 */
data class Image(val uid: String, val ownerId: String, val base64: String)
