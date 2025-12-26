package com.jackpf.locationhistory.server

import beacon.beacon_service.*
import com.jackpf.locationhistory.server.testutil.IntegrationTest
import io.grpc.Status.Code
import io.grpc.StatusRuntimeException

class DeviceTest extends IntegrationTest {
  "With no devices" should {
    "check a non-existing device" >> in(new IntegrationContext {}) { context =>
      val request =
        CheckDeviceRequest(device = Some(Device(id = "123", publicKey = "xxx")))

      val response = context.client.checkDevice(request)

      response.status === DeviceStatus.DEVICE_UNKNOWN
    }

    "fail on checking an empty device" >> in(new IntegrationContext {}) {
      context =>
        val request =
          CheckDeviceRequest(device = None)

        context.client.checkDevice(request) must throwA[StatusRuntimeException]
          .like { case e =>
            e.getStatus.getCode === Code.INVALID_ARGUMENT
            e.getMessage must contain("No device provided")
          }
    }

    "register a device" >> in(new IntegrationContext {}) { context =>
      val request =
        RegisterDeviceRequest(device =
          Some(Device(id = "123", publicKey = "xxx"))
        )

      val result = context.client.registerDevice(request)

      result.success === true
    }

    "fail registering an empty device" >> in(new IntegrationContext {}) {
      context =>
        val request =
          RegisterDeviceRequest(device = None)

        context.client
          .registerDevice(request) must throwA[StatusRuntimeException]
          .like { case e =>
            e.getStatus.getCode === Code.INVALID_ARGUMENT
            e.getMessage must contain("No device provided")
          }
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
      val request =
        CheckDeviceRequest(device = Some(context.device))

      val response = context.client.checkDevice(request)

      response.status === DeviceStatus.DEVICE_PENDING
    }

    "fail if re-registering device" >> in(
      new RegisteredDeviceContext {}
    ) { context =>
      context.client.registerDevice(context.registerDeviceRequest) must throwA[
        StatusRuntimeException
      ]
        .like { case e =>
          e.getStatus.getCode === Code.ALREADY_EXISTS
          e.getMessage must contain("Device 123 is already registered")
        }
    }

    "fail if re-registering device with a different public key" >> in(
      new RegisteredDeviceContext {
        override lazy val device = Device(id = "123", publicKey = "yyy")
      }
    ) { context =>
      context.client.registerDevice(context.registerDeviceRequest) must throwA[
        StatusRuntimeException
      ]
        .like { case e =>
          e.getStatus.getCode === Code.ALREADY_EXISTS
          e.getMessage must contain("Device 123 is already registered")
        }
    }
  }
}
