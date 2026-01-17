package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.beacon_service.{
  CheckDeviceRequest,
  CheckDeviceResponse,
  PingRequest,
  PingResponse,
  RegisterDeviceRequest,
  RegisterDeviceResponse
}
import com.jackpf.locationhistory.common.{Device, DeviceStatus}
import com.jackpf.locationhistory.server.testutil.IntegrationTest

class PingTest extends IntegrationTest {
  "Ping" should {
    "accept a ping request" >> in(new IntegrationContext {}) { context =>
      val request = PingRequest()

      val response = context.client.ping(request)

      response === PingResponse(message = "pong")
    }
  }

  "Beacon service public endpoints" should {
    "ping works without authentication" >> in(new IntegrationContext {
      override lazy val token: String = "invalid-token"
    }) { context =>
      val request = PingRequest()

      val response = context.client.ping(request)

      response === PingResponse(message = "pong")
    }

    "registerDevice works without authentication" >> in(new IntegrationContext {
      override lazy val token: String = "invalid-token"
    }) { context =>
      val request = RegisterDeviceRequest(device = Some(Device(id = "test-device")))

      val response = context.client.registerDevice(request)

      response === RegisterDeviceResponse(success = true, status = DeviceStatus.DEVICE_PENDING)
    }

    "checkDevice works without authentication" >> in(new IntegrationContext {
      override lazy val token: String = "invalid-token"
    }) { context =>
      val request = CheckDeviceRequest(deviceId = "any-device")

      val response = context.client.checkDevice(request)

      response === CheckDeviceResponse(status = DeviceStatus.DEVICE_UNKNOWN)
    }
  }
}
