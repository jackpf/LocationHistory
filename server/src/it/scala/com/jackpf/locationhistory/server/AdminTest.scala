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

      "fail with invalid password" >> in(new IntegrationContext {
        override lazy val adminPassword: String = "invalid"
      }) { context =>
        val request = ListDevicesRequest()

        context.adminClient.listDevices(request) must throwAGrpcRuntimeException(
          Code.UNAUTHENTICATED,
          "Invalid password"
        )
      }
    }
  }
}
