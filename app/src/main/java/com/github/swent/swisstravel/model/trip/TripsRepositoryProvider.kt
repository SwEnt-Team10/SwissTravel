package com.github.swent.swisstravel.model.trip

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object TripsRepositoryProvider {
  private val defaultRepository: TripsRepository by lazy {
    TripsRepositoryFirestore(Firebase.firestore)
  }
  private var _customRepository: TripsRepository? = null

  var repository: TripsRepository
    get() = _customRepository ?: defaultRepository
    set(value) {
      _customRepository = value
    }
}
