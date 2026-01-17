package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.admin_service.{
  ApproveDeviceRequest,
  ApproveDeviceResponse,
  DeleteDeviceRequest,
  DeleteDeviceResponse,
  ListDevicesRequest,
  LoginRequest,
  NotificationType,
  SendNotificationRequest,
  SendNotificationResponse
}
import com.jackpf.locationhistory.beacon_service.{
  RegisterDeviceRequest,
  RegisterDeviceResponse,
  RegisterPushHandlerRequest,
  RegisterPushHandlerResponse
}
import com.jackpf.locationhistory.common.{Device, PushHandler}
import com.jackpf.locationhistory.server.testutil.{GrpcMatchers, IntegrationTest, TestServer}
import io.grpc.Status.Code
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future
import scala.util.Success

class AdminTest extends IntegrationTest with GrpcMatchers {
  trait RegisteredDeviceContext extends IntegrationContext {
    lazy val device: Device = Device(id = "123")

    val registerDeviceRequest: RegisterDeviceRequest =
      RegisterDeviceRequest(device = Some(device))
    val registerDeviceResult: RegisterDeviceResponse =
      client.registerDevice(registerDeviceRequest)
    registerDeviceResult.success === true
  }

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

    "approve device endpoint" >> {
      "approve a pending device" >> in(new RegisteredDeviceContext {}) { context =>
        val request = ApproveDeviceRequest(deviceId = context.device.id)

        val response = context.adminClient.approveDevice(request)

        response === ApproveDeviceResponse(success = true)
      }

      "fail to approve non-existing device" >> in(new IntegrationContext {}) { context =>
        val request = ApproveDeviceRequest(deviceId = "non-existing")

        context.adminClient.approveDevice(request) must throwAGrpcRuntimeException(
          Code.NOT_FOUND,
          "Device non-existing does not exist"
        )
      }

      "fail to approve already approved device" >> in(new RegisteredDeviceContext {}) { context =>
        // First approval should succeed
        context.adminClient.approveDevice(
          ApproveDeviceRequest(deviceId = context.device.id)
        ) === ApproveDeviceResponse(success = true)

        // Second approval should fail
        context.adminClient.approveDevice(
          ApproveDeviceRequest(deviceId = context.device.id)
        ) must throwAGrpcRuntimeException(
          Code.PERMISSION_DENIED,
          "Device 123 has an invalid state"
        )
      }
    }

    "delete device endpoint" >> {
      "delete an existing device" >> in(new RegisteredDeviceContext {}) { context =>
        val request = DeleteDeviceRequest(deviceId = context.device.id)

        val response = context.adminClient.deleteDevice(request)

        response === DeleteDeviceResponse(success = true)
      }

      "fail to delete non-existing device" >> in(new IntegrationContext {}) { context =>
        val request = DeleteDeviceRequest(deviceId = "non-existing")

        context.adminClient.deleteDevice(request) must throwAGrpcRuntimeException(
          Code.NOT_FOUND,
          "Device non-existing does not exist"
        )
      }
    }

    "send notification endpoint" >> {
      trait ApprovedDeviceContext extends RegisteredDeviceContext {
        adminClient.approveDevice(
          ApproveDeviceRequest(deviceId = device.id)
        ) === ApproveDeviceResponse(success = true)
      }

      trait DeviceWithPushHandlerContext extends ApprovedDeviceContext {
        val pushHandler: PushHandler = PushHandler(name = "test-handler", url = "http://test.url")

        client.registerPushHandler(
          RegisterPushHandlerRequest(deviceId = device.id, pushHandler = Some(pushHandler))
        ) === RegisterPushHandlerResponse(success = true)

        // Mock the notification service to return success
        when(
          IntegrationTest.notificationService
            .sendNotification(any(), any())(using any())
        ).thenReturn(Future.successful(Success(())))
      }

      "send a notification" >> in(new DeviceWithPushHandlerContext {}) { context =>
        val request = SendNotificationRequest(
          deviceId = context.device.id,
          notificationType = NotificationType.REQUEST_BEACON
        )

        val response = context.adminClient.sendNotification(request)

        response === SendNotificationResponse(success = true)
      }

      "fail to send notification for non-existing device" >> in(new IntegrationContext {}) {
        context =>
          val request = SendNotificationRequest(
            deviceId = "non-existing",
            notificationType = NotificationType.REQUEST_BEACON
          )

          context.adminClient.sendNotification(request) must throwAGrpcRuntimeException(
            Code.NOT_FOUND,
            "Device non-existing does not exist"
          )
      }

      "fail to send notification when no push handler registered" >> in(
        new ApprovedDeviceContext {}
      ) { context =>
        val request = SendNotificationRequest(
          deviceId = context.device.id,
          notificationType = NotificationType.REQUEST_BEACON
        )

        context.adminClient.sendNotification(request) must throwAGrpcRuntimeException(
          Code.INVALID_ARGUMENT,
          "Device 123 has no registered push handler"
        )
      }
    }
  }
}
