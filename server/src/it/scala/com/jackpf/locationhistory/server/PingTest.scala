package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.beacon_service.{PingRequest, PingResponse}
import com.jackpf.locationhistory.server.testutil.IntegrationTest

class PingTest extends IntegrationTest {
  "Ping" should {
    "accept a ping request" >> in(new IntegrationContext {}) { context =>
      val request = PingRequest()

      val response = context.client.ping(request)

      response === PingResponse(message = "pong")
    }
  }
}
