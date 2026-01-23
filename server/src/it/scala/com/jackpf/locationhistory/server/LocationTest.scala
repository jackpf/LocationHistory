package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.admin_service.{
  ApproveDeviceRequest,
  ApproveDeviceResponse,
  ListLocationsRequest
}
import com.jackpf.locationhistory.beacon_service.*
import com.jackpf.locationhistory.common.*
import com.jackpf.locationhistory.server.testutil.{GrpcMatchers, IntegrationTest}
import io.grpc.Status.Code

class LocationTest extends IntegrationTest with GrpcMatchers {
  "With no devices" should {
    "fail to set location for non-existing device" >> in(new IntegrationContext {}) { context =>
      val request = SetLocationRequest(
        timestamp = System.currentTimeMillis(),
        deviceId = "non-existing-device",
        location = Some(Location(lat = 0.1, lon = 0.2, accuracy = 0.3))
      )

      context.client.setLocation(request) must throwAGrpcRuntimeException(
        Code.NOT_FOUND,
        "Device non-existing-device does not exist"
      )
    }

    "fail to set location with no location provided" >> in(new IntegrationContext {}) { context =>
      val request = SetLocationRequest(
        timestamp = System.currentTimeMillis(),
        deviceId = "any-device",
        location = None
      )

      context.client.setLocation(request) must throwAGrpcRuntimeException(
        Code.INVALID_ARGUMENT,
        "No location provided"
      )
    }
  }

  trait PendingDeviceContext extends IntegrationContext {
    lazy val device = Device(id = "123")

    val registerDeviceRequest = RegisterDeviceRequest(device = Some(device))
    val registerDeviceResult: RegisterDeviceResponse =
      client.registerDevice(registerDeviceRequest)
    registerDeviceResult.success === true
  }

  "With pending device" should {
    "fail to set location for pending device" >> in(new PendingDeviceContext {}) { context =>
      val request = SetLocationRequest(
        timestamp = System.currentTimeMillis(),
        deviceId = context.device.id,
        location = Some(Location(lat = 0.1, lon = 0.2, accuracy = 0.3))
      )

      context.client.setLocation(request) must throwAGrpcRuntimeException(
        Code.PERMISSION_DENIED,
        "Device 123 has an invalid state"
      )
    }
  }

  trait ApprovedDeviceContext extends PendingDeviceContext {
    val approveDeviceRequest = ApproveDeviceRequest(deviceId = device.id)
    val approveDeviceResponse: ApproveDeviceResponse =
      adminClient.approveDevice(approveDeviceRequest)
    approveDeviceResponse.success === true
  }

  "With approved device" should {
    "list locations returns empty when no locations set" >> in(
      new ApprovedDeviceContext {}
    ) { context =>
      val request = ListLocationsRequest(deviceId = context.device.id)

      val response = context.adminClient.listLocations(request)

      response.locations === Seq.empty
    }

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
        startTimestamp = timestamp,
        endTimestamp = None,
        count = 1L
      )
    }

    "set a location with duplicates" >> in(
      new ApprovedDeviceContext {}
    ) { context =>
      def insertLocation(lat: Double, lon: Double, accuracy: Double, timestamp: Long) = {
        val request =
          SetLocationRequest(
            timestamp = timestamp,
            deviceId = context.device.id,
            location = Some(Location(lat, lon, accuracy))
          )

        context.client.setLocation(request) === SetLocationResponse(success = true)
      }

      insertLocation(51.500700, -0.124600, 0.1, 1L)
      insertLocation(51.500700, -0.124600, 0.1, 2L) // Exact duplicate
      insertLocation(51.500800, -0.124500, 0.2, 3L) // Close duplicate
      insertLocation(35.659500, 139.700500, 0.1, 4L) // Not a duplicate

      // Check location the fetched location is actually correct
      val listLocationsRequest = ListLocationsRequest(deviceId = context.device.id)
      val listLocationsResponse = context.adminClient.listLocations(listLocationsRequest)

      listLocationsResponse.locations must haveSize(2)
      listLocationsResponse.locations === Seq(
        StoredLocation(
          location = Some(Location(lat = 51.500800, lon = -0.124500, accuracy = 0.2)),
          startTimestamp = 1L,
          endTimestamp = Some(3L),
          count = 3L
        ),
        StoredLocation(
          location = Some(Location(lat = 35.659500, lon = 139.700500, accuracy = 0.1)),
          startTimestamp = 4L,
          endTimestamp = None,
          count = 1L
        )
      )
    }
  }
}
