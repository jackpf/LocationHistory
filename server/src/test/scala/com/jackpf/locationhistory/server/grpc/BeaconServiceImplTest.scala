package com.jackpf.locationhistory.server.grpc

import beacon.beacon_service.BeaconServiceGrpc.BeaconService
import beacon.beacon_service.{PingRequest, PingResponse}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.testutil.DefaultSuite
import org.mockito.Mockito.mock

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait DefaultScope {
  val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
  val locationRepo: LocationRepo = mock(classOf[LocationRepo])
  val beaconService: BeaconService =
    new BeaconServiceImpl(deviceRepo, locationRepo)
}

trait PingScope extends DefaultScope {
  lazy val request: PingRequest
  lazy val result: PingResponse =
    Await.result(beaconService.ping(request), Duration.Inf)
}

class BeaconServiceImplTest extends DefaultSuite {
  test("can call ping endpoint") {
    new PingScope {
      override lazy val request: PingRequest = PingRequest()

      result.message === "pong"
    }
  }

//  test("can call setLocation endpoint") {
//    new PingScope {
//      override lazy val request: PingRequest = PingRequest(
//        timestamp = 123L,
//        deviceId = "mock-device-id",
//        publicKey = "mock-public-key",
//        lat = 0.123,
//        lon = 0.456
//      )
//
//      result.ok === true
//    }
//  }
}
