package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}

class LocationRepoExtensionsTest extends DefaultSpecification {
  trait Context extends DefaultScope {}

  "LocationRepoExtensions" should {
    "foo" >> in(new Context {}) { context =>
      failure("TODO")
    }
  }
}
