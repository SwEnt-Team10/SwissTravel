package com.github.swent.swisstravel.model.trip

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object TripsRepositoryProvider {
  private val _repository: TripsRepository by lazy { TripsRepositoryFirestore(Firebase.firestore) }
  var repository: TripsRepository = _repository
}
