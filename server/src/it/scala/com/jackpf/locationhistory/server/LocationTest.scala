package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.admin_service.{
  ApproveDeviceRequest,
  ApproveDeviceResponse,
  ListLocationsRequest
}
import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.common.*
import com.jackpf.locationhistory.server.testutil.{GrpcMatchers, IntegrationTest}

class LocationTest extends IntegrationTest with GrpcMatchers {
  "With no devices" should {
    // TODO
  }

  trait ApprovedDeviceContext extends IntegrationContext {
    lazy val device = Device(id = "123", publicKey = "xxx")

    val registerDeviceRequest =
      RegisterDeviceRequest(device = Some(device))
    val registerDeviceResult: RegisterDeviceResponse =
      client.registerDevice(registerDeviceRequest)
    registerDeviceResult.success === true

    val approveDeviceRequest = ApproveDeviceRequest(deviceId = device.id)
    val approveDeviceResponse: ApproveDeviceResponse =
      adminClient.approveDevice(approveDeviceRequest)
    approveDeviceResponse.success === true
  }

  "With approved device" should {
    "set a location" >> in(
      new ApprovedDeviceContext {}
    ) { context =>
      val timestamp = System.currentTimeMillis()
      val location = Location(lat = 0.1, lon = 0.2, accuracy = 0.3)
      val request =
        SetLocationRequest(
          timestamp = timestamp,
          deviceId = context.device.id,
          location = Some(location)
        )

      val response = context.client.setLocation(request)
      response === SetLocationResponse(success = true)

      // Check location the fetched location is actually correct
      val listLocationsRequest = ListLocationsRequest(deviceId = context.device.id)
      val listLocationsResponse = context.adminClient.listLocations(listLocationsRequest)

      listLocationsResponse.locations must haveSize(1)
      listLocationsResponse.locations.head === StoredLocation(
        location = Some(location),
        timestamp = timestamp
      )
    }
  }
}
