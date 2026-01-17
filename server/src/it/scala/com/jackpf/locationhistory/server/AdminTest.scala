package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.admin_service.ListDevicesRequest
import com.jackpf.locationhistory.server.testutil.{GrpcMatchers, IntegrationTest}
import io.grpc.Status.Code

class AdminTest extends IntegrationTest with GrpcMatchers {
  "Admin" should {
    "list devices endpoint" >> {
      "list devices" >> in(new IntegrationContext {}) { context =>
        val request = ListDevicesRequest()

        val response = context.adminClient.listDevices(request)

        response.devices === Seq.empty
      }

      "fail with invalid token" >> in(new IntegrationContext {
        override lazy val token: String = "invalid"
      }) { context =>
        val request = ListDevicesRequest()

        context.adminClient.listDevices(request) must throwAGrpcRuntimeException(
          Code.UNAUTHENTICATED,
          "Authentication failure"
        )
      }

      "fail with expired token" >> in(new IntegrationContext {
        override lazy val tokenDuration: Long = 1L
      }) { context =>
        val request = ListDevicesRequest()

        Thread.sleep(2000) // Expire our 1s token

        context.adminClient.listDevices(request) must throwAGrpcRuntimeException(
          Code.UNAUTHENTICATED,
          "Authentication failure: The token is expired since"
        )
      }
    }
  }
}
