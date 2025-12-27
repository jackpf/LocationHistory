package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.beacon_service.PingRequest
import com.jackpf.locationhistory.server.testutil.IntegrationTest

class PingTest extends IntegrationTest {
  "Ping" should {
    "accept a ping request" >> in(new IntegrationContext {}) { context =>
      val request = PingRequest()

      val response = context.client.ping(request)

      response.message must beEqualTo("pong")
    }
  }
}
