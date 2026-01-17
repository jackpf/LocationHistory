package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.admin_service.{ListDevicesRequest, LoginRequest}
import com.jackpf.locationhistory.server.testutil.{GrpcMatchers, IntegrationTest, TestServer}
import io.grpc.Status.Code

class AdminTest extends IntegrationTest with GrpcMatchers {
  "Admin" should {
    "login endpoint" >> {
      "login with correct password" >> in(new IntegrationContext {}) { context =>
        val request = LoginRequest(password = TestServer.TestAdminPassword)

        val response = context.unauthenticatedAdminClient.login(request)

        response.token must not(beEmpty)
      }

      "fail login with incorrect password" >> in(new IntegrationContext {}) { context =>
        val request = LoginRequest(password = "wrong-password")

        context.unauthenticatedAdminClient.login(request) must throwAGrpcRuntimeException(
          Code.UNAUTHENTICATED,
          "Invalid password"
        )
      }
    }

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
        override lazy val tokenDuration: Long = -1L
      }) { context =>
        val request = ListDevicesRequest()

        context.adminClient.listDevices(request) must throwAGrpcRuntimeException(
          Code.UNAUTHENTICATED,
          "Authentication failure"
        )
      }
    }
  }
}
