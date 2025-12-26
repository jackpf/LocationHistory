package com.jackpf.locationhistory.server.grpc

import beacon.beacon_service.BeaconServiceGrpc.BeaconService
import beacon.beacon_service.{PingRequest, PingResponse}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import com.jackpf.locationhistory.server.testutil.DefaultSuite
import org.mockito.Mockito.mock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.ExecutionContext.Implicits.global

trait Context {
  val deviceRepo: DeviceRepo = mock(classOf[DeviceRepo])
  val locationRepo: LocationRepo = mock(classOf[LocationRepo])
  val beaconService: BeaconService =
    new BeaconServiceImpl(deviceRepo, locationRepo)
}

trait PingContext extends Context {
  lazy val request: PingRequest
  lazy val result: PingResponse = beaconService.ping(request).futureValue
}

class BeaconServiceImplTest extends DefaultSuite {
  test("can call ping endpoint") {
    new PingContext {
      override lazy val request: PingRequest = PingRequest()

      result.message mustBe "pong"
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
//      result.ok mustBe true
//    }
//  }
}
