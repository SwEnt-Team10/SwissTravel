package com.github.swent.swisstravel.utils

import org.junit.After
import org.junit.Before

open class FirestoreSwissTravelTest : SwissTravelTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
