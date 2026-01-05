package com.jackpf.locationhistory.server.grpc

import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}

class AuthenticationManagerTest extends DefaultSpecification {
  trait Context extends DefaultScope {
    lazy val adminPassword: String = "mock-password"
    val authenticationManager: AuthenticationManager = new AuthenticationManager(adminPassword)
  }

  "Authentication manager" should {
    "compare correct password" >> in(new Context {}) { context =>
      context.authenticationManager.isValidPassword("mock-password") must beTrue
    }

    "compare incorrect password" >> in(new Context {}) { context =>
      context.authenticationManager.isValidPassword("wrong-password") must beFalse
    }
  }
}
