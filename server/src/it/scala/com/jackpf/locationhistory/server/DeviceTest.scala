package com.jackpf.locationhistory.server

import beacon.beacon_service.{CheckDeviceRequest, Device, DeviceStatus}
import com.jackpf.locationhistory.server.testutil.IntegrationTest
import io.grpc.StatusRuntimeException

class DeviceTest extends IntegrationTest {
  "Device" should {
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
            e.getStatus.getCode === io.grpc.Status.Code.INVALID_ARGUMENT
            e.getMessage must contain("No device provided")
          }
    }
  }
}
