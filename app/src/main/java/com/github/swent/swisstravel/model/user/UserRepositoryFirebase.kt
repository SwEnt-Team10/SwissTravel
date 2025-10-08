package com.github.swent.swisstravel.model.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

  override suspend fun getCurrentUser(): User {
    val firebaseUser = auth.currentUser ?: throw IllegalStateException("User is not logged in")
    val uid = firebaseUser.uid

    val doc = db.collection("users").document(uid).get().await()

    if (doc.exists()) {
      return createUser(doc, uid)
    } else {
      return retrieveUser(firebaseUser, uid)
    }
  }

  private fun createUser(doc: DocumentSnapshot, uid: String): User {
    return User(
        uid = uid,
        name = doc.getString("name") ?: "",
        email = doc.getString("email") ?: "",
        profilePicUrl = doc.getString("profilePicUrl") ?: "",
        preferences =
            (doc.get("preferences") as? List<*>)?.mapNotNull { str ->
              enumValues<UserPreference>().find { it.displayString() == str }
            } ?: emptyList())
  }

  private fun retrieveUser(firebaseUser: FirebaseUser, uid: String): User {
    val newUser =
        User(
            uid = uid,
            name = firebaseUser.displayName.orEmpty(),
            email = firebaseUser.email.orEmpty(),
            profilePicUrl = firebaseUser.photoUrl?.toString().orEmpty(),
            preferences = emptyList())
    db.collection("users").document(uid).set(newUser)
    return newUser
  }

  override suspend fun updateUserPreferences(uid: String, preferences: List<String>) {
    val userDoc = db.collection("users").document(uid).get().await()
    if (userDoc.exists()) {
      db.collection("users").document(uid).update("preferences", preferences).await()
    } else {
      throw IllegalStateException("User document does not exist for uid: $uid")
    }
  }
}
