package com.jackpf.locationhistory.server

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
        RegisterDeviceRequest(device = Some(Device(id = "123", publicKey = "xxx")))

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
    lazy val device = Device(id = "123", publicKey = "xxx")
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

    "fail if re-registering device with a different public key" >> in(
      new RegisteredDeviceContext {
        override lazy val device = Device(id = "123", publicKey = "yyy")
      }
    ) { context =>
      context.client.registerDevice(context.registerDeviceRequest) must throwAGrpcRuntimeException(
        Code.ALREADY_EXISTS,
        "Device 123 is already registered"
      )
    }

    "register a push handler" >> in(
      new RegisteredDeviceContext {
        override lazy val device = Device(id = "123", publicKey = "yyy")
      }
    ) { context =>
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
