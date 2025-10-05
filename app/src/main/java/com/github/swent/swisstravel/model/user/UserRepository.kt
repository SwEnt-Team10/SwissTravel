package com.github.swent.swisstravel.model.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getCurrentUser(): User {
        // Simulate a delay to mimic network
        kotlinx.coroutines.delay(500)

        return User(
            uid = "test_uid",
            name = "Test User",
            email = "test@example.com",
            profilePicUrl = "https://i.pravatar.cc/300", // random avatar generator
            preferences = listOf(UserPreference.HIKING, UserPreference.MUSEUMS)
        )
    }

//    suspend fun getCurrentUser(): User {
//        val firebaseUser = auth.currentUser ?: throw IllegalStateException("User is not logged in")
//        val uid = firebaseUser.uid
//
//        val doc = db.collection("users").document(uid).get().await()
//
//        return if (doc.exists()) {
//            User(
//                uid = uid,
//                name = doc.getString("name") ?: firebaseUser.displayName.orEmpty(),
//                email = doc.getString("email") ?: firebaseUser.email.orEmpty(),
//                profilePicUrl = doc.getString("profilePicUrl") ?: firebaseUser.photoUrl?.toString().orEmpty(),
//                preferences = (doc.get("preferences") as? List<*>)?.mapNotNull { str ->
//                  enumValues<UserPreference>().find { it.displayString() == str }
//}                 ?: emptyList()
//            )
//        } else {
//            val newUser = User(
//                uid = uid,
//                name = firebaseUser.displayName.orEmpty(),
//                email = firebaseUser.email.orEmpty(),
//                profilePicUrl = firebaseUser.photoUrl?.toString().orEmpty(),
//                preferences = emptyList()
//            )
//            db.collection("users").document(uid).set(newUser)
//            newUser
//        }
//    }

    suspend fun updateUserPreferences(uid: String, preferences: List<String>) {
        db.collection("users").document(uid)
            .update("preferences", preferences)
            .await()
    }
}