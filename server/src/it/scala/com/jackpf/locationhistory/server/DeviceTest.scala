package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.admin_service.{ApproveDeviceRequest, ApproveDeviceResponse}
import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.common.*
import com.jackpf.locationhistory.server.testutil.{GrpcMatchers, IntegrationTest}
import io.grpc.Status.Code

class DeviceTest extends IntegrationTest with GrpcMatchers {
  "With no devices" should {
    "check a non-existing device" >> in(new IntegrationContext {}) { context =>
      val request = CheckDeviceRequest(deviceId = "123")

      val response = context.client.checkDevice(request)

      response === CheckDeviceResponse(status = DeviceStatus.DEVICE_UNKNOWN)
    }

    "register a device" >> in(new IntegrationContext {}) { context =>
      val request =
        RegisterDeviceRequest(device = Some(Device(id = "123")))

      val result = context.client.registerDevice(request)

      result === RegisterDeviceResponse(success = true, status = DeviceStatus.DEVICE_PENDING)
    }

    "fail registering an empty device" >> in(new IntegrationContext {}) { context =>
      val request =
        RegisterDeviceRequest(device = None)

      context.client.registerDevice(request) must throwAGrpcRuntimeException(
        Code.INVALID_ARGUMENT,
        "No device provided"
      )
    }

    "fail on registering a push handler" >> in(new IntegrationContext {}) { context =>
      context.client.registerPushHandler(
        RegisterPushHandlerRequest(
          deviceId = "123",
          pushHandler = Some(PushHandler(name = "ph", url = "phUrl"))
        )
      ) must throwAGrpcRuntimeException(
        Code.NOT_FOUND,
        "Device 123 does not exist"
      )
    }
  }

  trait RegisteredDeviceContext extends IntegrationContext {
    lazy val device = Device(id = "123")
    val registerDeviceRequest =
      RegisterDeviceRequest(device = Some(device))
    val registerDeviceResult: RegisterDeviceResponse =
      client.registerDevice(registerDeviceRequest)
    registerDeviceResult.success === true
  }

  "With registered device" should {
    "check the device with status pending" >> in(
      new RegisteredDeviceContext {}
    ) { context =>
      val request = CheckDeviceRequest(deviceId = context.device.id)

      val response = context.client.checkDevice(request)

      response === CheckDeviceResponse(status = DeviceStatus.DEVICE_PENDING)
    }

    "fail if re-registering device" >> in(
      new RegisteredDeviceContext {}
    ) { context =>
      context.client.registerDevice(context.registerDeviceRequest) must throwAGrpcRuntimeException(
        Code.ALREADY_EXISTS,
        "Device 123 is already registered"
      )
    }

    "not register a push handler on non-approved device" >> in(
      new RegisteredDeviceContext {
        override lazy val device = Device(id = "123")
      }
    ) { context =>
      context.client.registerPushHandler(
        RegisterPushHandlerRequest(
          deviceId = context.device.id,
          pushHandler = Some(PushHandler(name = "ph", url = "phUrl"))
        )
      ) must throwAGrpcRuntimeException(
        Code.PERMISSION_DENIED,
        "Device 123 has an invalid state; expected Pending but was Registered"
      )
    }

    "register a push handler on an approved device" >> in(
      new RegisteredDeviceContext {
        override lazy val device = Device(id = "123")
      }
    ) { context =>
      context.adminClient.approveDevice(
        ApproveDeviceRequest(deviceId = context.device.id)
      ) === ApproveDeviceResponse(success = true)

      val result = context.client.registerPushHandler(
        RegisterPushHandlerRequest(
          deviceId = context.device.id,
          pushHandler = Some(PushHandler(name = "ph", url = "phUrl"))
        )
      )

      result === RegisterPushHandlerResponse(success = true)
    }
  }
}
