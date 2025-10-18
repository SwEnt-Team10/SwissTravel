package com.github.swent.swisstravel.model.user

import kotlin.test.Test
import kotlin.test.assertNotNull

class PreferenceTest {
  @Test
  fun `toSwissTourismFacet covers all preferences`() {
    Preference.values().forEach {
      val result = it.toSwissTourismFacet()
      assertNotNull(result)
    }
  }

  @Test
  fun `toSwissTourismFacetFilter covers all preferences`() {
    Preference.values().forEach {
      val result = it.toSwissTourismFacetFilter()
      assertNotNull(result)
    }
  }
}
