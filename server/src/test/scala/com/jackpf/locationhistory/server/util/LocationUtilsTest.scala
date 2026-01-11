package com.jackpf.locationhistory.server.util

import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}

class LocationUtilsTest extends DefaultSpecification {
  trait Context extends DefaultScope

  "Location utils" should {
    "detect duplicate locations" >> in(new Context {}) { _ =>
      LocationUtils.isDuplicate(
        MockModels.location(),
        123L,
        MockModels.storedLocation()
      ) must beTrue
    }

    "detect non-duplicate locations" >> in(new Context {}) { _ =>
      LocationUtils.isDuplicate(
        MockModels.location(lat = 888.0, lon = 999.0),
        123L,
        MockModels.storedLocation()
      ) must beFalse
    }
  }
}
