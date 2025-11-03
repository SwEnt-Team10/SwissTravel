package com.github.swent.swisstravel.model.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class UserRepositoryFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

  init {
    db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
  }

  /**
   * Retrieves the current user from Firebase Authentication. If the user is not signed in, a guest
   * user is returned. If the user is signed in, their information is retrieved from Firestore. If
   * the user's information is not found in Firestore, a new user is created.
   */
  override suspend fun getCurrentUser(): User {
    val firebaseUser = auth.currentUser

    if (firebaseUser == null || firebaseUser.isAnonymous) {
      return User(
          uid = "guest",
          name = "Guest",
          email = "Not signed in",
          profilePicUrl = "",
          preferences = emptyList())
    }

    val uid = firebaseUser.uid
    return try {
      val doc = db.collection("users").document(uid).get(Source.SERVER).await()
      if (doc.exists()) createUser(doc, uid) else retrieveUser(firebaseUser, uid)
    } catch (e: Exception) {
      try {
        val cachedDoc = db.collection("users").document(uid).get(Source.CACHE).await()
        if (cachedDoc.exists()) {
          createUser(cachedDoc, uid)
        } else {
          User(
              uid = uid,
              name = firebaseUser.displayName ?: "Guest",
              email = firebaseUser.email ?: "Unknown",
              profilePicUrl = "",
              preferences = emptyList())
        }
      } catch (cacheException: Exception) {
        retrieveUser(firebaseUser, uid)
      }
    }
  }

  private fun createUser(doc: DocumentSnapshot, uid: String): User {
    val rawList = (doc.get("preferences") as? List<*>) ?: emptyList<Any>()
    val prefs = mutableListOf<Preference>()

    for (item in rawList) {
      val str = item?.toString() ?: continue
      val pref =
          try {
            Preference.valueOf(str)
          } catch (e: IllegalArgumentException) {
            // Fallback: if it's stored as the old displayString
            Preference.values().find { it.displayString() == str }
          }

      if (pref != null) prefs.add(pref)
    }

    return User(
        uid = uid,
        name = doc.getString("name") ?: "",
        email = doc.getString("email") ?: "",
        profilePicUrl = doc.getString("profilePicUrl") ?: "",
        preferences = prefs)
  }

  private suspend fun retrieveUser(firebaseUser: FirebaseUser, uid: String): User {
    val newUser =
        User(
            uid = uid,
            name = firebaseUser.displayName.orEmpty(),
            email = firebaseUser.email.orEmpty(),
            profilePicUrl = firebaseUser.photoUrl?.toString().orEmpty(),
            preferences = emptyList())
    db.collection("users").document(uid).set(newUser).await()
    return newUser
  }

  override suspend fun updateUserPreferences(uid: String, preferences: List<Preference>) {
    if (uid == "guest") return
    val docRef = db.collection("users").document(uid)
    val userDoc = docRef.get().await()
    if (!userDoc.exists()) throw IllegalStateException("User document does not exist for uid: $uid")

    val names = preferences.map { it.name }
    docRef.update("preferences", names).await()
  }
}
